package io.zershyan.avernus.client.realm_render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.zershyan.avernus.registry.AvernusShaders;
import io.zershyan.avernus.registry.config.RealmFxQuality;
import net.minecraft.client.renderer.ShaderInstance;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_ALPHA;
import static org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_RGB;
import static org.lwjgl.opengl.GL20.glBlendEquationSeparate;
import static org.lwjgl.opengl.GL30.*;

/**
 * 领域后处理通道调度：降分辨率（低、中档 1/4，高档起 1/2）的 fog（氛围、天空、雾穹）与 fx（光幕、冲击壳，低档并入 fog），
 * 全分辨率 scene 采样主画面合成写入 HDR 目标，中档起对 scene 做逐级降采样再加性升采样的泛光，
 * composite 合成回主画面。关档只走 scene 与 composite。
 */
final class RealmPostChain {
    private static final RealmPostTarget FOG = new RealmPostTarget();
    private static final RealmPostTarget FX = new RealmPostTarget();
    private static final RealmPostTarget SCENE = new RealmPostTarget();
    private static final RealmPostTarget[] BLOOM = new RealmPostTarget[RealmFxQuality.ULTRA.bloomLevels];

    static {
        for (int i = 0; i < BLOOM.length; i++) BLOOM[i] = new RealmPostTarget();
    }

    private RealmPostChain() {
    }

    static void render(RealmPostFrame frame, RenderTarget main) {
        ShaderInstance fog = AvernusShaders.Effect.POST_FOG.get();
        ShaderInstance fx = AvernusShaders.Effect.POST_FX.get();
        ShaderInstance scene = AvernusShaders.Effect.POST_SCENE.get();
        ShaderInstance composite = AvernusShaders.Effect.POST_COMPOSITE.get();
        boolean legacy = frame.quality() == RealmFxQuality.OFF;
        boolean merged = frame.quality() == RealmFxQuality.LOW;
        int downscale = frame.quality().atLeast(RealmFxQuality.HIGH) ? 2 : 4;
        if (scene == null || composite == null || !legacy && fog == null || !legacy && !merged && fx == null) return;
        GlSnapshot state = GlSnapshot.capture();
        int filter = main.filterMode;
        try {
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            SCENE.resize(main.width, main.height);
            if (legacy) FOG.close();
            else coarse(fog, FOG, frame, main, downscale);
            if (legacy || merged) FX.close();
            else coarse(fx, FX, frame, main, downscale);

            main.setFilterMode(GL_LINEAR);
            frame.apply(scene);
            scene.safeGetUniform("Downscale").set(downscale);
            scene.setSampler("DiffuseSampler", main.getColorTextureId());
            scene.setSampler("DepthSampler", main.getDepthTextureId());
            scene.setSampler("FogSampler", FOG.texture());
            scene.setSampler("FxSampler", FX.texture());
            scene.setSampler("NoiseSampler", RealmNoise.id());
            SCENE.bindWrite();
            draw(scene);
            main.setFilterMode(filter);
            int levels = bloom(frame, main);

            frame.apply(composite);
            composite.setSampler("SceneSampler", SCENE.texture());
            composite.setSampler("BloomSampler", levels > 0 ? BLOOM[0].texture() : SCENE.texture());
            main.bindWrite(true);
            draw(composite);
        } finally {
            if (main.filterMode != filter) main.setFilterMode(filter);
            state.restore();
        }
    }

    /**
     * 泛光金字塔：第 0 级为半分辨率，逐级减半；下采样后自最小级起加性升采样叠回上一级。
     * @return 实际使用的级数，0 表示本档无泛光或着色器缺失
     */
    private static int bloom(RealmPostFrame frame, RenderTarget main) {
        ShaderInstance down = AvernusShaders.Effect.POST_BLOOM_DOWN.get();
        ShaderInstance up = AvernusShaders.Effect.POST_BLOOM_UP.get();
        int levels = down == null || up == null ? 0 : frame.quality().bloomLevels;
        for (int i = levels; i < BLOOM.length; i++) BLOOM[i].close();
        if (levels == 0) return 0;
        int width = main.width, height = main.height;
        for (int i = 0; i < levels; i++) {
            width = Math.max(1, width / 2);
            height = Math.max(1, height / 2);
            BLOOM[i].resize(width, height);
            frame.apply(down);
            down.safeGetUniform("Prefilter").set(i == 0 ? 1 : 0);
            down.setSampler("SourceSampler", (i == 0 ? SCENE : BLOOM[i - 1]).texture());
            BLOOM[i].bindWrite();
            draw(down);
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL_ONE, GL_ONE);
        glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);
        for (int i = levels - 1; i > 0; i--) {
            up.setSampler("SourceSampler", BLOOM[i].texture());
            BLOOM[i - 1].bindWrite();
            draw(up);
        }
        RenderSystem.disableBlend();
        return levels;
    }

    /** 降分辨率通道：每个 n×n 块取左下像素的深度。 */
    private static void coarse(ShaderInstance shader, RealmPostTarget target, RealmPostFrame frame, RenderTarget main, int downscale) {
        target.resize(Math.ceilDiv(main.width, downscale), Math.ceilDiv(main.height, downscale));
        frame.apply(shader);
        shader.safeGetUniform("Downscale").set(downscale);
        shader.setSampler("DepthSampler", main.getDepthTextureId());
        shader.setSampler("NoiseSampler", RealmNoise.id());
        target.bindWrite();
        draw(shader);
    }

    static void close() {
        FOG.close();
        FX.close();
        SCENE.close();
        for (RealmPostTarget level : BLOOM) level.close();
        RealmNoise.close();
    }

    /** 全屏四边形，顶点即裁剪坐标。 */
    private static void draw(ShaderInstance shader) {
        RenderSystem.setShader(() -> shader);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(-1.0F, -1.0F, 0.0F);
        builder.addVertex(1.0F, -1.0F, 0.0F);
        builder.addVertex(1.0F, 1.0F, 0.0F);
        builder.addVertex(-1.0F, 1.0F, 0.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    /** 后处理前后需要原样保留的 GL 状态。 */
    private record GlSnapshot(boolean depth, boolean depthWrite, boolean blend, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha,
                              int equationRgb, int equationAlpha, int[] viewport, int readFramebuffer, int drawFramebuffer) {
        static GlSnapshot capture() {
            int[] viewport = new int[4];
            glGetIntegerv(GL_VIEWPORT, viewport);
            return new GlSnapshot(glIsEnabled(GL_DEPTH_TEST), glGetBoolean(GL_DEPTH_WRITEMASK), glIsEnabled(GL_BLEND),
                    glGetInteger(GL_BLEND_SRC_RGB), glGetInteger(GL_BLEND_DST_RGB), glGetInteger(GL_BLEND_SRC_ALPHA), glGetInteger(GL_BLEND_DST_ALPHA),
                    glGetInteger(GL_BLEND_EQUATION_RGB), glGetInteger(GL_BLEND_EQUATION_ALPHA), viewport,
                    GlStateManager._getInteger(GL_READ_FRAMEBUFFER_BINDING), GlStateManager._getInteger(GL_DRAW_FRAMEBUFFER_BINDING));
        }

        void restore() {
            RenderSystem.depthMask(depthWrite);
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            glBlendEquationSeparate(equationRgb, equationAlpha);
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, readFramebuffer);
            GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        }
    }
}
