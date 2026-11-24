package io.zershyan.avernus.mixin.avernus.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.zershyan.avernus.realm.RealmPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GroundPathNavigation.class)
public abstract class MixinGroundPathNavigation extends PathNavigation {
    protected MixinGroundPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @WrapOperation(method = "createPath(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/pathfinder/Path;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState avernus$target(Level world, BlockPos pos, Operation<BlockState> original) {
        return RealmPhysics.collision(world, mob, pos, original.call(world, pos));
    }

    @WrapOperation(method = "getSurfaceY", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState avernus$surface(Level world, BlockPos pos, Operation<BlockState> original) {
        return RealmPhysics.interior(world, mob, pos, original.call(world, pos));
    }
}
