package io.zershyan.avernus.realm;

import io.zershyan.avernus.registry.AvernusAttachments;
import io.zershyan.avernus.registry.attachment.RealmPresence;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * 服务端逐实体跟踪生效领域：变化时统一重新解析——按需授予一次首次落地保护，并在嵌入地形时沿当前 X/Z 向上脱困。
 * 波纹扫过/扫离、关闭、离境、重叠胜出者变化与永恒移走都表现为生效领域变化，走同一路径。
 */
public final class RealmPresenceTracker {
    private static final double SUPPORT_PROBE = 0.05;

    private RealmPresenceTracker() {
    }

    public static void tick(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        RealmPresence presence = entity.getData(AvernusAttachments.REALM_PRESENCE);
        Realm realm = RealmPhysics.realmAt(level, entity, entity.blockPosition());
        UUID current = realm == null ? null : realm.id();
        boolean pendingRecovery = entity.getData(AvernusAttachments.PENDING_RECOVERY);

        if (!Objects.equals(current, presence.realm())) {
            presence = new RealmPresence(current, presence.fallGuard() || !supported(level, entity));
            escapeIfEmbedded(level, entity);
        } else if (current == null && pendingRecovery) {
            escapeIfEmbedded(level, entity);
        }
        if (presence.fallGuard() && (entity.onGround() || entity.isInWater())) presence = presence.withoutGuard();

        setPresence(entity, presence);
        if (pendingRecovery != (current != null)) entity.setData(AvernusAttachments.PENDING_RECOVERY, current != null);
    }

    /** 首次落地：先阻止本次摔伤，再无条件消耗保护。返回是否拦截。 */
    public static boolean consumeFallGuard(LivingEntity entity) {
        RealmPresence presence = entity.getExistingDataOrNull(AvernusAttachments.REALM_PRESENCE);
        if (presence == null || !presence.fallGuard()) return false;
        setPresence(entity, presence.withoutGuard());
        return true;
    }

    private static void setPresence(LivingEntity entity, RealmPresence presence) {
        if (presence.equals(RealmPresence.NONE)) {
            if (entity.hasData(AvernusAttachments.REALM_PRESENCE)) entity.removeData(AvernusAttachments.REALM_PRESENCE);
        } else {
            entity.setData(AvernusAttachments.REALM_PRESENCE, presence);
        }
    }

    private static boolean supported(ServerLevel level, LivingEntity entity) {
        return entity.isInWater() || !level.noCollision(entity, entity.getBoundingBox().move(0.0, -SUPPORT_PROBE, 0.0));
    }

    /** 嵌入当前有效地形时，保持 X/Z 向上寻找首个能容纳完整 AABB 的位置；找不到则回退到该列最高表面。 */
    public static void escapeIfEmbedded(ServerLevel level, LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        if (level.noCollision(entity, box)) return;
        Double target = findFreeY(level, entity, box);
        double y = target != null ? target
                : level.getHeight(Heightmap.Types.MOTION_BLOCKING, entity.getBlockX(), entity.getBlockZ());
        entity.resetFallDistance();
        entity.teleportTo(entity.getX(), y, entity.getZ());
    }

    private static @Nullable Double findFreeY(ServerLevel level, LivingEntity entity, AABB box) {
        int from = entity.getBlockY();
        for (int y = from + 1; y < level.getMaxBuildHeight(); y++) {
            if (level.noCollision(entity, box.move(0.0, y - entity.getY(), 0.0))) return (double) y;
        }
        return null;
    }
}
