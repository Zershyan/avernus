package io.zershyan.avernus.handler.client;

import io.zershyan.avernus.client.mirror.MirrorAnchors;
import io.zershyan.avernus.client.particle.RealmParticles;
import io.zershyan.avernus.client.realm_render.RealmEnvironment;
import io.zershyan.avernus.client.realm_render.RealmImpact;
import io.zershyan.avernus.client.realm_render.RealmRenderState;
import io.zershyan.avernus.client.sound.RealmSounds;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.level.ChunkEvent;

/** 客户端领域渲染的游戏事件：网格重建、附加几何、镜子演出、粒子、领域音效、环境后处理与退出清理。 */
public final class RealmRenderHandler {
    private RealmRenderHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        RealmEnvironment.tick();
        MirrorAnchors.tick();
        RealmParticles.tick();
        RealmSounds.tick();
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) RealmRenderState.renderWeakWaterAbyss(event);
    }

    @SubscribeEvent
    public static void onBlockOverlay(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() != RenderBlockScreenEffectEvent.OverlayType.FIRE
                && RealmRenderState.inWeakWaterAbyss(Minecraft.getInstance().gameRenderer.getMainCamera().getBlockPosition())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        RealmEnvironment.modifyFogColor(event);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        RealmImpact.shake(event);
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        invalidateChunk(event);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        invalidateChunk(event);
    }

    private static void invalidateChunk(ChunkEvent event) {
        if (!(event.getLevel() instanceof ClientLevel level)) return;
        var pos = event.getChunk().getPos();
        for (int x = pos.x - 1; x <= pos.x + 1; x++) {
            for (int z = pos.z - 1; z <= pos.z + 1; z++) {
                for (int y = level.getMinSection(); y < level.getMaxSection(); y++) RealmRenderState.dirty(level, x, y, z);
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RealmSets.clearClient();
        RealmRenderState.reset();
        RealmEnvironment.close();
        MirrorAnchors.reset();
        RealmParticles.reset();
        RealmSounds.reset();
    }
}
