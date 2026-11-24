package io.zershyan.avernus.compat.curios.registry;

import io.zershyan.avernus.api.AvernusApi;
import io.zershyan.avernus.compat.AvernusCompatFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** 客户端 → 服务端：开关胸饰镜领域；仅在安装 Curios 时注册。 */
public final class CuriosToggleMirrorPacket {
    /** 兼容消息占用高位编号，避免与本体消息冲突。 */
    public static final int ID = 100;
    /** 同一玩家两次请求的最小间隔，过滤连按与滥发。 */
    private static final int MIN_INTERVAL = 10;
    private static final Map<UUID, Long> LAST_REQUEST = new ConcurrentHashMap<>();

    public CuriosToggleMirrorPacket() {
    }

    public CuriosToggleMirrorPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static void doRegister(SimpleChannel channel) {
        channel.messageBuilder(CuriosToggleMirrorPacket.class, ID, NetworkDirection.PLAY_TO_SERVER)
                .decoder(CuriosToggleMirrorPacket::new).encoder(CuriosToggleMirrorPacket::encode).consumerMainThread(CuriosToggleMirrorPacket::handle).add();
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        ServerPlayer player = supplier.get().getSender();
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        long now = player.serverLevel().getGameTime();
        Long last = LAST_REQUEST.put(player.getUUID(), now);
        if (last != null && now - last < MIN_INTERVAL && now >= last) return;
        ItemStack mirror = AvernusCompatFactory.CURIOS.findWornMirror(player);
        AvernusApi.formOf(mirror).ifPresent(form -> AvernusApi.toggle(player, form));
    }

    public static void forget(UUID player) {
        LAST_REQUEST.remove(player);
    }
}
