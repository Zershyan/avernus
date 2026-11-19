package io.zershyan.avernus.realm;

import io.zershyan.avernus.registry.AvernusAttachments;
import io.zershyan.avernus.registry.attachment.RealmPresence;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class RealmPresenceTracker {
    private static final double SUPPORT_PROBE = 0.05;
    private static final double EPSILON = 1.0E-7;

    private RealmPresenceTracker() {
    }

    public static void tick(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || entity.noPhysics || entity.isRemoved()) return;
        RealmPresence old = presence(entity);
        boolean pending = Boolean.TRUE.equals(entity.getExistingDataOrNull(AvernusAttachments.PENDING_RECOVERY));
        RealmSet realms = RealmSets.of(level);
        if (realms.isEmpty() && old == RealmPresence.NONE && !pending) return;

        Footprint geometry = footprint(level, realms, entity);
        boolean affected = geometry.signature() != 0L;
        UUID realm = geometry.realmId();
        boolean guard = old.fallGuard();
        boolean entered = entity instanceof LivingEntity && realm != null && !realm.equals(old.realm());
        if (geometry.signature() != old.checkedGeometry() || pending && !affected) escapeIfEmbedded(level, entity);
        if (entity.isInWater()) guard = false;
        else if (entered || guard && entity.onGround()) {
            boolean supported = !level.noBlockCollision(entity, entity.getBoundingBox().move(0.0, -SUPPORT_PROBE, 0.0));
            if (supported && entity.onGround()) guard = false;
            else if (entered && !supported) guard = true;
        }
        store(entity, old, new RealmPresence(realm, guard, geometry.signature()));
        if (affected != pending) {
            if (affected) entity.setData(AvernusAttachments.PENDING_RECOVERY, true);
            else entity.removeData(AvernusAttachments.PENDING_RECOVERY);
        }
    }

    /** 包含服务端玩家移动包：它最终调用 Entity 的实现，而不是 ServerPlayer 的空覆写。 */
    public static void beforeFallCheck(LivingEntity entity, boolean onGround, double dy, BlockPos support) {
        if (!(entity.level() instanceof ServerLevel level) || entity.noPhysics || (!onGround && dy >= 0.0)) return;
        RealmPresence old = presence(entity);
        RealmSet realms = RealmSets.of(level);
        if (realms.isEmpty() && old == RealmPresence.NONE) return;
        UUID id = footprint(level, realms, entity).realmId();
        if (id == null && onGround) {
            Realm realm = RealmPhysics.realmAt(level, entity, support);
            if (realm != null) id = realm.id();
        }
        if (id != null && !id.equals(old.realm())) {
            boolean guard = old.fallGuard() || entity.fallDistance > 0.0F || dy < 0.0;
            store(entity, old, new RealmPresence(id, guard, old.checkedGeometry()));
        }
    }

    public static boolean consumeFallGuard(LivingEntity entity) {
        RealmPresence old = presence(entity);
        if (!old.fallGuard()) return false;
        store(entity, old, new RealmPresence(old.realm(), false, old.checkedGeometry()));
        return true;
    }

    private static RealmPresence presence(Entity entity) {
        RealmPresence value = entity.getExistingDataOrNull(AvernusAttachments.REALM_PRESENCE);
        return value != null ? value : RealmPresence.NONE;
    }

    private static void store(Entity entity, RealmPresence old, RealmPresence next) {
        if (next.equals(old)) return;
        if (next.equals(RealmPresence.NONE)) entity.removeData(AvernusAttachments.REALM_PRESENCE);
        else entity.setData(AvernusAttachments.REALM_PRESENCE, next);
    }

    private record Footprint(@Nullable Realm realm, long signature) {
        static final Footprint NONE = new Footprint(null, 0L);

        @Nullable UUID realmId() {
            return realm == null ? null : realm.id();
        }
    }

    private static Footprint footprint(ServerLevel level, RealmSet realms, Entity entity) {
        if (realms.isEmpty()) return Footprint.NONE;
        AABB box = entity.getBoundingBox();
        long time = level.getGameTime();
        int cx = entity.getBlockX(), cz = entity.getBlockZ();
        Realm center = realms.geometryAt(entity, cx, cz, time);
        Realm primary = center;
        Realm first = null;
        long signature = 1L;
        boolean mixed = false;
        int count = 0;
        for (int x = Mth.floor(box.minX + EPSILON); x <= Mth.floor(box.maxX - EPSILON); x++) {
            for (int z = Mth.floor(box.minZ + EPSILON); z <= Mth.floor(box.maxZ - EPSILON); z++) {
                Realm realm = x == cx && z == cz ? center : realms.geometryAt(entity, x, z, time);
                if (count++ == 0) first = realm;
                else mixed |= first != realm;
                if (primary == null && realm != null) primary = realm;
                signature = 31L * signature + (realm == null ? 0L : realm.sequence());
            }
        }
        if (primary == null) return Footprint.NONE;
        return new Footprint(primary, mixed ? signature | Long.MIN_VALUE : primary.sequence());
    }

    /** 保持 X/Z，只为实际嵌入方块的实体恢复位置。 */
    public static void escapeIfEmbedded(ServerLevel level, Entity entity) {
        AABB box = entity.getBoundingBox().deflate(EPSILON);
        if (entity.isPassenger() || level.noBlockCollision(entity, box)) return;
        double start = entity.getY();
        boolean aquatic = SpawnPlacements.getPlacementType(entity.getType()) == SpawnPlacementTypes.IN_WATER;
        for (int y = Mth.ceil(start); y <= level.getMaxBuildHeight(); y++) {
            AABB candidate = box.move(0.0, y - start, 0.0);
            if (!level.noBlockCollision(entity, candidate)) continue;
            if (aquatic && waterAt(level, entity, BlockPos.containing(entity.getX(), y, entity.getZ()))) {
                restore(entity, y);
                return;
            }
            double down = Shapes.collide(Direction.Axis.Y, candidate,
                    level.getBlockCollisions(entity, candidate.expandTowards(0.0, -1.0, 0.0)), -1.0);
            if (down > -1.0 + EPSILON && y + down >= start) {
                restore(entity, y + down);
                return;
            }
        }
        int top = level.getMinBuildHeight();
        for (int x = Mth.floor(box.minX); x <= Mth.floor(box.maxX); x++) {
            for (int z = Mth.floor(box.minZ); z <= Mth.floor(box.maxZ); z++) {
                top = Math.max(top, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z));
            }
        }
        double y = Math.max(start, top);
        if (!level.noBlockCollision(entity, box.move(0.0, y - start, 0.0))) y = Math.max(y, level.getMaxBuildHeight());
        restore(entity, y);
    }

    private static boolean waterAt(ServerLevel level, Entity entity, BlockPos pos) {
        return RealmPhysics.fluid(level, entity, pos, level.getFluidState(pos)).is(FluidTags.WATER);
    }

    private static void restore(Entity entity, double y) {
        entity.resetFallDistance();
        entity.teleportTo(entity.getX(), y, entity.getZ());
    }
}
