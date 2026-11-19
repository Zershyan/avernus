package io.zershyan.avernus.mixin.avernus.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.zershyan.avernus.realm.RealmPhysics;
import io.zershyan.avernus.realm.RealmTraitEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 移动时的地面摩擦与流体阻力读取有效地形；往生先于不死图腾判定。 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {
    @WrapOperation(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState avernus$ground(Level level, BlockPos pos, Operation<BlockState> original) {
        return RealmPhysics.interior(level, (LivingEntity) (Object) this, pos, original.call(level, pos));
    }

    @WrapOperation(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
    private FluidState avernus$fluid(Level level, BlockPos pos, Operation<FluidState> original) {
        return RealmPhysics.fluid(level, (LivingEntity) (Object) this, pos, original.call(level, pos));
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void avernus$rebirth(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (RealmTraitEffects.tryRebirth((LivingEntity) (Object) this, source)) cir.setReturnValue(true);
    }
}
