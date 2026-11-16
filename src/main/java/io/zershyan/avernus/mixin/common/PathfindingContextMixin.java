package io.zershyan.avernus.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.zershyan.avernus.realm.RealmPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathTypeCache;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 领域覆盖处绕开世界共享的 PathTypeCache，避免有效地形写入或读到真实缓存。 */
@Mixin(PathfindingContext.class)
public abstract class PathfindingContextMixin {
    @WrapOperation(method = "getPathTypeFromState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/pathfinder/PathTypeCache;getOrCompute(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/pathfinder/PathType;"))
    private PathType avernus$bypassCache(PathTypeCache cache, BlockGetter level, BlockPos pos, Operation<PathType> original) {
        if (RealmPhysics.realmAt(RealmPhysics.levelOf(level), null, pos) == null) return original.call(cache, level, pos);
        return WalkNodeEvaluatorInvoker.avernus$getPathTypeFromState(level, pos);
    }
}
