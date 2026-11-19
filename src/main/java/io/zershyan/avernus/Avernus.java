package io.zershyan.avernus;

import io.zershyan.avernus.compat.AvernusCompatFactory;
import io.zershyan.avernus.registry.AvernusAttachments;
import io.zershyan.avernus.registry.AvernusConfigs;
import io.zershyan.avernus.registry.AvernusDataComponents;
import io.zershyan.avernus.registry.AvernusEntities;
import io.zershyan.avernus.registry.AvernusEvents;
import io.zershyan.avernus.registry.AvernusItemModels;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.AvernusKeyMappings;
import io.zershyan.avernus.registry.AvernusPackets;
import io.zershyan.avernus.registry.AvernusParticles;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.AvernusRecipes;
import io.zershyan.avernus.registry.AvernusRegistries;
import io.zershyan.avernus.registry.AvernusShaders;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Avernus.MODID)
public class Avernus {
    public static final String MODID = "avernus";
    public static final Logger LOGGER = LoggerFactory.getLogger(Avernus.class);

    public Avernus(IEventBus modEventBus, Dist dist, ModContainer modContainer) {
        AvernusRegistries.doRegister(modEventBus);
        AvernusRealmAspects.doRegister(modEventBus);
        AvernusRealmTraits.doRegister(modEventBus);
        AvernusDataComponents.doRegister(modEventBus);
        AvernusAttachments.doRegister(modEventBus);
        AvernusItems.doRegister(modEventBus);
        AvernusParticles.doRegister(modEventBus);
        AvernusEntities.doRegister(modEventBus, dist);
        AvernusRecipes.doRegister(modEventBus);
        AvernusPackets.doRegister(modEventBus);
        AvernusConfigs.doRegister(modContainer, dist);
        AvernusEvents.doRegister(NeoForge.EVENT_BUS, modEventBus, dist);
        AvernusCompatFactory.doRegister(modEventBus);

        if (dist.isClient()) {
            AvernusItemModels.doRegister(modEventBus);
            AvernusShaders.doRegister(modEventBus);
            AvernusKeyMappings.doRegister(modEventBus);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
