package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.data_component.MirrorForm;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AvernusDataComponents {
    public static final DeferredRegister.DataComponents REGISTER =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Avernus.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MirrorForm>> MIRROR_FORM =
            REGISTER.registerComponentType("mirror_form", builder -> builder
                    .persistent(MirrorForm.CODEC)
                    .networkSynchronized(MirrorForm.STREAM_CODEC)
                    .cacheEncoding());

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
