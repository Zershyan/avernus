package io.zershyan.avernus.realm;

import io.zershyan.avernus.api.event.RealmEvent;
import io.zershyan.avernus.api.realm.RealmPhase;
import io.zershyan.avernus.datagen.lang.AvernusKeyLang;
import io.zershyan.avernus.registry.AvernusAttachments;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.config.ServerConfig;
import io.zershyan.avernus.registry.data_component.MirrorForm;
import io.zershyan.avernus.registry.packet.RealmRemovePacket;
import io.zershyan.avernus.registry.packet.RealmSnapshotPacket;
import io.zershyan.avernus.registry.packet.RealmUpdatePacket;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** 服务端领域生命周期：开境、关闭、维持与同步。只在服务端主线程调用。 */
public final class RealmManager {
    /** 同维度单 tick 位移超过该距离视为传送，镜子直接贴到新位置。 */
    private static final double TELEPORT_DISTANCE_SQR = 16.0 * 16.0;
    private static final AtomicLong SEQUENCE = new AtomicLong();
    /** 覆盖变化期间生物重算路径的间隔。 */
    private static final int PATH_REFRESH_INTERVAL = 10;

    private RealmManager() {
    }

    public enum Result {
        OPENED, CLOSING, ALREADY_CLOSING, COOLDOWN, INVALID_FORM, INVALID_SUBJECT
    }

    // region 开关

    /** 镜子使用入口：有自己的领域则请求关闭，否则以该形态开境。 */
    public static Result toggle(LivingEntity owner, MirrorForm form) {
        if (!(owner.level() instanceof ServerLevel level) || !owner.isAlive()) return Result.INVALID_SUBJECT;
        Realm existing = find(level.getServer(), owner.getUUID());
        if (existing != null) return requestClose(level, existing) ? Result.CLOSING : Result.ALREADY_CLOSING;
        return open(owner, form);
    }

    public static Result open(LivingEntity owner, MirrorForm form) {
        if (!(owner.level() instanceof ServerLevel level) || !owner.isAlive()) return Result.INVALID_SUBJECT;
        if (find(level.getServer(), owner.getUUID()) != null) return Result.INVALID_SUBJECT;
        long cooldown = RealmCooldowns.remaining(owner);
        if (cooldown > 0L) {
            notify(owner, AvernusKeyLang.MessageCooldown.get(Mth.ceil(cooldown / 20.0)));
            return Result.COOLDOWN;
        }
        Optional<MirrorForm.Resolved> resolved = form.resolve();
        if (resolved.isEmpty()) {
            notify(owner, AvernusKeyLang.MessageInvalidForm);
            return Result.INVALID_FORM;
        }
        Realm realm = create(level, owner, form, resolved.get());
        RealmSets.of(level).put(realm);
        owner.setData(AvernusAttachments.OWNS_OPEN_REALM, true);
        realm.aspect().onRealmOpened(realm);
        realm.traits().forEach(trait -> trait.onRealmOpened(realm));
        PacketDistributor.sendToPlayersInDimension(level, new RealmUpdatePacket(realm));
        NeoForge.EVENT_BUS.post(new RealmEvent.Opened(level, realm));
        return Result.OPENED;
    }

    private static Realm create(ServerLevel level, LivingEntity owner, MirrorForm form, MirrorForm.Resolved resolved) {
        RealmTimeline timeline = RealmTimeline.fromConfig();
        Vec3 chest = chestOf(owner);
        float yaw = owner.getYRot();
        float pitch = owner.getXRot();
        boolean eternal = resolved.traits().contains(AvernusRealmTraits.ETERNITY.get());
        double offsetX = 0.0;
        double offsetZ = 0.0;
        if (!eternal) {
            Vec3 shatter = chest.add(Vec3.directionFromRotation(pitch, yaw).scale(timeline.flyDistance()));
            offsetX = shatter.x - owner.getX();
            offsetZ = shatter.z - owner.getZ();
        }
        return new Realm(UUID.randomUUID(), owner.getUUID(), level.dimension(), form, resolved,
                Mth.floor(owner.getY()), ServerConfig.REALM_RADIUS.get(), SEQUENCE.incrementAndGet(),
                level.getGameTime(), level.getRandom().nextLong(), timeline, chest, yaw, pitch,
                owner.getX(), owner.getZ(), offsetX, offsetZ);
    }

    public static Vec3 chestOf(Entity entity) {
        return entity.position().add(0.0, entity.getBbHeight() * 0.7, 0.0);
    }

