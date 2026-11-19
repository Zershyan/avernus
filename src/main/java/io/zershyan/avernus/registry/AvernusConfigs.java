package io.zershyan.avernus.registry;

import io.zershyan.avernus.registry.config.ClientConfig;
import io.zershyan.avernus.registry.config.ServerConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class AvernusConfigs {
    public static void doRegister(ModContainer container, Dist dist) {
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        if (dist.isClient()) Client.register(container);
    }

    private static class Client {
        static void register(ModContainer container) {
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }
}
