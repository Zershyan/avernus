package io.zershyan.avernus.handler.common;

import io.zershyan.avernus.realm.RealmCooldowns;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.realm.RealmSync;
import io.zershyan.avernus.compat.curios.registry.CuriosToggleMirrorPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;

/** 领域的服务端生命周期：维持、所有者死亡/下线/跨维度时紧急关闭、登录与切维度时同步。 */
public final class RealmLifecycleHandler {
    private RealmLifecycleHandler() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) RealmManager.tick(level);
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
            if (player.getServer() != null) RealmManager.emergencyCloseOwned(player.getServer(), player.getUUID());
            CuriosToggleMirrorPacket.forget(player.getUUID());
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
