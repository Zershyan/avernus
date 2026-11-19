package io.zershyan.avernus.mixin.avernus.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.zershyan.avernus.realm.RealmPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 射线（视线、交互、投射物、第三人称相机）的方块与流体形状取有效地形。 */
@Mixin(ClipContext.class)
public abstract class MixinClipContext {
    @Shadow
    @Final
    private CollisionContext collisionContext;

    @ModifyVariable(method = "getBlockShape", at = @At("HEAD"), argsOnly = true)
    private BlockState avernus$effectiveBlock(BlockState state, BlockState original, BlockGetter level, BlockPos pos) {
        return RealmPhysics.collision(RealmPhysics.levelOf(level), RealmPhysics.subjectOf(collisionContext), pos, state);
    }

    @WrapOperation(method = "getFluidShape", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape avernus$surface(FluidState state, BlockGetter level, BlockPos pos, Operation<VoxelShape> original) {
        float height = RealmPhysics.surfaceHeight(level, RealmPhysics.subjectOf(collisionContext), pos, state);
        return height < 0.0F ? original.call(state, level, pos) : Shapes.box(0, 0, 0, 1, height, 1);
    }
    @ModifyVariable(method = "getFluidShape", at = @At("HEAD"), argsOnly = true)
    private FluidState avernus$effectiveFluid(FluidState state, FluidState original, BlockGetter level, BlockPos pos) {
        return RealmPhysics.fluid(RealmPhysics.levelOf(level), RealmPhysics.subjectOf(collisionContext), pos, state);
    }
}
