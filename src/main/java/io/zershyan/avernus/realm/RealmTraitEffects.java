package io.zershyan.avernus.realm;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.AvernusAttachments;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.attachment.IllusionPermits;
import io.zershyan.avernus.registry.config.ServerConfig;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.EffectCures;
import net.minecraftforge.common.MinecraftForgeMod;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 境印效果的服务端判定，事件层只负责转发。 */
public final class RealmTraitEffects {
    private RealmTraitEffects() {
    }

    /** 实体所在列生效、且带有该境印的领域。 */
    private static @Nullable Realm effectWith(Entity entity, RealmTrait trait) {
        if (!(entity.level() instanceof ServerLevel level)) return null;
        RealmSet set = RealmSets.of(level);
        if (set.isEmpty()) return null;
        Realm realm = set.effectAt(entity.getBlockX(), entity.getBlockZ(), level.getGameTime());
        return realm != null && realm.hasTrait(trait) ? realm : null;
    }

    /** 带特定境印的领域每 tick 的服务端效果。 */
    public static void tick(ServerLevel level, Realm realm) {
        if (realm.hasTrait(AvernusRealmTraits.EQUALITY.get()) && level.getGameTime() % EQUALITY_INTERVAL == 0) liftBelowSurface(level, realm);
        if (realm.hasTrait(AvernusRealmTraits.FREEDOM.get())) updateFreedom(level, realm, !realm.isClosing(level.getGameTime()));
    }

    /** 领域结束时撤销持续效果。 */
    public static void release(ServerLevel level, Realm realm) {
        if (realm.hasTrait(AvernusRealmTraits.FREEDOM.get())) updateFreedom(level, realm, false);
    }

    // region 平等

    private static final int EQUALITY_INTERVAL = 10;

