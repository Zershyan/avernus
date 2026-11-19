package io.zershyan.avernus.compat.curios;

import io.zershyan.avernus.compat.ICompatUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class CuriosCompat implements ICompatUtils {
    @Override
    public boolean isModLoaded() {
        return ModList.get().isLoaded("curios");
    }

    public void register(IEventBus modBus) {
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(CuriosAccess::registerMirror));
    }

    public ItemStack findWornMirror(LivingEntity entity) {
        return testLoadedAndCall(() -> CuriosAccess.findWornMirror(entity), ItemStack.EMPTY);
    }
}
