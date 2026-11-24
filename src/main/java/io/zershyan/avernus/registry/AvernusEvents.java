package io.zershyan.avernus.registry;

import io.zershyan.avernus.datagen.DataGeneratorHandler;
import io.zershyan.avernus.handler.client.MirrorClientHandler;
import io.zershyan.avernus.handler.client.RealmRenderHandler;
import io.zershyan.avernus.handler.common.RealmAttributeHandler;
import io.zershyan.avernus.handler.common.RealmLifecycleHandler;
import io.zershyan.avernus.handler.common.RealmPresenceHandler;
import io.zershyan.avernus.handler.common.RealmProtectionHandler;
import io.zershyan.avernus.handler.common.RealmTraitHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.loading.DatagenModLoader;

public final class AvernusEvents {
    public static void doRegister(IEventBus modBus, Dist dist) {
        IEventBus gameBus = NeoForge.EVENT_BUS;

        gameBus.register(RealmLifecycleHandler.class);
        gameBus.register(RealmAttributeHandler.class);
        gameBus.register(RealmPresenceHandler.class);
        gameBus.register(RealmProtectionHandler.class);
        gameBus.register(RealmTraitHandler.class);
        if (dist.isClient()) Client.register(gameBus);
        if (DatagenModLoader.isRunningDataGen()) Data.register(modBus);
    }

    private static class Client {
        static void register(IEventBus gameBus) {
            gameBus.register(MirrorClientHandler.class);
            gameBus.register(RealmRenderHandler.class);
        }
    }

    private static class Data {
        static void register(IEventBus bus) {
            bus.addListener(DataGeneratorHandler::gatherData);
        }
    }
}
