package io.zershyan.avernus.compat.curios;

import io.zershyan.avernus.api.AvernusApi;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

final class CuriosAccess {
    private static final String BODY = "body";

    static void registerMirror() {
        AvernusApi.mirrorItems().forEach(mirror -> CuriosApi.registerCurio(mirror, new ICurioItem() {}));
    }

    static ItemStack findWornMirror(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).flatMap(inventory -> inventory.getStacksHandler(BODY).map(body -> {
            var stacks = body.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (inventory.isSlotActive(BODY, i) && AvernusApi.isMirror(stack)) return stack;
            }
            return ItemStack.EMPTY;
        })).orElse(ItemStack.EMPTY);
    }
}
