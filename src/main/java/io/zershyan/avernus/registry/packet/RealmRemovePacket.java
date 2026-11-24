package io.zershyan.avernus.registry.packet;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.handling.IPayloadContext;

import java.util.UUID;

public record RealmRemovePacket(ResourceKey<Level> dimension, long epoch, long order, UUID id) implements CustomPacketPayload {
    public static final Type<RealmRemovePacket> TYPE = new Type<>(Avernus.id("realm_remove"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RealmRemovePacket> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), RealmRemovePacket::dimension,
            ByteBufCodecs.VAR_LONG, RealmRemovePacket::epoch,
            ByteBufCodecs.VAR_LONG, RealmRemovePacket::order,
            UUIDUtil.STREAM_CODEC, RealmRemovePacket::id,
            RealmRemovePacket::new);

    @Override
    public Type<RealmRemovePacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        Level level = context.player().level();
        if (!level.dimension().equals(dimension)) return;
        var set = RealmSets.of(level);
        if (set.acceptUpdate(epoch, order)) set.remove(id);
    }
}
