package io.zershyan.avernus.client.realm_render;

import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.registry.config.ClientConfig;
import io.zershyan.avernus.registry.config.RealmFxQuality;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.util.List;

import static org.lwjgl.opengl.GL11.GL_COLOR;
import static org.lwjgl.opengl.GL30.glClearBufferfv;

/** 领域环境：摄像机所在领域的氛围渐变、雾色，以及每帧组装后处理输入。 */
public final class RealmEnvironment {
    private static final float FADE_PER_TICK = 0.05F;
    private static final int TIME_PERIOD_TICKS = 120000;

    private static @Nullable RealmVisualProfile profile;
    private static @Nullable Realm current;
    private static float strength;
    private static float strengthO;

    private RealmEnvironment() {
    }

    // region 状态

    /** 摄像机所在列被观察者可见的领域覆盖时，环境渐入；离开后渐出并保留最后的外观。 */
    public static void tick() {
        strengthO = strength;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Camera camera = mc.gameRenderer.getMainCamera();
        Realm realm = null;
        if (level != null && camera.isInitialized()) {
            Vec3 pos = camera.getPosition();
            realm = RealmSets.of(level).geometryAt(camera.getEntity(), Mth.floor(pos.x), Mth.floor(pos.z), level.getGameTime());
        }
        if (realm != null) {
            current = realm;
            profile = RealmVisuals.get(realm.aspect());
        }
        strength = Mth.approach(strength, realm != null ? 1.0F : 0.0F, FADE_PER_TICK);
        if (strength <= 0.0F && strengthO <= 0.0F && current != null) {
            current = null;
            profile = null;
        }
    }

    private static float strength(float partialTick) {
        return Mth.lerp(partialTick, strengthO, strength);
    }

    /** 境内天空已整体替换为程序化天空，原版云层只会留下色差斑块（关档保持原样）。 */
    public static boolean hidesClouds() {
        return strength > 0.0F && ClientConfig.quality() != RealmFxQuality.OFF;
    }

    public static void modifyFogColor(ViewportEvent.ComputeFogColor event) {
        float s = strength((float) event.getPartialTick());
        if (s <= 0.0F || profile == null) return;
        Vector3f fog = RealmVisualProfile.rgb(profile.fogColor());
        event.setRed(Mth.lerp(s, event.getRed(), fog.x));
        event.setGreen(Mth.lerp(s, event.getGreen(), fog.y));
        event.setBlue(Mth.lerp(s, event.getBlue(), fog.z));
    }

    // endregion

    // region 资源

    public static ResourceManagerReloadListener reloadListener() {
        return manager -> RealmPostChain.close();
    }

    public static void close() {
        RealmPostChain.close();
        current = null;
        profile = null;
        strength = strengthO = 0.0F;
    }

    // endregion

    public static void render(RenderLevelStageEvent event) {
        if (RealmRenderState.inWeakWaterAbyss(event.getCamera().getBlockPosition())) {
            try (var stack = MemoryStack.stackPush()) {
                glClearBufferfv(GL_COLOR, 0, stack.floats(0F, 0F, 0F, 1F));
            }
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        RealmFxQuality quality = ClientConfig.quality();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        Realm realm = current;
        float s = realm != null && profile != null ? strength(partialTick) : 0.0F;
        if (quality == RealmFxQuality.OFF && s <= 0.0F) return;

        List<RealmMoments.Moment> moments = RealmMoments.collect(level, camera.getEntity(), cam, partialTick);
        RealmVisualProfile visual = profile != null ? profile : moments.isEmpty() ? RealmVisualProfile.FALLBACK : moments.getFirst().visual();
        double wave = realm == null ? -1.0 : moments.stream().filter(moment -> moment.realm() == realm)
                .mapToDouble(RealmMoments.Moment::wave).findFirst().orElseGet(() -> realm.waveRadius(level.getGameTime()));
        Matrix4f viewProj = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
        float time = (event.getRenderTick() % TIME_PERIOD_TICKS + partialTick) / 20.0F;
        var frame = new RealmPostFrame(quality, s, realm, visual, wave, viewProj, new Matrix4f(viewProj).invert(), cam, time,
                moments, RealmImpact.shock(moments, cam), RealmImpact.flash(moments, cam, quality));
        if (!frame.idle()) RealmPostChain.render(frame, mc.getMainRenderTarget());
    }
}
