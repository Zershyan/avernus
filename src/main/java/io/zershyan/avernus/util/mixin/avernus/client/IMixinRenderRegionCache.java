package io.zershyan.avernus.util.mixin.avernus.client;

import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IMixinRenderRegionCache {
    RenderChunkRegion avernus$createIncludingEmpty(Level level, BlockPos start, BlockPos end, int padding);
}
