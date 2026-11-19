package io.zershyan.avernus.registry.packet;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 开始新观察流；完整状态由随后的单领域包逐一补齐。 */
public record RealmSnapshotPacket(ResourceKey<Level> dimension, long epoch) implements CustomPacketPayload {
    public static final Type<RealmSnapshotPacket> TYPE = new Type<>(Avernus.id("realm_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RealmSnapshotPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), RealmSnapshotPacket::dimension,
            ByteBufCodecs.VAR_LONG, RealmSnapshotPacket::epoch,
            RealmSnapshotPacket::new);

    @Override
    public Type<RealmSnapshotPacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        Level level = context.player().level();
        if (level.dimension().equals(dimension)) RealmSets.of(level).beginUpdates(epoch);
    }
}
