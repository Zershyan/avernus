package io.zershyan.avernus.mixin.avernus.common;

import io.zershyan.avernus.realm.RealmProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 受保护列上的火焰不推进 tick，从而不延烧、不烧毁方块。 */
@Mixin(FireBlock.class)
public abstract class MixinFireBlock {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void avernus$protect(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (RealmProtection.isProtected(level, pos)) ci.cancel();
    }
}
