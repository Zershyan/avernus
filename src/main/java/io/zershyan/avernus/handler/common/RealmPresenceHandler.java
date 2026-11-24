package io.zershyan.avernus.handler.common;

import io.zershyan.avernus.realm.RealmPresenceTracker;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 领域在场跟踪、首次落地保护与脱困；载具经 MixinEntity 单独跟踪。 */
public final class RealmPresenceHandler {
    private RealmPresenceHandler() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        RealmPresenceTracker.tick(event.getEntity());
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!event.getEntity().level().isClientSide && RealmPresenceTracker.consumeFallGuard(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
