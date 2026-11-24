package io.zershyan.avernus;

import io.zershyan.avernus.compat.AvernusCompatFactory;
import io.zershyan.avernus.registry.AvernusAttachments;
import io.zershyan.avernus.registry.AvernusConfigs;
import io.zershyan.avernus.registry.AvernusDataComponents;
import io.zershyan.avernus.registry.AvernusEntities;
import io.zershyan.avernus.registry.AvernusEvents;
import io.zershyan.avernus.registry.AvernusItemExtensions;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.AvernusPackets;
import io.zershyan.avernus.registry.AvernusParticleProviders;
import io.zershyan.avernus.registry.AvernusParticles;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.AvernusRecipes;
import io.zershyan.avernus.registry.AvernusRegistries;
import io.zershyan.avernus.registry.AvernusShaders;
import io.zershyan.avernus.registry.AvernusSounds;
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
        IEventBus neoEventBus = NeoForge.EVENT_BUS;

        AvernusItems.doRegister(modEventBus);
        AvernusSounds.doRegister(modEventBus);
        AvernusPackets.doRegister(modEventBus);
        AvernusRecipes.doRegister(modEventBus);
        AvernusParticles.doRegister(modEventBus);
        AvernusRegistries.doRegister(modEventBus);
        AvernusAttachments.doRegister(modEventBus);
        AvernusRealmTraits.doRegister(modEventBus);
        AvernusEvents.doRegister(modEventBus, dist);
        AvernusRealmAspects.doRegister(modEventBus);
        AvernusConfigs.doRegister(modContainer, dist);
        AvernusDataComponents.doRegister(modEventBus);
        AvernusEntities.doRegister(modEventBus, dist);
        AvernusCompatFactory.register(neoEventBus, modEventBus);

        if (dist.isClient()) {
            AvernusShaders.doRegister(modEventBus);
            AvernusItemExtensions.doRegister(modEventBus);
            AvernusParticleProviders.doRegister(modEventBus);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
