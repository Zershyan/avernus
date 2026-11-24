package io.zershyan.avernus.handler.common;

import io.zershyan.avernus.realm.RealmPresenceTracker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.tick.EntityTickEvent;

/** 领域在场跟踪、首次落地保护与脱困。 */
public final class RealmPresenceHandler {
    private RealmPresenceHandler() {
    }

    @SubscribeEvent
    public static void beforeEntityTick(EntityTickEvent.Pre event) {
        var entity = event.getEntity();
        if (entity instanceof LivingEntity || entity instanceof VehicleEntity) RealmPresenceTracker.tick(entity);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        var entity = event.getEntity();
        if (entity instanceof LivingEntity || entity instanceof VehicleEntity) RealmPresenceTracker.tick(entity);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!event.getEntity().level().isClientSide && RealmPresenceTracker.consumeFallGuard(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
