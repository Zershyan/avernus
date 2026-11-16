package io.zershyan.avernus.handler.common;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.realm.RealmTraitEffects;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** 类型效果事件：湮没、虚妄、幻化；往生经由 LivingEntityMixin 先于图腾触发。 */
@EventBusSubscriber(modid = Avernus.MODID)
public final class RealmTraitHandler {
    private static final int SUPPRESS_INTERVAL = 5;

    private RealmTraitHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent event) {
        if (RealmTraitEffects.annihilatesDrops(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (RealmTraitEffects.negatesDamage(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0.0F) RealmTraitEffects.recordRetaliation(event.getEntity(), event.getSource());
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide
                && RealmTraitEffects.forbidsTarget(mob, event.getNewAboutToBeSetTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide && mob.tickCount % SUPPRESS_INTERVAL == 0) {
            RealmTraitEffects.suppressTargets(mob);
        }
    }
}
