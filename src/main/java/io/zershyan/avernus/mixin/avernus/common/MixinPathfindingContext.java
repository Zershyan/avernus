package io.zershyan.avernus.mixin.avernus.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.zershyan.avernus.realm.RealmPhysics;
import io.zershyan.avernus.util.mixin.avernus.common.IMixinPathNavigationRegion;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathTypeCache;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 领域覆盖处绕开世界共享的 PathTypeCache，避免有效地形写入或读到真实缓存。 */
@Mixin(PathfindingContext.class)
public abstract class MixinPathfindingContext {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void avernus$bindSubject(CollisionGetter level, Mob mob, CallbackInfo ci) {
        if (level instanceof IMixinPathNavigationRegion region) region.avernus$setSubject(mob);
    }
    @WrapOperation(method = "getPathTypeFromState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/pathfinder/PathTypeCache;getOrCompute(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/pathfinder/PathType;"))
    private PathType avernus$bypassCache(PathTypeCache cache, BlockGetter level, BlockPos pos, Operation<PathType> original) {
        if (!(level instanceof IMixinPathNavigationRegion region) || RealmPhysics.realmAt(region.avernus$level(), region.avernus$subject(), pos) == null) return original.call(cache, level, pos);
        return MixinWalkNodeEvaluator.avernus$getPathTypeFromState(level, pos);
    }
}
