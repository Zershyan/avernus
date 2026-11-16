package io.zershyan.avernus.mixin.common;

import io.zershyan.avernus.realm.RealmPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 射线（视线、交互、投射物、第三人称相机）的方块与流体形状取有效地形。 */
@Mixin(ClipContext.class)
public abstract class ClipContextMixin {
    @Shadow
    @Final
    private CollisionContext collisionContext;

    @ModifyVariable(method = "getBlockShape", at = @At("HEAD"), argsOnly = true)
    private BlockState avernus$effectiveBlock(BlockState state, BlockState original, BlockGetter level, BlockPos pos) {
        return RealmPhysics.collision(RealmPhysics.levelOf(level), RealmPhysics.subjectOf(collisionContext), pos, state);
    }

    @ModifyVariable(method = "getFluidShape", at = @At("HEAD"), argsOnly = true)
    private FluidState avernus$effectiveFluid(FluidState state, FluidState original, BlockGetter level, BlockPos pos) {
        return RealmPhysics.fluid(RealmPhysics.levelOf(level), RealmPhysics.subjectOf(collisionContext), pos, state);
    }
}
