package io.zershyan.avernus.realm;

import io.zershyan.avernus.api.event.RealmEvent;
import io.zershyan.avernus.api.realm.MirrorForm;
import io.zershyan.avernus.api.realm.RealmResult;
import io.zershyan.avernus.datagen.lang.AvernusKeyLang;
import io.zershyan.avernus.registry.AvernusAttachments;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.AvernusSounds;
import io.zershyan.avernus.registry.config.ServerConfig;
import io.zershyan.avernus.registry.entity.MirrorEntity;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** 服务端领域生命周期：开境、关闭、维持与同步。只在服务端主线程调用。 */
public final class RealmManager {
    /** 同维度单 tick 位移超过该距离视为传送，镜子直接贴到新位置。 */
    private static final double TELEPORT_DISTANCE_SQR = 16.0 * 16.0;
    private static final AtomicLong SEQUENCE = new AtomicLong();
    /** 永恒镜子中心高出头顶的距离。 */
    public static final double HEAD_CLEARANCE = 1.8;
    private static final Map<UUID, MirrorEntity> MIRRORS = new HashMap<>();

    private RealmManager() {
    }

    // region 开关

    /** 镜子使用入口：有自己的领域则请求关闭，否则以该形态开境。 */
    public static RealmResult toggle(LivingEntity owner, MirrorForm form) {
        if (!(owner.level() instanceof ServerLevel level) || !owner.isAlive()) return RealmResult.INVALID_SUBJECT;
        Realm existing = find(level.getServer(), owner.getUUID());
        if (existing != null) return requestClose(level, existing) ? RealmResult.CLOSING : RealmResult.ALREADY_CLOSING;
        return open(owner, form);
    }

    public static RealmResult open(LivingEntity owner, MirrorForm form) {
        if (!(owner.level() instanceof ServerLevel level) || !owner.isAlive()) return RealmResult.INVALID_SUBJECT;
        if (find(level.getServer(), owner.getUUID()) != null) return RealmResult.INVALID_SUBJECT;
        long cooldown = RealmCooldowns.remaining(owner);
        if (cooldown > 0L) {
            notify(owner, AvernusKeyLang.MessageCooldown.get(Mth.ceil(cooldown / 20.0)));
            return RealmResult.COOLDOWN;
        }
        Optional<MirrorForms.Resolved> resolved = MirrorForms.resolve(form);
        if (resolved.isEmpty()) {
            notify(owner, AvernusKeyLang.MessageInvalidForm);
            return RealmResult.INVALID_FORM;
        }
        Realm realm = create(level, owner, form, resolved.get());
        RealmSets.of(level).put(realm);
        MIRRORS.put(realm.id(), MirrorEntity.spawn(level, realm));
        owner.removeData(AvernusAttachments.REALM_COOLDOWN);
        owner.setData(AvernusAttachments.OWNS_OPEN_REALM, true);
        realm.aspect().onRealmOpened(realm);
        realm.traits().forEach(trait -> trait.onRealmOpened(realm));
        RealmSync.tick(level, RealmSets.of(level));
        play(level, realm.launchOrigin(), AvernusSounds.MIRROR_THROW.get());
        NeoForge.EVENT_BUS.post(new RealmEvent.Opened(level, realm));
        return RealmResult.OPENED;
    }

