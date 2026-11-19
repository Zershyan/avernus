package io.zershyan.avernus.client.mirror;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.zershyan.avernus.registry.AvernusShaders;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** 镜子专用图层：本体半透明写深度，残影加法混合不写深度；共用镜子着色器。 */
final class MirrorRenderTypes extends RenderType {
    private static final ShaderStateShard MIRROR_SHADER = new ShaderStateShard(AvernusShaders.Effect.MIRROR::get);

    private static final Function<ResourceLocation, RenderType> BODY = Util.memoize(texture -> create("avernus_mirror",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, CompositeState.builder()
                    .setShaderState(MIRROR_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(true)));

    private static final Function<ResourceLocation, RenderType> GHOST = Util.memoize(texture -> create("avernus_mirror_ghost",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true, CompositeState.builder()
                    .setShaderState(MIRROR_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)));

    private MirrorRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                              boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    static RenderType body(ResourceLocation texture) {
        return BODY.apply(texture);
    }

    static RenderType ghost(ResourceLocation texture) {
        return GHOST.apply(texture);
    }
}
