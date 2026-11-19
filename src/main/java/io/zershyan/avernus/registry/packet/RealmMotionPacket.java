package io.zershyan.avernus.registry.packet;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record RealmMotionPacket(ResourceKey<Level> dimension, long epoch, long order, UUID id,
                                Realm.Motion motion) implements CustomPacketPayload {
    public static final Type<RealmMotionPacket> TYPE = new Type<>(Avernus.id("realm_motion"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RealmMotionPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), RealmMotionPacket::dimension,
            ByteBufCodecs.VAR_LONG, RealmMotionPacket::epoch,
            ByteBufCodecs.VAR_LONG, RealmMotionPacket::order,
            UUIDUtil.STREAM_CODEC, RealmMotionPacket::id,
            Realm.Motion.STREAM_CODEC, RealmMotionPacket::motion,
            RealmMotionPacket::new);

    @Override
    public Type<RealmMotionPacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        Level level = context.player().level();
        if (!level.dimension().equals(dimension)) return;
        var set = RealmSets.of(level);
        if (!set.acceptUpdate(epoch, order)) return;
        Realm realm = set.get(id);
        if (realm != null) realm.applyMotion(motion);
    }
}
