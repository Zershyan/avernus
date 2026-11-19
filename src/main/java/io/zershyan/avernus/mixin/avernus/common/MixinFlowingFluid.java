package io.zershyan.avernus.mixin.avernus.common;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.zershyan.avernus.realm.RealmPhysics;
import io.zershyan.avernus.realm.RealmProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 流体不冲毁受保护的原方块。 */
@Mixin(FlowingFluid.class)
public abstract class MixinFlowingFluid {
    @ModifyReturnValue(method = "canSpreadTo", at = @At("RETURN"))
    private boolean avernus$protect(boolean original, BlockGetter getter, BlockPos fromPos, BlockState fromState, Direction direction,
                                    BlockPos toPos, BlockState toState, FluidState toFluid, Fluid fluid) {
        if (!original) return false;
        Level level = RealmPhysics.levelOf(getter);
        return level == null || !RealmProtection.protectsReplacement(level, toPos, toState);
    }
}
