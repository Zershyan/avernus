package io.zershyan.avernus.mixin.avernus.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.zershyan.avernus.realm.RealmPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** 下一个路点的落脚高度按有效地形计算，否则目标被抬高会触发起跳。 */
@Mixin(PathNavigation.class)
public abstract class MixinPathNavigation {
    @Shadow
    @Final
    protected Mob mob;

    @WrapOperation(method = "getGroundY", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState avernus$groundBelow(Level level, BlockPos pos, Operation<BlockState> original) {
        return RealmPhysics.collision(level, mob, pos, original.call(level, pos));
    }

    @WrapOperation(method = "getGroundY", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/pathfinder/WalkNodeEvaluator;getFloorLevel(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)D"))
    private double avernus$floor(BlockGetter level, BlockPos pos, Operation<Double> original) {
        return RealmPhysics.floorLevel(mob, pos, () -> original.call(level, pos));
    }
}