    /** 主动关闭：进入收束演出并立刻结算冷却；重复请求无效果。 */
    public static boolean requestClose(ServerLevel level, Realm realm) {
        if (!realm.requestClose(level.getGameTime())) return false;
        startCooldown(level, realm);
        PacketDistributor.sendToPlayersInDimension(level, new RealmUpdatePacket(realm));
        NeoForge.EVENT_BUS.post(new RealmEvent.Closing(level, realm));
        return true;
    }

    /** 紧急关闭：立即撤销玩法并移除；若此前未结算冷却则补结算。 */
    public static void emergencyClose(ServerLevel level, Realm realm) {
        if (realm.closeTime() < 0L) startCooldown(level, realm);
        finish(level, realm, true);
    }

    private static void startCooldown(ServerLevel level, Realm realm) {
        if (level.getEntity(realm.ownerId()) instanceof LivingEntity owner) RealmCooldowns.start(owner);
        else PendingCooldowns.add(realm.ownerId());
    }

    private static void finish(ServerLevel level, Realm realm, boolean emergency) {
        if (RealmSets.of(level).remove(realm.id()) == null) return;
        if (level.getEntity(realm.ownerId()) instanceof LivingEntity owner) owner.removeData(AvernusAttachments.OWNS_OPEN_REALM);
        refreshPaths(level, realm);
        realm.aspect().onRealmClosed(realm);
        realm.traits().forEach(trait -> trait.onRealmClosed(realm));
        PacketDistributor.sendToPlayersInDimension(level, new RealmRemovePacket(realm.id(), emergency));
        NeoForge.EVENT_BUS.post(new RealmEvent.Removed(level, realm, emergency));
    }

    // endregion

    // region 维持

    public static void tick(ServerLevel level) {
        RealmSet set = RealmSets.of(level);
        if (set.isEmpty()) return;
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
            if (realm.eternal() && followOwner(realm, owner)) {
                PacketDistributor.sendToPlayersInDimension(level, new RealmUpdatePacket(realm));
            }
            RealmPhase phase = realm.phase(time);
            if ((phase == RealmPhase.EXPANDING || phase == RealmPhase.CLOSING) && time % PATH_REFRESH_INTERVAL == 0) {
                refreshPaths(level, realm);
            }
            realm.aspect().tickRealm(realm);
            for (RealmTrait trait : realm.traits()) trait.tickRealm(realm);
        }
    }

    /** 覆盖范围变化后，让范围内仍在寻路的生物按新的有效地形重算路径。 */
    private static void refreshPaths(ServerLevel level, Realm realm) {
        double r = realm.radius() + 1.0;
        AABB box = new AABB(realm.exactCenterX() - r, level.getMinBuildHeight(), realm.exactCenterZ() - r,
                realm.exactCenterX() + r, level.getMaxBuildHeight(), realm.exactCenterZ() + r);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) {
            if (!mob.getNavigation().isDone()) mob.getNavigation().recomputePath();
        }
    }

    private static boolean followOwner(Realm realm, Entity owner) {
        double dx = owner.getX() - realm.exactCenterX();
        double dz = owner.getZ() - realm.exactCenterZ();
        if (dx == 0.0 && dz == 0.0) return false;
        if (dx * dx + dz * dz > TELEPORT_DISTANCE_SQR) realm.markTeleported();
        realm.moveCenter(owner.getX(), owner.getZ());
        return true;
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
        PacketDistributor.sendToPlayer(player, new RealmSnapshotPacket(player.level().dimension(),
                new ArrayList<>(RealmSets.of(player.serverLevel()).all())));
    }

    public static void shutdown() {
        RealmSets.clearServer();
        PendingCooldowns.clear();
    }

    private static void notify(LivingEntity entity, net.minecraft.network.chat.Component message) {
        if (entity instanceof Player player) player.displayClientMessage(message, true);
    }

    // endregion

    /** 所有者已不在本维度时的冷却补记，待其再次出现时写入附件。 */
    public static final class PendingCooldowns {
        private static final List<UUID> PENDING = new ArrayList<>();

        private PendingCooldowns() {
        }

        static synchronized void add(UUID owner) {
            if (!PENDING.contains(owner)) PENDING.add(owner);
        }

        /** 实体进入世界时：补记离场期间的冷却；若带着上次异常退出遗留的领域标记，也视为一次关闭。 */
        public static synchronized void apply(LivingEntity entity) {
            boolean stale = entity.getData(AvernusAttachments.OWNS_OPEN_REALM) && find(entity.getServer(), entity.getUUID()) == null;
            if (PENDING.remove(entity.getUUID()) || stale) RealmCooldowns.start(entity);
            if (stale) entity.removeData(AvernusAttachments.OWNS_OPEN_REALM);
        }

        static synchronized void clear() {
            PENDING.clear();
        }
    }
}
