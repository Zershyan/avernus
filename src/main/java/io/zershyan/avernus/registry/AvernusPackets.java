package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.compat.AvernusCompatFactory;
import io.zershyan.avernus.registry.packet.RealmMotionPacket;
import io.zershyan.avernus.registry.packet.RealmRemovePacket;
import io.zershyan.avernus.registry.packet.RealmSnapshotPacket;
import io.zershyan.avernus.registry.packet.RealmUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class AvernusPackets {
    /** 使用完整模组版本协商网络兼容性；版本不一致时拒绝连接。 */
    private static final String PROTOCOL_VERSION = ModList.get()
            .getModContainerById(Avernus.MODID)
            .map(container -> container.getModInfo().getVersion().toString())
            .orElse("unknown");
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Avernus.MODID, "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static int nextId;

    private AvernusPackets() {
    }

    public static int nextId() {
        return nextId++;
    }

    public static void doRegister() {
        INSTANCE.messageBuilder(RealmUpdatePacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(RealmUpdatePacket::new).encoder(RealmUpdatePacket::encode).consumerMainThread(RealmUpdatePacket::handle).add();
        INSTANCE.messageBuilder(RealmRemovePacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(RealmRemovePacket::new).encoder(RealmRemovePacket::encode).consumerMainThread(RealmRemovePacket::handle).add();
        INSTANCE.messageBuilder(RealmMotionPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(RealmMotionPacket::new).encoder(RealmMotionPacket::encode).consumerMainThread(RealmMotionPacket::handle).add();
        INSTANCE.messageBuilder(RealmSnapshotPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(RealmSnapshotPacket::new).encoder(RealmSnapshotPacket::encode).consumerMainThread(RealmSnapshotPacket::handle).add();
        AvernusCompatFactory.registerNetwork(INSTANCE);
    }
}
