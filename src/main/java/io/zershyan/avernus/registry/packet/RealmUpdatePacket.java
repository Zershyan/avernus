package io.zershyan.avernus.registry.packet;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 玩家开始观察一个领域时收到一次完整状态。 */
public record RealmUpdatePacket(long epoch, long order, Realm.Snapshot snapshot) implements CustomPacketPayload {
    public static final Type<RealmUpdatePacket> TYPE = new Type<>(Avernus.id("realm_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RealmUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, RealmUpdatePacket::epoch,
            ByteBufCodecs.VAR_LONG, RealmUpdatePacket::order,
            Realm.Snapshot.STREAM_CODEC, RealmUpdatePacket::snapshot,
            RealmUpdatePacket::new);

    @Override
    public Type<RealmUpdatePacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        Level level = context.player().level();
        if (!level.dimension().equals(snapshot.dimension())) return;
        var set = RealmSets.of(level);
        if (!set.acceptUpdate(epoch, order)) return;
        Realm realm = snapshot.instantiate();
        if (realm != null) set.put(realm);
    }
}
