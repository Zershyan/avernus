package io.zershyan.avernus.mixin.avernus.common;

import io.zershyan.avernus.realm.RealmProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 领域覆盖列如同冒险模式：任何游戏模式都无法开始挖掘，也不能对其使用桶、刷怪蛋等改动方块的物品。 */
@Mixin(Player.class)
public abstract class MixinPlayer {
    @Inject(method = "blockActionRestricted", at = @At("HEAD"), cancellable = true)
    private void avernus$restrictBreak(Level level, BlockPos pos, GameType gameMode, CallbackInfoReturnable<Boolean> cir) {
        if (RealmProtection.isProtected(level, pos)) cir.setReturnValue(true);
    }

    @Inject(method = "mayUseItemAt", at = @At("HEAD"), cancellable = true)
    private void avernus$restrictUse(BlockPos pos, Direction facing, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (RealmProtection.isProtected(player.level(), pos)) cir.setReturnValue(false);
    }
}
