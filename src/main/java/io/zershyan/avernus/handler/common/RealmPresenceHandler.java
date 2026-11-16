package io.zershyan.avernus.handler.common;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.realm.RealmPresenceTracker;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** 领域在场跟踪、首次落地保护与脱困。 */
@EventBusSubscriber(modid = Avernus.MODID)
public final class RealmPresenceHandler {
    private RealmPresenceHandler() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide) RealmPresenceTracker.tick(living);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!event.getEntity().level().isClientSide && RealmPresenceTracker.consumeFallGuard(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