    private static Realm create(ServerLevel level, LivingEntity owner, MirrorForm form, MirrorForms.Resolved resolved) {
        RealmTimeline timeline = RealmTimeline.fromConfig();
        Vec3 chest = chestOf(owner);
        Vec3 eye = owner.getEyePosition();
        float yaw = owner.getYRot();
        boolean eternal = resolved.traits().contains(AvernusRealmTraits.ETERNITY.get());
        double offsetX = 0.0;
        double offsetZ = 0.0;
        if (!eternal) {
            Vec3 shatter = eye.add(Vec3.directionFromRotation(0.0F, yaw).scale(timeline.flyDistance()));
            offsetX = shatter.x - owner.getX();
            offsetZ = shatter.z - owner.getZ();
        }
        Realm.Motion motion = new Realm.Motion(0L, -1L, owner.getX(), owner.getZ(), chest, headY(owner), 0);
        return new Realm(new Realm.Snapshot(UUID.randomUUID(), owner.getUUID(), owner.getId(), level.dimension(), form,
                Mth.floor(owner.getY()), ServerConfig.REALM_RADIUS.get(), SEQUENCE.incrementAndGet(), level.getGameTime(),
                level.getRandom().nextLong(), timeline, eye, yaw, offsetX, offsetZ, motion), resolved);
    }

    public static Vec3 chestOf(Entity entity) {
        return entity.position().add(0.0, entity.getBbHeight() * 0.7, 0.0);
    }

    private static double headY(Entity entity) {
        return entity.getY() + entity.getBbHeight() + HEAD_CLEARANCE;
    }

    /** 主动关闭：进入收束演出并立刻结算冷却；重复请求无效果。 */
    public static boolean requestClose(ServerLevel level, Realm realm) {
        if (!realm.requestClose(level.getGameTime())) return false;
        startCooldown(level, realm);
        if (level.getEntity(realm.ownerId()) instanceof LivingEntity owner) realm.moveAnchor(chestOf(owner), headY(owner), false);
        RealmSync.tick(level, RealmSets.of(level));
        NeoForge.EVENT_BUS.post(new RealmEvent.Closing(level, realm));
        return true;
    }

    /** 紧急关闭：立即撤销玩法并移除；若此前未结算冷却则补结算。 */
    public static void emergencyClose(ServerLevel level, Realm realm) {
        if (realm.closeTime() < 0L) startCooldown(level, realm);
        finish(level, realm, true);
    }

    private static void startCooldown(ServerLevel level, Realm realm) {
        if (level.getEntity(realm.ownerId()) instanceof LivingEntity owner) {
            RealmCooldowns.start(owner);
            owner.removeData(AvernusAttachments.OWNS_OPEN_REALM);
        } else PendingCooldowns.add(realm.ownerId());
    }

    private static void finish(ServerLevel level, Realm realm, boolean emergency) {
        if (RealmSets.of(level).remove(realm.id()) == null) return;
        MirrorEntity mirror = MIRRORS.remove(realm.id());
        if (mirror != null) mirror.discard();
        if (level.getEntity(realm.ownerId()) instanceof LivingEntity owner) {
            owner.removeData(AvernusAttachments.OWNS_OPEN_REALM);
            if (!emergency) play(level, chestOf(owner), AvernusSounds.MIRROR_RETURN.get());
        }
        RealmNavigation.remove(level, realm);
        RealmTraitEffects.release(level, realm);
        RealmAttributeEffects.release(level, realm);
        realm.aspect().onRealmClosed(realm);
        realm.traits().forEach(trait -> trait.onRealmClosed(realm));
        RealmSync.remove(level, realm);
        NeoForge.EVENT_BUS.post(new RealmEvent.Removed(level, realm, emergency));
    }

    // endregion

    // region 维持

    public static void tick(ServerLevel level) {
        RealmSet set = RealmSets.of(level);
        long time = level.getGameTime();
        for (Realm realm : set.all()) {
            Entity owner = level.getEntity(realm.ownerId());
            if (!(owner instanceof LivingEntity living) || !living.isAlive() || owner.isRemoved()) {
                emergencyClose(level, realm);
                continue;
            }
            if (time >= realm.endTime()) {
                finish(level, realm, false);
                continue;
            }
            boolean snap = false;
            if (realm.eternal() || realm.isClosing(time)) {
                Vec3 chest = chestOf(owner);
                snap = realm.eternal() && chest.distanceToSqr(realm.motion().chest()) > TELEPORT_DISTANCE_SQR;
                realm.moveAnchor(chest, headY(owner), snap);
            }
            ensureMirror(level, realm);
            RealmNavigation.update(level, realm, time, snap);
            RealmTraitEffects.tick(level, realm);
            realm.aspect().tickRealm(realm);
            for (RealmTrait trait : realm.traits()) trait.tickRealm(realm);
        }
        RealmAttributeEffects.tick(level);
        RealmSync.tick(level, set);
    }

