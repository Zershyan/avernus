package io.zershyan.avernus.mixin.avernus.common;

import io.zershyan.avernus.util.mixin.avernus.common.IMixinPathNavigationRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 每次寻路创建的区域视图绑定发起者，供有效地形按主体解析隔绝与重叠。 */
@Mixin(PathNavigation.class)
public abstract class MixinPathNavigation {
    @Shadow
    @Final
    protected Mob mob;

    @Redirect(method = "createPath(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/pathfinder/Path;",
            at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/PathNavigationRegion;"))
    private PathNavigationRegion avernus$bindSubject(Level level, BlockPos from, BlockPos to) {
        PathNavigationRegion region = new PathNavigationRegion(level, from, to);
        ((IMixinPathNavigationRegion) region).avernus$setSubject(mob);
        return region;
    }
}
