package io.zershyan.avernus.mixin.avernus.common;

import io.zershyan.avernus.realm.RealmProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 腐烂和融雪在移除方块前生成掉落。 */
@Mixin({LeavesBlock.class, SnowLayerBlock.class})
public abstract class MixinBlockDecay {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void avernus$protectDecay(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (RealmProtection.isProtected(level, pos)) ci.cancel();
    }
}
