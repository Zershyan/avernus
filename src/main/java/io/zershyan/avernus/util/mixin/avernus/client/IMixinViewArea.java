package io.zershyan.avernus.util.mixin.avernus.client;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public interface IMixinViewArea {
    @Nullable RenderSection avernus$sectionAt(BlockPos pos);
}
