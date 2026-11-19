package io.zershyan.avernus.mixin.avernus.common;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.zershyan.avernus.realm.RealmPhysics;
import io.zershyan.avernus.util.mixin.avernus.common.IMixinPathNavigationRegion;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** 每次寻路的区域视图使用发起者解析隔绝与重叠。 */
@Mixin(PathNavigationRegion.class)
public abstract class MixinPathNavigationRegion implements IMixinPathNavigationRegion {
    @Shadow
    @Final
    protected Level level;

    @Unique private @Nullable Mob avernus$subject;

    @Override
    public Level avernus$level() { return level; }

    @Override
    public @Nullable Mob avernus$subject() { return avernus$subject; }

    @Override
    public void avernus$setSubject(Mob mob) { avernus$subject = mob; }
    @ModifyReturnValue(method = "getBlockState", at = @At("RETURN"))
    private BlockState avernus$effectiveBlock(BlockState original, BlockPos pos) {
        return RealmPhysics.collision(level, avernus$subject, pos, original);
    }

    @ModifyReturnValue(method = "getFluidState", at = @At("RETURN"))
    private FluidState avernus$effectiveFluid(FluidState original, BlockPos pos) {
        return RealmPhysics.fluid(level, avernus$subject, pos, original);
    }
}
