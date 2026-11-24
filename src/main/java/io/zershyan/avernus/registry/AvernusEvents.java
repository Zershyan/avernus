package io.zershyan.avernus.registry;

import io.zershyan.avernus.datagen.DataGeneratorHandler;
import io.zershyan.avernus.handler.client.MirrorClientHandler;
import io.zershyan.avernus.handler.client.RealmRenderHandler;
import io.zershyan.avernus.handler.common.RealmAttributeHandler;
import io.zershyan.avernus.handler.common.RealmLifecycleHandler;
import io.zershyan.avernus.handler.common.RealmPresenceHandler;
import io.zershyan.avernus.handler.common.RealmProtectionHandler;
import io.zershyan.avernus.handler.common.RealmTraitHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;

public final class AvernusEvents {
    public static void doRegister(IEventBus forgeBus, IEventBus modBus) {
        forgeBus.register(RealmLifecycleHandler.class);
        forgeBus.register(RealmAttributeHandler.class);
        forgeBus.register(RealmPresenceHandler.class);
        forgeBus.register(RealmProtectionHandler.class);
        forgeBus.register(RealmTraitHandler.class);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.register(forgeBus));
        modBus.addListener(DataGeneratorHandler::gatherData);
    }

    private static class Client {
        static void register(IEventBus forgeBus) {
            forgeBus.register(MirrorClientHandler.class);
            forgeBus.register(RealmRenderHandler.class);
        }
    }
}
