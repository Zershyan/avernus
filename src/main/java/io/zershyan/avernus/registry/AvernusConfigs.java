package io.zershyan.avernus.registry;

import io.zershyan.avernus.registry.config.ClientConfig;
import io.zershyan.avernus.registry.config.ServerConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLLoader;

public final class AvernusConfigs {
    private AvernusConfigs() {
    }

    public static void doRegister() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        if (FMLLoader.getDist().isClient()) Client.register(context);
    }

    private static final class Client {
        static void register(ModLoadingContext context) {
            context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        }
    }
}
