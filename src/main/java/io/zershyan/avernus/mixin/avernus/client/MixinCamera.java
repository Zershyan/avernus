package io.zershyan.avernus.mixin.avernus.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.zershyan.avernus.realm.RealmPhysics;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** 相机所处流体与方块（水下雾、熔岩/细雪遮罩）读取有效地形。 */
@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow
    private Entity entity;

    @WrapOperation(method = "getFluidInCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getHeight(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
    private float avernus$surface(FluidState fluid, BlockGetter level, BlockPos pos, Operation<Float> original) {
        float height = RealmPhysics.surfaceHeight(level, entity, pos, fluid);
        return height < 0.0F ? original.call(fluid, level, pos) : height;
    }
    @WrapOperation(method = "getFluidInCamera", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/BlockGetter;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
    private FluidState avernus$fluid(BlockGetter level, BlockPos pos, Operation<FluidState> original) {
        return RealmPhysics.fluid(RealmPhysics.levelOf(level), entity, pos, original.call(level, pos));
    }

    @WrapOperation(method = {"getFluidInCamera", "getBlockAtCamera"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/BlockGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState avernus$block(BlockGetter level, BlockPos pos, Operation<BlockState> original) {
        return RealmPhysics.interior(RealmPhysics.levelOf(level), entity, pos, original.call(level, pos));
    }
}
