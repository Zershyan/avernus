package io.zershyan.avernus.mixin.avernus.client;

import io.zershyan.avernus.util.mixin.avernus.client.IMixinViewArea;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ViewArea.class)
public interface MixinViewArea extends IMixinViewArea {
    @Override
    @Invoker("getRenderSectionAt")
    @Nullable SectionRenderDispatcher.RenderSection avernus$sectionAt(BlockPos pos);
}
