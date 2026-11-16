package io.zershyan.avernus.registry.packet;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** 服务端 → 客户端：新增或更新一个领域（含关闭时刻与永恒中心）。 */
public record RealmUpdatePacket(Realm realm) implements CustomPacketPayload {
    public static final Type<RealmUpdatePacket> TYPE = new Type<>(Avernus.id("realm_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RealmUpdatePacket> STREAM_CODEC =
            Realm.STREAM_CODEC.map(RealmUpdatePacket::new, RealmUpdatePacket::realm);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if (level.dimension().equals(realm.dimension())) RealmSets.of(level).put(realm);
        });
    }
}
