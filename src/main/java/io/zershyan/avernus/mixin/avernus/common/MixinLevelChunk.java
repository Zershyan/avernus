package io.zershyan.avernus.mixin.avernus.common;

import io.zershyan.avernus.realm.RealmPlacement;
import io.zershyan.avernus.realm.RealmProtection;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk {
    @Shadow @Final private Level level;

    @Inject(method = "setBlockState", at = @At("HEAD"), cancellable = true)
    private void avernus$protectWrite(BlockPos pos, BlockState state, boolean moving, CallbackInfoReturnable<BlockState> result) {
        if (!(level instanceof ServerLevel) || level.restoringBlockSnapshots || RealmSets.of(level).isEmpty()) return;
        BlockState before = ((LevelChunk) (Object) this).getBlockState(pos);
        // 活塞的过渡占位不是原方块，允许波纹到达前已开始的移动收尾。
        if (!before.is(Blocks.MOVING_PISTON) && RealmProtection.blocksMutation(level, pos, before, state)) {
            if (level.captureBlockSnapshots) RealmPlacement.deny(level);
            result.setReturnValue(null);
        }
    }
}