    /** 把境面以下、已被覆盖列中的生物带到该列境面之上：实心处站在境面上，空洞与水面处落在一格深的坑底。 */
    private static void liftBelowSurface(ServerLevel level, Realm realm) {
        int radius = realm.radius();
        AABB area = new AABB(realm.exactCenterX() - radius, level.getMinBuildHeight(), realm.exactCenterZ() - radius,
                realm.exactCenterX() + radius, realm.height() - 1, realm.exactCenterZ() + radius);
        long time = level.getGameTime();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, entity -> realm.belowSurface(entity)
                && realm.reaches(entity) && realm.covers(entity.getBlockX(), entity.getBlockZ(), time))) {
            BlockPos surface = BlockPos.containing(entity.getX(), realm.height() - 1, entity.getZ());
            boolean solid = RealmTerrain.classify(realm, level.getBlockState(surface), surface.getY()) == RealmTerrain.Kind.SOLID;
            entity.resetFallDistance();
            entity.teleportTo(entity.getX(), solid ? realm.height() : realm.height() - 1, entity.getZ());
        }
    }

    // endregion

    // region 自由

    private static final ResourceLocation FREEDOM_MODIFIER = Avernus.id("freedom");

    /** 开境者站在自己领域生效的列上时获得飞行能力，离开或领域收起时撤销。 */
    private static void updateFreedom(ServerLevel level, Realm realm, boolean active) {
        if (!(level.getEntity(realm.ownerId()) instanceof Player owner)) return;
        AttributeInstance flight = owner.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight == null) return;
        boolean grant = active && realm.covers(owner.getBlockX(), owner.getBlockZ(), level.getGameTime());
        if (grant == flight.hasModifier(FREEDOM_MODIFIER)) return;
        if (grant) flight.addTransientModifier(new AttributeModifier(FREEDOM_MODIFIER, 1.0, AttributeModifier.Operation.ADD_VALUE));
        else flight.removeModifier(FREEDOM_MODIFIER);
    }

    // endregion

    // region 湮没

    public static boolean annihilatesDrops(LivingEntity entity) {
        return !(entity instanceof Player) && effectWith(entity, AvernusRealmTraits.ANNIHILATION.get()) != null;
    }

    // endregion

    // region 虚妄

    /** 所有者在自己的虚妄领域内，免疫无造成者的伤害。 */
    public static boolean negatesDamage(LivingEntity entity, DamageSource source) {
        if (source.getEntity() != null) return false;
        Realm realm = effectWith(entity, AvernusRealmTraits.UNREALITY.get());
        return realm != null && realm.ownerId().equals(entity.getUUID());
    }

    // endregion

    // region 往生

    /** 致命伤时先于不死图腾：所有者拥有往生领域即消费该领域救回一次，并紧急关闭领域。 */
    public static boolean tryRebirth(LivingEntity entity, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !(entity.level() instanceof ServerLevel level)) return false;
        Realm realm = effectWith(entity, AvernusRealmTraits.REBIRTH.get());
        if (realm == null || !realm.ownerId().equals(entity.getUUID())) return false;
        RealmManager.emergencyClose(level, realm);
        entity.setHealth(1.0F);
        entity.removeEffectsCuredBy(EffectCures.PROTECTED_BY_TOTEM);
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ServerConfig.REBIRTH_REGENERATION_TICKS.get(),
                ServerConfig.REBIRTH_REGENERATION_LEVEL.get() - 1));
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ServerConfig.REBIRTH_ABSORPTION_TICKS.get(),
                ServerConfig.REBIRTH_ABSORPTION_LEVEL.get() - 1));
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ServerConfig.REBIRTH_FIRE_RESISTANCE_TICKS.get(), 0));
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    // endregion

    // region 幻化

    /** 敌对生物与玩家处于同一幻化领域时返回该领域。 */
    private static @Nullable Realm sharedIllusion(Mob mob, Player player) {
        if (!(mob instanceof Enemy)) return null;
        Realm realm = effectWith(mob, AvernusRealmTraits.ILLUSION.get());
        return realm != null && realm == effectWith(player, AvernusRealmTraits.ILLUSION.get()) ? realm : null;
    }

    private static boolean permitted(Mob mob, Realm realm, Player player) {
        IllusionPermits permits = AvernusAttachments.getExisting(mob, AvernusAttachments.ILLUSION_PERMITS);
        return permits != null && permits.allows(realm.id(), player.getUUID());
    }

    /** 是否禁止该生物以该玩家为目标。 */
    public static boolean forbidsTarget(Mob mob, @Nullable LivingEntity target) {
        if (!(target instanceof Player player)) return false;
        Realm realm = sharedIllusion(mob, player);
        return realm != null && !permitted(mob, realm, player);
    }

    /** 玩家（含其远程攻击）实际伤害生物后，记录该生物对该玩家的反击许可。 */
    public static void recordRetaliation(LivingEntity victim, DamageSource source) {
        if (!(victim instanceof Mob mob) || !(source.getEntity() instanceof Player player)) return;
        Realm realm = sharedIllusion(mob, player);
        if (realm == null) return;
        IllusionPermits old = AvernusAttachments.getExisting(mob, AvernusAttachments.ILLUSION_PERMITS);
        Set<UUID> players = new HashSet<>();
        if (old != null && old.realm().equals(realm.id())) players.addAll(old.players());
        players.add(player.getUUID());
        AvernusAttachments.set(mob, AvernusAttachments.ILLUSION_PERMITS, new IllusionPermits(realm.id(), Set.copyOf(players)));
    }

    /** 清除生物对受保护玩家的既有目标、中立愤怒与 Brain 攻击记忆。 */
    public static void suppressTargets(Mob mob) {
        if (!(mob instanceof Enemy) || !(mob.level() instanceof ServerLevel level) || RealmSets.of(level).isEmpty()) return;
        if (forbidsTarget(mob, mob.getTarget())) {
            mob.setTarget(null);
            if (mob instanceof Creeper creeper && !creeper.isIgnited()) creeper.setSwellDir(-1);
        }
        if (mob instanceof NeutralMob neutral && neutral.getPersistentAngerTarget() != null
                && forbidsTarget(mob, mob.level().getPlayerByUUID(neutral.getPersistentAngerTarget()))) {
            neutral.stopBeingAngry();
        }
        var brain = mob.getBrain();
        if (brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                && forbidsTarget(mob, brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null))) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
        if (brain.hasMemoryValue(MemoryModuleType.ANGRY_AT)
                && forbidsTarget(mob, mob.level().getPlayerByUUID(brain.getMemory(MemoryModuleType.ANGRY_AT).orElseThrow()))) {
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
        }
    }

    // endregion
}
