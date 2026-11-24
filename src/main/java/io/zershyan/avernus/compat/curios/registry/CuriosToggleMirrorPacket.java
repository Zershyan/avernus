package io.zershyan.avernus.compat.curios.registry;

import io.zershyan.avernus.api.AvernusApi;
import io.zershyan.avernus.compat.AvernusCompatFactory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 客户端 → 服务端：开关胸饰镜领域；仅在安装 Curios 时作为可选包注册。 */
public record CuriosToggleMirrorPacket() implements CustomPacketPayload {
    public static final Type<CuriosToggleMirrorPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AvernusApi.MODID, "toggle_worn_mirror"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CuriosToggleMirrorPacket> STREAM_CODEC = StreamCodec.unit(new CuriosToggleMirrorPacket());
    /** 同一玩家两次请求的最小间隔，过滤连按与滥发。 */
    private static final int MIN_INTERVAL = 10;
    private static final Map<UUID, Long> LAST_REQUEST = new ConcurrentHashMap<>();

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void doRegister(PayloadRegistrar registrar) {
        registrar.optional().playToServer(TYPE, STREAM_CODEC, CuriosToggleMirrorPacket::handle);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return;
            long now = player.serverLevel().getGameTime();
            Long last = LAST_REQUEST.put(player.getUUID(), now);
            if (last != null && now - last < MIN_INTERVAL && now >= last) return;
            ItemStack mirror = AvernusCompatFactory.CURIOS.findWornMirror(player);
            AvernusApi.formOf(mirror).ifPresent(form -> AvernusApi.toggle(player, form));
        });
    }

    public static void forget(UUID player) {
        LAST_REQUEST.remove(player);
    }
}
