package io.zershyan.avernus.mixin.client;

import io.zershyan.avernus.client.realm_render.RealmRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 被领域消隐的方块实体不再绘制（箱子、告示牌文字、信标光柱等）。 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void avernus$hideCovered(E blockEntity, float partialTick, PoseStack poseStack,
                                                            MultiBufferSource bufferSource, CallbackInfo ci) {
        if (RealmRenderState.hasVisibleRealm() && RealmRenderState.hides(blockEntity.getBlockPos())) ci.cancel();
    }
}
