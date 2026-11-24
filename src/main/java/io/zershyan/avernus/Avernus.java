package io.zershyan.avernus;

import io.zershyan.avernus.compat.AvernusCompatFactory;
import io.zershyan.avernus.registry.AvernusAttachments;
import io.zershyan.avernus.registry.AvernusConfigs;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Avernus.MODID)
public class Avernus {
    public static final String MODID = "avernus";
    public static final Logger LOGGER = LoggerFactory.getLogger(Avernus.class);

    public Avernus() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        AvernusConfigs.doRegister();
        AvernusPackets.doRegister();
        AvernusItems.doRegister(modBus);
        AvernusSounds.doRegister(modBus);
        AvernusRecipes.doRegister(modBus);
        AvernusEntities.doRegister(modBus);
        AvernusParticles.doRegister(modBus);
        AvernusRegistries.doRegister(modBus);
        AvernusAttachments.doRegister(modBus);
        AvernusRealmTraits.doRegister(modBus);
        AvernusRealmAspects.doRegister(modBus);
        AvernusEvents.doRegister(forgeBus, modBus);
        AvernusCompatFactory.register(forgeBus, modBus);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            AvernusShaders.doRegister(modBus);
            AvernusItemExtensions.doRegister(modBus);
            AvernusParticleProviders.doRegister(modBus);
        });
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
