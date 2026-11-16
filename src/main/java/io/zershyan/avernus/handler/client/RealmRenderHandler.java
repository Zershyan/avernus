package io.zershyan.avernus.handler.client;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.client.mirror.RealmMirrorRenderer;
import io.zershyan.avernus.client.realm_render.RealmEnvironment;
import io.zershyan.avernus.client.realm_render.RealmRenderState;
import io.zershyan.avernus.realm.RealmSets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** 客户端领域渲染的游戏事件：网格重建、附加几何、镜子演出、环境后处理与退出清理。 */
@EventBusSubscriber(modid = Avernus.MODID, value = Dist.CLIENT)
public final class RealmRenderHandler {
    private RealmRenderHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        RealmRenderState.tick();
        RealmEnvironment.tick();
        RealmMirrorRenderer.tick();
    }

    @SubscribeEvent
    public static void onSectionGeometry(AddSectionGeometryEvent event) {
        RealmRenderState.addGeometry(event);
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) RealmMirrorRenderer.render(event);
        else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) RealmEnvironment.render(event);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        RealmEnvironment.modifyFogColor(event);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RealmSets.clearClient();
        RealmRenderState.reset();
        RealmEnvironment.close();
        RealmMirrorRenderer.reset();
    }
}
