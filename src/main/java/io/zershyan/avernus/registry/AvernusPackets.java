package io.zershyan.avernus.registry;

import io.zershyan.avernus.registry.packet.RealmMotionPacket;
import io.zershyan.avernus.registry.packet.RealmRemovePacket;
import io.zershyan.avernus.registry.packet.RealmSnapshotPacket;
import io.zershyan.avernus.registry.packet.RealmUpdatePacket;
import io.zershyan.avernus.registry.packet.ToggleWornMirrorPacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AvernusPackets {
    /** 协议版本独立固定，字段变化时手动升级。 */
    private static final String PROTOCOL_VERSION = "2";

    private static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(RealmUpdatePacket.TYPE, RealmUpdatePacket.STREAM_CODEC, RealmUpdatePacket::handle);
        registrar.playToClient(RealmRemovePacket.TYPE, RealmRemovePacket.STREAM_CODEC, RealmRemovePacket::handle);
        registrar.playToClient(RealmMotionPacket.TYPE, RealmMotionPacket.STREAM_CODEC, RealmMotionPacket::handle);
        registrar.playToServer(ToggleWornMirrorPacket.TYPE, ToggleWornMirrorPacket.STREAM_CODEC, ToggleWornMirrorPacket::handle);
        registrar.playToClient(RealmSnapshotPacket.TYPE, RealmSnapshotPacket.STREAM_CODEC, RealmSnapshotPacket::handle);
    }

    public static void doRegister(IEventBus modBus) {
        modBus.addListener(AvernusPackets::register);
    }
}
