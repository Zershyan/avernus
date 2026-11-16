package io.zershyan.avernus.client.realm_render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;

/**
 * 摄像机所处领域的环境后处理：世界绘制完成后、第一人称手部之前，把主目标颜色复制到自有目标，
 * 结合场景深度做境相色调、深度雾与波环亮线，再写回主目标颜色，不改动主场景深度。
 */
public final class RealmEnvironment {
    private static final float FADE_PER_TICK = 0.05F;

    private static @Nullable EffectInstance effect;
    private static boolean failed;
    private static @Nullable RenderTarget copy;
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
        if (strength <= 0.0F && strengthO <= 0.0F) current = null;
    }

    private static float strength(float partialTick) {
        return Mth.lerp(partialTick, strengthO, strength);
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
        return manager -> close();
    }

    public static void close() {
        if (effect != null) effect.close();
        if (copy != null) copy.destroyBuffers();
        effect = null;
        copy = null;
        failed = false;
        current = null;
        profile = null;
        strength = strengthO = 0.0F;
    }

    private static @Nullable EffectInstance effect() {
        if (effect == null && !failed) {
            try {
                effect = new EffectInstance(Minecraft.getInstance().getResourceManager(), Avernus.id("realm_environment").toString());
            } catch (IOException e) {
                failed = true;
                Avernus.LOGGER.error("领域环境 Shader 加载失败，环境后处理已停用", e);
            }
        }
        return effect;
    }

    private static RenderTarget copyOf(RenderTarget main) {
        if (copy == null) {
            copy = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
        } else if (copy.width != main.width || copy.height != main.height) {
            copy.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        return copy;
    }

    // endregion

    public static void render(RenderLevelStageEvent event) {
        Realm realm = current;
        RealmVisualProfile visual = profile;
        float s = strength(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        if (realm == null || visual == null || s <= 0.0F) return;
        EffectInstance shader = effect();
        if (shader == null) return;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        RenderTarget target = copyOf(main);

        GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, target.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, target.width, target.height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        main.bindWrite(true);

        Vec3 cam = event.getCamera().getPosition();
        Matrix4f invViewProj = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix()).invert();
        float time = event.getRenderTick() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float w = main.width, h = main.height;

        shader.setSampler("DiffuseSampler", target::getColorTextureId);
        shader.setSampler("DepthSampler", main::getDepthTextureId);
        shader.safeGetUniform("ProjMat").set(new Matrix4f().setOrtho(0.0F, w, 0.0F, h, 0.1F, 1000.0F));
        shader.safeGetUniform("InSize").set(w, h);
        shader.safeGetUniform("OutSize").set(w, h);
        shader.safeGetUniform("InvViewProj").set(invViewProj);
        shader.safeGetUniform("Tint").set(RealmVisualProfile.rgb(visual.tint()));
        shader.safeGetUniform("FogTint").set(RealmVisualProfile.rgb(visual.fogColor()));
        shader.safeGetUniform("Strength").set(s);
        shader.safeGetUniform("FogDistance").set(visual.fogDistance());
        shader.safeGetUniform("WaveSource").set((float) (realm.sourceX() - cam.x), (float) (realm.sourceZ() - cam.z));
        shader.safeGetUniform("WaveRadius").set(mc.level == null ? -1.0F : (float) realm.waveRadius(mc.level.getGameTime()));
        shader.safeGetUniform("Time").set(time);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        shader.apply();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(0.0F, 0.0F, 500.0F);
        builder.addVertex(w, 0.0F, 500.0F);
        builder.addVertex(w, h, 500.0F);
        builder.addVertex(0.0F, h, 500.0F);
        BufferUploader.draw(builder.buildOrThrow());
        shader.clear();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        main.bindWrite(true);
    }

    private static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    private static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;
    private static final int GL_COLOR_BUFFER_BIT = 0x4000;
    private static final int GL_NEAREST = 0x2600;
}
