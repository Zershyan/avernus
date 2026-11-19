package io.zershyan.avernus.registry.packet;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.compat.AvernusCompatFactory;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.registry.item.MirrorItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 客户端 → 服务端：开关胸饰镜领域。不携带任何可信数据，服务端自行查找实际佩戴的镜。 */
public record ToggleWornMirrorPacket() implements CustomPacketPayload {
    public static final Type<ToggleWornMirrorPacket> TYPE = new Type<>(Avernus.id("toggle_worn_mirror"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleWornMirrorPacket> STREAM_CODEC = StreamCodec.unit(new ToggleWornMirrorPacket());
    /** 同一玩家两次请求的最小间隔，过滤连按与滥发。 */
    private static final int MIN_INTERVAL = 10;
    private static final Map<UUID, Long> LAST_REQUEST = new ConcurrentHashMap<>();

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return;
            long now = player.serverLevel().getGameTime();
            Long last = LAST_REQUEST.put(player.getUUID(), now);
            if (last != null && now - last < MIN_INTERVAL && now >= last) return;
            ItemStack mirror = AvernusCompatFactory.CURIOS.findWornMirror(player);
            if (!mirror.isEmpty()) RealmManager.toggle(player, MirrorItem.getForm(mirror));
        });
    }

    public static void forget(UUID player) {
        LAST_REQUEST.remove(player);
    }
}
