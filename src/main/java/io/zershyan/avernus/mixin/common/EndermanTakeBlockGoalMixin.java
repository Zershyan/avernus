package io.zershyan.avernus.mixin.common;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.zershyan.avernus.realm.RealmProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 末影人不搬走受保护方块。 */
@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal")
public abstract class EndermanTakeBlockGoalMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean avernus$protect(boolean holdable, @Local Level level, @Local BlockPos pos) {
        return holdable && !RealmProtection.isProtected(level, pos);
    }
}
