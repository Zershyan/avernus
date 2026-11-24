package io.zershyan.avernus.mixin.avernus.client;

import io.zershyan.avernus.util.mixin.avernus.client.IMixinViewArea;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ViewArea.class)
public interface MixinViewArea extends IMixinViewArea {
    @Override
    @Invoker("getRenderChunkAt")
    @Nullable ChunkRenderDispatcher.RenderChunk avernus$sectionAt(BlockPos pos);
}
