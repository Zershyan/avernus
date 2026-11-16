package io.zershyan.avernus.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.zershyan.avernus.realm.RealmPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 箭矢在地/嵌墙检测读取有效地形，被消隐的方块不再托住箭。 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState avernus$effective(Level level, BlockPos pos, Operation<BlockState> original) {
        return RealmPhysics.collision(level, (AbstractArrow) (Object) this, pos, original.call(level, pos));
    }
}
