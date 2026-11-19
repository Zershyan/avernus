package io.zershyan.avernus.mixin.avernus.common;

import io.zershyan.avernus.realm.RealmProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 受保护的钟乳石不坠落。 */
@Mixin(PointedDripstoneBlock.class)
public abstract class MixinPointedDripstoneBlock {
    @Inject(method = "spawnFallingStalactite", at = @At("HEAD"), cancellable = true)
    private static void avernus$protect(BlockState state, ServerLevel level, BlockPos pos, CallbackInfo ci) {
        if (RealmProtection.isProtected(level, pos)) ci.cancel();
    }
}
