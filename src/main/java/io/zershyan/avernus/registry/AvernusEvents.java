package io.zershyan.avernus.registry;

import io.zershyan.avernus.client.particle.RealmParticles;
import io.zershyan.avernus.datagen.DataGeneratorHandler;
import io.zershyan.avernus.handler.client.MirrorClientHandler;
import io.zershyan.avernus.handler.client.RealmRenderHandler;
import io.zershyan.avernus.handler.common.RealmLifecycleHandler;
import io.zershyan.avernus.handler.common.RealmPresenceHandler;
import io.zershyan.avernus.handler.common.RealmProtectionHandler;
import io.zershyan.avernus.handler.common.RealmTraitHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.loading.DatagenModLoader;

public final class AvernusEvents {
    public static void doRegister(IEventBus gameBus, IEventBus modBus, Dist dist) {
        gameBus.register(RealmLifecycleHandler.class);
        gameBus.register(RealmPresenceHandler.class);
        gameBus.register(RealmProtectionHandler.class);
        gameBus.register(RealmTraitHandler.class);
        if (dist.isClient()) Client.register(gameBus, modBus);
        if (DatagenModLoader.isRunningDataGen()) Data.register(modBus);
    }

    private static class Client {
        static void register(IEventBus bus, IEventBus modBus) {
            bus.register(MirrorClientHandler.class);
            bus.register(RealmRenderHandler.class);
            modBus.addListener(RealmParticles::registerProviders);
        }
    }

    private static class Data {
        static void register(IEventBus bus) {
            bus.addListener(DataGeneratorHandler::gatherData);
        }
    }
}
