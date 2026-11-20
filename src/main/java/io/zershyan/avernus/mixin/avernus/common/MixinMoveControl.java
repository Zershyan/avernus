package io.zershyan.avernus.mixin.avernus.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.zershyan.avernus.realm.RealmPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** 跟随路径时的起跳与侧移判定读取有效地形，避免被境面下的真实方块误导。 */
@Mixin(MoveControl.class)
public abstract class MixinMoveControl {
    @Shadow
    @Final
    protected Mob mob;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState avernus$jumpCheck(Level level, BlockPos pos, Operation<BlockState> original) {
        return RealmPhysics.collision(level, mob, pos, original.call(level, pos));
    }

    @WrapOperation(method = "isWalkable", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/pathfinder/NodeEvaluator;getPathType(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/pathfinder/PathType;"))
    private PathType avernus$strafe(NodeEvaluator evaluator, Mob subject, BlockPos pos, Operation<PathType> original) {
        PathType type = RealmPhysics.pathType(evaluator, subject, pos);
        return type != null ? type : original.call(evaluator, subject, pos);
    }
}
