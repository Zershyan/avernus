package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.data_component.MirrorTraits;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public class AvernusDataComponents {
    public static final DeferredRegister.DataComponents REGISTER =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Avernus.MODID);

    public static final RegistryObject<DataComponentType<?>, DataComponentType<MirrorTraits>> MIRROR_TRAITS =
            REGISTER.registerComponentType("mirror_traits", builder -> builder
                    .persistent(MirrorTraits.CODEC)
                    .networkSynchronized(MirrorTraits.STREAM_CODEC)
                    .cacheEncoding());

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
