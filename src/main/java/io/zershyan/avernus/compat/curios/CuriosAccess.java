package io.zershyan.avernus.compat.curios;

import io.zershyan.avernus.api.AvernusApi;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/** 直接引用 Curios API 的唯一位置，只经 {@link CuriosCompat} 在已安装时调用。 */
final class CuriosAccess {
    private static final String BODY_SLOT = "body";

    private CuriosAccess() {
    }

    static ItemStack findWornMirror(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(inventory -> inventory.findFirstCurio(AvernusApi::isMirror))
                .filter(result -> BODY_SLOT.equals(result.slotContext().identifier()))
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
    }
}
