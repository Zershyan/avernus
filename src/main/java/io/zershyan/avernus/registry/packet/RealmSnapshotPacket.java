package io.zershyan.avernus.registry.packet;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmSet;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** 服务端 → 客户端：登录或切换维度时下发该维度的全部领域，按当前阶段重建而不重播开场。 */
public record RealmSnapshotPacket(ResourceKey<Level> dimension, List<Realm> realms) implements CustomPacketPayload {
    public static final Type<RealmSnapshotPacket> TYPE = new Type<>(Avernus.id("realm_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RealmSnapshotPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), RealmSnapshotPacket::dimension,
            Realm.STREAM_CODEC.apply(ByteBufCodecs.list(1024)), RealmSnapshotPacket::realms,
            RealmSnapshotPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if (!level.dimension().equals(dimension)) return;
            RealmSet set = RealmSets.of(level);
            set.clear();
            realms.forEach(set::put);
        });
    }
}
