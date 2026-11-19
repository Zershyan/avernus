package io.zershyan.avernus.handler.common;

import io.zershyan.avernus.realm.RealmCooldowns;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.realm.RealmSync;
import io.zershyan.avernus.registry.packet.ToggleWornMirrorPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** 领域的服务端生命周期：维持、所有者死亡/下线/跨维度时紧急关闭、登录与切维度时同步。 */
public final class RealmLifecycleHandler {
    private RealmLifecycleHandler() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) RealmManager.tick(level);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!event.isCanceled() && event.getEntity().level() instanceof ServerLevel level) {
            RealmManager.emergencyCloseOwned(level.getServer(), event.getEntity().getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (!event.isCanceled() && event.getEntity().level() instanceof ServerLevel level) {
            RealmManager.emergencyCloseOwned(level.getServer(), event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RealmManager.emergencyCloseOwned(player.server, player.getUUID());
            ToggleWornMirrorPacket.forget(player.getUUID());
            RealmSync.forget(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RealmManager.sendSnapshot(player);
            RealmCooldowns.refreshDisplay(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) RealmManager.sendSnapshot(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RealmManager.sendSnapshot(player);
            RealmCooldowns.refreshDisplay(player);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            RealmManager.PendingCooldowns.apply(living);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (var realm : RealmSets.of(level).all()) RealmManager.emergencyClose(level, realm);
        }
        RealmManager.shutdown();
    }
}
