package io.zershyan.avernus.mixin.avernus.client;

import io.zershyan.avernus.util.mixin.avernus.client.IMixinRenderChunkInfo;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer$RenderChunkInfo")
public interface MixinRenderChunkInfo extends IMixinRenderChunkInfo {
    @Override
    @Accessor("chunk")
    ChunkRenderDispatcher.RenderChunk avernus$chunk();
}
