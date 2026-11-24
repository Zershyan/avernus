package io.zershyan.avernus.mixin.avernus.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.zershyan.avernus.realm.RealmPlacement;
import io.zershyan.avernus.realm.RealmProtection;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ForgeHooks.class)
public abstract class MixinCommonHooks {
    @WrapOperation(method = "onPlaceItemIntoWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
    private static InteractionResult avernus$capture(Item item, UseOnContext context, Operation<InteractionResult> original) {
        return RealmPlacement.capture(context, () -> original.call(item, context));
    }

    /** 服务端放置同样视作冒险模式。 */
    @ModifyExpressionValue(method = "onPlaceItemIntoWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;mayBuild:Z"))
    private static boolean avernus$mayBuild(boolean mayBuild, UseOnContext context) {
        return mayBuild && !RealmProtection.forbidsUse(context);
    }
}
