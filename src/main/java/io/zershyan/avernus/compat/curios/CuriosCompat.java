package io.zershyan.avernus.compat.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Curios 可选兼容入口；未安装时不加载任何 Curios 类型。 */
public final class CuriosCompat {
    public static final String MODID = "curios";

    private CuriosCompat() {
    }

    public static boolean loaded() {
        return ModList.get().isLoaded(MODID);
    }

    /** 胸饰槽中实际佩戴的镜；未安装 Curios 或未佩戴时为空。 */
    public static ItemStack findWornMirror(LivingEntity entity) {
        return loaded() ? CuriosAccess.findWornMirror(entity) : ItemStack.EMPTY;
    }
}
