package io.zershyan.avernus.mixin.avernus.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.realm.RealmPhysics;
import io.zershyan.avernus.realm.RealmPresenceTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/** 实体内部状态（窒息、方块内效果、脚下方块、速度/跳跃系数）与流体接触读取有效地形。 */
@Mixin(Entity.class)
public abstract class MixinEntity {
    @Inject(method = "teleportTo(DDD)V", at = @At("RETURN"))
    private void avernus$snapPosition(CallbackInfo ci) {
        RealmManager.ownerTeleported((Entity) (Object) this);
    }

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z", at = @At("RETURN"))
    private void avernus$snapSameLevel(ServerLevel level, double x, double y, double z, Set<RelativeMovement> relative,
                                       float yaw, float pitch, CallbackInfoReturnable<Boolean> ci) {
        Entity entity = (Entity) (Object) this;
        if (ci.getReturnValueZ() && entity.level() == level) RealmManager.ownerTeleported(entity);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void avernus$vehiclePresence(CallbackInfo ci) {
        if (!((Object) this instanceof LivingEntity)) RealmPresenceTracker.tick((Entity) (Object) this);
    }

    @Inject(method = "checkFallDamage", at = @At("HEAD"))
    private void avernus$beforeFallCheck(double dy, boolean onGround, BlockState state, BlockPos pos, CallbackInfo ci) {
        if ((Object) this instanceof LivingEntity living) RealmPresenceTracker.beforeFallCheck(living, onGround, dy, pos);
    }

    @Inject(method = "checkFallDamage", at = @At("RETURN"))
    private void avernus$afterFallCheck(double dy, boolean onGround, BlockState state, BlockPos pos, CallbackInfo ci) {
        if (onGround && (Object) this instanceof LivingEntity living && !living.level().isClientSide) {
            RealmPresenceTracker.consumeFallGuard(living);
        }
    }

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

    @WrapOperation(method = {"updateFluidHeightAndDoFluidPushing(Ljava/util/function/Predicate;)V", "updateFluidOnEyes"},
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;getHeight(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
    private float avernus$surface(FluidState fluid, BlockGetter level, BlockPos pos, Operation<Float> original) {
        float height = RealmPhysics.surfaceHeight(level, (Entity) (Object) this, pos, fluid);
        return height < 0.0F ? original.call(fluid, level, pos) : height;
    }

    @WrapOperation(method = "updateFluidHeightAndDoFluidPushing(Ljava/util/function/Predicate;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 avernus$stillWater(FluidState fluid, BlockGetter level, BlockPos pos, Operation<Vec3> original) {
        var realm = RealmPhysics.realmAt(RealmPhysics.levelOf(level), (Entity) (Object) this, pos);
        return realm != null && pos.getY() == realm.height() - 1 ? Vec3.ZERO : original.call(fluid, level, pos);
    }
    @WrapOperation(method = {"updateFluidHeightAndDoFluidPushing(Ljava/util/function/Predicate;)V", "updateFluidOnEyes"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
    private FluidState avernus$fluid(Level level, BlockPos pos, Operation<FluidState> original) {
        return RealmPhysics.fluid(level, (Entity) (Object) this, pos, original.call(level, pos));
    }
}
