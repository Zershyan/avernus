package io.zershyan.avernus.realm;

import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 单个维度内的领域集合，按开启序号从新到旧排列。查询只读当前快照，写入复制后替换，
 * 因此区块网格线程可无锁读取。
 */
public final class RealmSet {
    private volatile List<Realm> realms = List.of();

    public List<Realm> all() {
        return realms;
    }

    public boolean isEmpty() {
        return realms.isEmpty();
    }

    public synchronized void put(Realm realm) {
        List<Realm> next = new ArrayList<>(realms);
        next.removeIf(existing -> existing.id().equals(realm.id()));
        next.add(realm);
        next.sort(Comparator.comparingLong(Realm::sequence).reversed());
        realms = List.copyOf(next);
    }

    public synchronized @Nullable Realm remove(UUID id) {
        List<Realm> next = new ArrayList<>(realms);
        Realm removed = null;
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).id().equals(id)) removed = next.remove(i--);
        }
        realms = List.copyOf(next);
        return removed;
    }

    public synchronized void clear() {
        realms = List.of();
    }

    public @Nullable Realm get(UUID id) {
        for (Realm realm : realms) if (realm.id().equals(id)) return realm;
        return null;
    }

    public @Nullable Realm ownedBy(UUID owner) {
        for (Realm realm : realms) if (realm.ownerId().equals(owner)) return realm;
        return null;
    }

    /** 该列对指定主体生效的几何领域：已被波纹覆盖、几何适用且序号最新者。 */
    public @Nullable Realm geometryAt(@Nullable Entity subject, int x, int z, long gameTime) {
        for (Realm realm : realms) {
            if (realm.geometryAppliesTo(subject) && realm.covers(x, z, gameTime)) return realm;
        }
        return null;
    }

    /** 该列的效果领域：不区分主体，取已覆盖的最新领域。 */
    public @Nullable Realm effectAt(int x, int z, long gameTime) {
        for (Realm realm : realms) {
            if (realm.covers(x, z, gameTime)) return realm;
        }
        return null;
    }

    /** 该列是否被任一领域覆盖（方块保护使用并集）。 */
    public boolean anyCovers(int x, int z, long gameTime) {
        return effectAt(x, z, gameTime) != null;
    }
}
