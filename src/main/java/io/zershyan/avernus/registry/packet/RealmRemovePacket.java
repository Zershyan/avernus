package io.zershyan.avernus.registry.packet;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** 服务端 → 客户端：领域已移除；emergency 为真时客户端立即撤销虚拟地形。 */
public record RealmRemovePacket(UUID id, boolean emergency) implements CustomPacketPayload {
    public static final Type<RealmRemovePacket> TYPE = new Type<>(Avernus.id("realm_remove"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RealmRemovePacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RealmRemovePacket::id,
            ByteBufCodecs.BOOL, RealmRemovePacket::emergency,
            RealmRemovePacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> RealmSets.of(context.player().level()).remove(id));
    }
}
