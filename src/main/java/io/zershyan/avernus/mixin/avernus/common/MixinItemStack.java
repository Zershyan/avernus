package io.zershyan.avernus.mixin.avernus.common;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.zershyan.avernus.realm.RealmProtection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 对领域覆盖列使用物品时视为不可建造，与冒险模式一样不能放置方块。 */
@Mixin(ItemStack.class)
public abstract class MixinItemStack {
    @ModifyExpressionValue(method = "onItemUse", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;mayBuild:Z"))
    private boolean avernus$mayBuild(boolean mayBuild, UseOnContext context) {
        return mayBuild && !RealmProtection.forbidsUse(context);
    }
}
