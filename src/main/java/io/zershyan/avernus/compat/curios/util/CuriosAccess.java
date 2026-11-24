package io.zershyan.avernus.compat.curios.util;

import io.zershyan.avernus.api.AvernusApi;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/** Curios 查询：读取胸饰槽中实际生效的镜子。 */
public final class CuriosAccess {
    private static final String BODY = "body";

    private CuriosAccess() {
    }

    public static ItemStack findWornMirror(LivingEntity entity) {
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
