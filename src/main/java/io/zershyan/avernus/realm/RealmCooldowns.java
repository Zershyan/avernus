package io.zershyan.avernus.realm;

import io.zershyan.avernus.registry.AvernusAttachments;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.attachment.RealmCooldown;
import io.zershyan.avernus.registry.config.ServerConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 使用者级冷却，统一以服务器主世界 gameTime 计时。 */
public final class RealmCooldowns {
    private RealmCooldowns() {
    }

    private static long now(ServerLevel level) {
        return level.getServer().overworld().getGameTime();
    }

    public static long remaining(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return 0L;
        RealmCooldown cooldown = AvernusAttachments.get(entity, AvernusAttachments.REALM_COOLDOWN);
        return cooldown == null ? 0L : cooldown.remaining(now(level));
    }

    public static void start(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        int ticks = ServerConfig.CLOSE_COOLDOWN_TICKS.get();
        AvernusAttachments.set(entity, AvernusAttachments.REALM_COOLDOWN, new RealmCooldown(now(level) + ticks));
        if (entity instanceof Player player) showCooldown(player, ticks);
    }

    /** 重新登录或重生后，用权威剩余时间恢复物品冷却条显示。 */
    public static void refreshDisplay(Player player) {
        long remaining = remaining(player);
        if (remaining > 0L) showCooldown(player, (int) remaining);
    }

    private static void showCooldown(Player player, int ticks) {
        AvernusItems.MIRRORS_BY_ASPECT.values().forEach(mirror -> player.getCooldowns().addCooldown(mirror.get(), ticks));
    }
}
