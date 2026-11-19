package io.zershyan.avernus.realm;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** 只重算经过覆盖变化位置的活动路径。传送前后分别查询，不扫描两点之间的世界。 */
final class RealmNavigation {
    private static final int PATH_LOOKAHEAD = 16;
    private static final int CHECK_INTERVAL = 10;
    private static final Map<UUID, Realm.Coverage> LAST = new HashMap<>();

    private RealmNavigation() {
    }

    static void update(ServerLevel level, Realm realm, long time, boolean immediate) {
        if (realm.isolated() || !immediate && time % CHECK_INTERVAL != 0L) return;
        Realm.Coverage old = LAST.get(realm.id());
        Realm.Coverage coverage = realm.coverage(time);
        if (coverage.wave() <= 0.0) LAST.remove(realm.id());
        else LAST.put(realm.id(), coverage);
        refresh(level, old, coverage.wave() > 0.0 ? coverage : null);
    }

    static void remove(ServerLevel level, Realm realm) {
        refresh(level, LAST.remove(realm.id()), null);
    }

    static void clear() {
        LAST.clear();
    }

    private static void refresh(ServerLevel level, @Nullable Realm.Coverage before, @Nullable Realm.Coverage after) {
        if (Objects.equals(before, after)) return;
        Set<Mob> checked = new HashSet<>();
        if (before != null) recheck(level, before, before, after, checked);
        if (after != null) recheck(level, after, before, after, checked);
    }

    private static void recheck(ServerLevel level, Realm.Coverage search, @Nullable Realm.Coverage before,
                                @Nullable Realm.Coverage after, Set<Mob> checked) {
        double padding = PATH_LOOKAHEAD;
        double x0 = Math.max(search.centerX() - search.radius(), search.sourceX() - search.wave()) - padding;
        double x1 = Math.min(search.centerX() + search.radius(), search.sourceX() + search.wave()) + padding;
        double z0 = Math.max(search.centerZ() - search.radius(), search.sourceZ() - search.wave()) - padding;
        double z1 = Math.min(search.centerZ() + search.radius(), search.sourceZ() + search.wave()) + padding;
        if (x0 > x1 || z0 > z1) return;
        var box = new AABB(x0, level.getMinBuildHeight(), z0, x1, level.getMaxBuildHeight(), z1);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) {
            if (!checked.add(mob)) continue;
            var navigation = mob.getNavigation();
            var path = navigation.getPath();
            if (path == null || path.isDone()) continue;
            if (changed(before, after, mob.getBlockX(), mob.getBlockZ())) {
                navigation.recomputePath();
                continue;
            }
            for (int i = path.getNextNodeIndex(); i < path.getNodeCount(); i++) {
                var step = path.getNode(i);
                if (changed(before, after, step.x, step.z)) {
                    navigation.recomputePath();
                    break;
                }
            }
        }
    }

    private static boolean changed(@Nullable Realm.Coverage before, @Nullable Realm.Coverage after, int x, int z) {
        return (before != null && before.covers(x, z)) != (after != null && after.covers(x, z));
    }
}
