package io.zershyan.avernus.registry;

import io.zershyan.avernus.registry.config.ServerConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

public class AvernusConfigs {
    public static void doRegister(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }
}
