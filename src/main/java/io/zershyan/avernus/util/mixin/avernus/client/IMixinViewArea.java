package io.zershyan.avernus.util.mixin.avernus.client;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public interface IMixinViewArea {
    @Nullable ChunkRenderDispatcher.RenderChunk avernus$sectionAt(BlockPos pos);
}