    /** 镜子实体不存档，所在区块卸载后在重新加载时补生成。 */
    private static void ensureMirror(ServerLevel level, Realm realm) {
        MirrorEntity mirror = MIRRORS.get(realm.id());
        if (mirror != null && !mirror.isRemoved()) return;
        if (level.isLoaded(BlockPos.containing(realm.shatterPoint()))) MIRRORS.put(realm.id(), MirrorEntity.spawn(level, realm));
    }

    public static void ownerTeleported(Entity entity) {
        if (!(entity instanceof LivingEntity) || !(entity.level() instanceof ServerLevel level)) return;
        Realm realm = RealmSets.of(level).ownedBy(entity.getUUID());
        if (realm == null || !realm.eternal()) return;
        Realm.Motion previous = realm.motion();
        Vec3 chest = chestOf(entity);
        double head = headY(entity);
        if (previous.chest().equals(chest) && previous.headY() == head) return;
        realm.moveAnchor(chest, head, true);
        RealmNavigation.update(level, realm, level.getGameTime(), true);
        RealmSync.tick(level, RealmSets.of(level));
    }

    // endregion

    // region 查询与同步

    public static @Nullable Realm find(net.minecraft.server.MinecraftServer server, UUID owner) {
        for (ServerLevel level : server.getAllLevels()) {
            Realm realm = RealmSets.of(level).ownedBy(owner);
            if (realm != null) return realm;
        }
        return null;
    }

    /** 关闭某所有者在所有维度的领域（死亡、下线、跨维度）。 */
    public static void emergencyCloseOwned(net.minecraft.server.MinecraftServer server, UUID owner) {
        for (ServerLevel level : server.getAllLevels()) {
            Realm realm = RealmSets.of(level).ownedBy(owner);
            if (realm != null) emergencyClose(level, realm);
        }
    }

    public static void sendSnapshot(ServerPlayer player) {
        RealmSync.reset(player);
    }

    public static void shutdown() {
        RealmSets.clearServer();
        MIRRORS.clear();
        RealmSync.clear();
        RealmNavigation.clear();
        PendingCooldowns.clear();
    }

    private static void play(ServerLevel level, Vec3 at, SoundEvent sound) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void notify(LivingEntity entity, net.minecraft.network.chat.Component message) {
        if (entity instanceof Player player) player.displayClientMessage(message, true);
    }

    // endregion

    /** 所有者已不在本维度时的冷却补记，待其再次出现时写入附件。 */
    public static final class PendingCooldowns {
        private static final Set<UUID> PENDING = new HashSet<>();

        private PendingCooldowns() {
        }

        static synchronized void add(UUID owner) {
            PENDING.add(owner);
        }

        /** 实体进入世界时：补记离场期间的冷却；若带着上次异常退出遗留的领域标记，也视为一次关闭。 */
        public static synchronized void apply(LivingEntity entity) {
            if (!(entity.level() instanceof ServerLevel level)) return;
            boolean pending = PENDING.remove(entity.getUUID());
            boolean stale = entity.hasData(AvernusAttachments.OWNS_OPEN_REALM) && entity.getData(AvernusAttachments.OWNS_OPEN_REALM)
                    && find(level.getServer(), entity.getUUID()) == null;
            if (!pending && !stale) return;
            if (RealmCooldowns.remaining(entity) == 0L) RealmCooldowns.start(entity);
            entity.removeData(AvernusAttachments.OWNS_OPEN_REALM);
        }

        static synchronized void clear() {
            PENDING.clear();
        }
    }
}
