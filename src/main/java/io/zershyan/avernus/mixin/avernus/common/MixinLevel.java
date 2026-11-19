package io.zershyan.avernus.mixin.avernus.common;

import io.zershyan.avernus.realm.RealmProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class MixinLevel {
    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
            at = @At("HEAD"), cancellable = true)
    private void avernus$protectBeforeDrops(BlockPos pos, boolean drop, @Nullable Entity source, int limit,
                                             CallbackInfoReturnable<Boolean> result) {
        if ((Object) this instanceof ServerLevel level && !level.restoringBlockSnapshots
                && RealmProtection.isProtected(level, pos)) result.setReturnValue(false);
    }
}
