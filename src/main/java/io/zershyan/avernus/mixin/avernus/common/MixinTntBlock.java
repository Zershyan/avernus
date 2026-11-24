package io.zershyan.avernus.mixin.avernus.common;

import io.zershyan.avernus.realm.RealmProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TntBlock.class)
public abstract class MixinTntBlock {
    @Inject(method = "explode(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
    private static void avernus$preventPriming(Level level, BlockPos pos, @Nullable LivingEntity igniter, CallbackInfo ci) {
        if (RealmProtection.isProtected(level, pos)) ci.cancel();
    }

    @Inject(method = "wasExploded", at = @At("HEAD"), cancellable = true)
    private void avernus$preventChainReaction(Level level, BlockPos pos, Explosion explosion, CallbackInfo ci) {
        if (RealmProtection.isProtected(level, pos)) ci.cancel();
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void avernus$keepIgniter(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit,
                                     CallbackInfoReturnable<InteractionResult> ci) {
        ItemStack stack = player.getItemInHand(hand);
        if ((stack.is(Items.FIRE_CHARGE) || stack.is(Items.FLINT_AND_STEEL)) && RealmProtection.isProtected(level, pos)) {
            ci.setReturnValue(InteractionResult.FAIL);
        }
    }
}
