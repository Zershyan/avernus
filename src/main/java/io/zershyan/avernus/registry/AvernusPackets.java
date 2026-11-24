package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.compat.AvernusCompatFactory;
import io.zershyan.avernus.registry.packet.RealmMotionPacket;
import io.zershyan.avernus.registry.packet.RealmRemovePacket;
import io.zershyan.avernus.registry.packet.RealmSnapshotPacket;
import io.zershyan.avernus.registry.packet.RealmUpdatePacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AvernusPackets {
    /** 使用完整模组版本协商网络兼容性；版本不一致时拒绝连接。 */
    private static final String PROTOCOL_VERSION = ModList.get()
            .getModContainerById(Avernus.MODID)
            .map(container -> container.getModInfo().getVersion())
            .map(Object::toString)
            .orElse("unknown");

    private static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(RealmUpdatePacket.TYPE, RealmUpdatePacket.STREAM_CODEC, RealmUpdatePacket::handle);
        registrar.playToClient(RealmRemovePacket.TYPE, RealmRemovePacket.STREAM_CODEC, RealmRemovePacket::handle);
        registrar.playToClient(RealmMotionPacket.TYPE, RealmMotionPacket.STREAM_CODEC, RealmMotionPacket::handle);
        registrar.playToClient(RealmSnapshotPacket.TYPE, RealmSnapshotPacket.STREAM_CODEC, RealmSnapshotPacket::handle);
        AvernusCompatFactory.registerNetwork(registrar);
    }

    public static void doRegister(IEventBus modBus) {
        modBus.addListener(AvernusPackets::register);
    }
}
