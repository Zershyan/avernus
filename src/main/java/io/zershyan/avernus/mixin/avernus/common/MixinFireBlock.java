package io.zershyan.avernus.mixin.avernus.common;

import io.zershyan.avernus.realm.RealmProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 火焰不烧毁受保护方块。 */
@Mixin(FireBlock.class)
public abstract class MixinFireBlock {
    @Inject(method = "checkBurnOut", at = @At("HEAD"), cancellable = true)
    private void avernus$protect(Level level, BlockPos pos, int chance, RandomSource random, int age, Direction face, CallbackInfo ci) {
        if (RealmProtection.isProtected(level, pos)) ci.cancel();
    }
}
