package io.zershyan.avernus.mixin.common;

import io.zershyan.avernus.realm.RealmProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 受保护的重力方块不转为下落实体。 */
@Mixin({FallingBlock.class, BrushableBlock.class})
public abstract class FallingBlockMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void avernus$protect(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (RealmProtection.isProtected(level, pos)) ci.cancel();
    }
}
