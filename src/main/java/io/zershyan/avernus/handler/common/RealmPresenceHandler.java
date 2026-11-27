package io.zershyan.avernus.handler.common;

import io.zershyan.avernus.realm.RealmPresenceTracker;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 首次落地保护；在场追踪统一由 MixinServerLevel 接管。 */
public final class RealmPresenceHandler {
    private RealmPresenceHandler() {
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!event.getEntity().level().isClientSide && RealmPresenceTracker.consumeFallGuard(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
