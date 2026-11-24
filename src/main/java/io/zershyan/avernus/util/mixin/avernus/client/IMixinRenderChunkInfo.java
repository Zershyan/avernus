package io.zershyan.avernus.util.mixin.avernus.client;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

public interface IMixinRenderChunkInfo {
    ChunkRenderDispatcher.RenderChunk avernus$chunk();
}
