package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.item.MirrorItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AvernusItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(Avernus.MODID);

    public static final DeferredItem<MirrorItem> MIRROR = REGISTER.register("mirror", MirrorItem::new);

    private static void addToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(MIRROR);
    }

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
        modBus.addListener(AvernusItems::addToTabs);
    }
}
