package io.zershyan.avernus.mixin.avernus.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.zershyan.avernus.realm.RealmPhysics;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 被消隐的方块不再产生屏幕内部遮罩。 */
@Mixin(ScreenEffectRenderer.class)
public abstract class MixinScreenEffectRenderer {
    @WrapOperation(method = "getOverlayBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState avernus$effective(Level level, BlockPos pos, Operation<BlockState> original, @Local(argsOnly = true) Player player) {
        return RealmPhysics.interior(level, player, pos, original.call(level, pos));
    }
}
