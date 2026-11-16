package io.zershyan.avernus.mixin.common;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.zershyan.avernus.realm.RealmPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** 寻路区域只服务于寻路，直接呈现有效地形（隔绝领域不对寻路生效）。 */
@Mixin(PathNavigationRegion.class)
public abstract class PathNavigationRegionMixin {
    @Shadow
    @Final
    protected Level level;

    @ModifyReturnValue(method = "getBlockState", at = @At("RETURN"))
    private BlockState avernus$effectiveBlock(BlockState original, BlockPos pos) {
        return RealmPhysics.collision(level, null, pos, original);
    }

    @ModifyReturnValue(method = "getFluidState", at = @At("RETURN"))
    private FluidState avernus$effectiveFluid(FluidState original, BlockPos pos) {
        return RealmPhysics.fluid(level, null, pos, original);
    }
}
