package io.zershyan.avernus.mixin.avernus.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.zershyan.avernus.realm.RealmPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** 侧移判定读取有效地形，避免被境面下的真实方块误导。 */
@Mixin(MoveControl.class)
public abstract class MixinMoveControl {
    @Shadow
    @Final
    protected Mob mob;

    @WrapOperation(method = "isWalkable", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/pathfinder/NodeEvaluator;getBlockPathType(Lnet/minecraft/world/level/BlockGetter;III)Lnet/minecraft/world/level/pathfinder/BlockPathTypes;"))
    private BlockPathTypes avernus$strafe(NodeEvaluator evaluator, BlockGetter level, int x, int y, int z, Operation<BlockPathTypes> original) {
        BlockPathTypes type = RealmPhysics.pathType(evaluator, mob, new BlockPos(x, y, z));
        return type != null ? type : original.call(evaluator, level, x, y, z);
    }

    /** MOVE_TO 用脚下真实方块判断是否起跳，领域内改读有效地形以免被台阶顶起。 */
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState avernus$selfBlock(Level level, BlockPos pos, Operation<BlockState> original) {
        return RealmPhysics.collision(level, mob, pos, original.call(level, pos));
    }
}
