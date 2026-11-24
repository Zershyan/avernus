package io.zershyan.avernus.compat.curios;

import io.zershyan.avernus.compat.ICompatUtils;
import io.zershyan.avernus.compat.curios.event.CuriosClientSetupEvent;
import io.zershyan.avernus.compat.curios.event.CuriosCommonSetupEvent;
import io.zershyan.avernus.compat.curios.registry.CuriosToggleMirrorPacket;
import io.zershyan.avernus.compat.curios.util.CuriosAccess;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.registration.PayloadRegistrar;

public final class CuriosCompat implements ICompatUtils {
    public static final String MODID = "curios";

    @Override
    public boolean isModLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    @Override
    public void addCommonListener(IEventBus forgeBus, IEventBus modBus) {
        modBus.addListener(CuriosCommonSetupEvent::onCommonSetup);
    }

    @Override
    public void addClientListener(IEventBus forgeBus, IEventBus modBus) {
        modBus.addListener(CuriosClientSetupEvent::onRegisterKeyMappings);
    }

    @Override
    public void registerNetwork(PayloadRegistrar registrar) {
        CuriosToggleMirrorPacket.doRegister(registrar);
    }

    public ItemStack findWornMirror(LivingEntity entity) {
        return testLoadedAndCall(() -> CuriosAccess.findWornMirror(entity), ItemStack.EMPTY);
    }
}
