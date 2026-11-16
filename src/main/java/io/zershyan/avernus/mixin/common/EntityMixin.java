package io.zershyan.avernus.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.zershyan.avernus.realm.RealmPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 实体内部状态（窒息、方块内效果、脚下方块、速度/跳跃系数）与流体接触读取有效地形。 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @WrapOperation(method = {"checkInsideBlocks", "lambda$isInWall$8", "getBlockSpeedFactor", "getBlockJumpFactor",
            "getOnPos(F)Lnet/minecraft/core/BlockPos;", "getBlockStateOn", "getBlockStateOnLegacy", "getInBlockState",
            "move", "lambda$checkFallDamage$3"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState avernus$interior(Level level, BlockPos pos, Operation<BlockState> original) {
        return RealmPhysics.interior(level, (Entity) (Object) this, pos, original.call(level, pos));
    }

    @WrapOperation(method = "moveTowardsClosestSpace", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState avernus$pushOut(Level level, BlockPos pos, Operation<BlockState> original) {
        return RealmPhysics.collision(level, (Entity) (Object) this, pos, original.call(level, pos));
    }

    @WrapOperation(method = {"updateFluidHeightAndDoFluidPushing()V", "updateFluidOnEyes"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
    private FluidState avernus$fluid(Level level, BlockPos pos, Operation<FluidState> original) {
        return RealmPhysics.fluid(level, (Entity) (Object) this, pos, original.call(level, pos));
    }
}
