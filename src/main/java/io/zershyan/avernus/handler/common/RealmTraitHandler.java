package io.zershyan.avernus.handler.common;

import io.zershyan.avernus.realm.RealmTraitEffects;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 境印效果事件：湮没、虚妄、幻化；往生经由 MixinLivingEntity 先于图腾触发。 */
public final class RealmTraitHandler {
    private RealmTraitHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent event) {
        if (RealmTraitEffects.annihilatesDrops(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        if (RealmTraitEffects.negatesDamage(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent event) {
        if (event.getAmount() > 0.0F) RealmTraitEffects.recordRetaliation(event.getEntity(), event.getSource());
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide
                && RealmTraitEffects.forbidsTarget(mob, event.getNewTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob) RealmTraitEffects.suppressTargets(mob);
    }
}
