package io.zershyan.avernus.mixin.avernus.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.zershyan.avernus.util.mixin.avernus.client.IMixinRenderRegionCache;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderRegionCache.class)
public abstract class MixinRenderRegionCache implements IMixinRenderRegionCache {
    @Unique private boolean avernus$includeEmpty;

    @Override
    public RenderChunkRegion avernus$createIncludingEmpty(Level level, BlockPos start, BlockPos end, int padding) {
        avernus$includeEmpty = true;
        try {
            return ((RenderRegionCache) (Object) this).createRegion(level, start, end, padding);
        } finally {
            avernus$includeEmpty = false;
        }
    }

    @ModifyExpressionValue(method = "createRegion", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/RenderRegionCache;isAllEmpty(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;II[[Lnet/minecraft/client/renderer/chunk/RenderRegionCache$ChunkInfo;)Z"))
    private boolean avernus$skipEmptyCheck(boolean empty) {
        return !avernus$includeEmpty && empty;
    }
}
