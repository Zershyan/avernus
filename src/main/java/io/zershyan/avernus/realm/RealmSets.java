package io.zershyan.avernus.realm;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按逻辑端与维度存放领域集合。单人游戏中服务端与客户端共处一个 JVM，因此以端区分；
 * 通用代码只经由 {@link Level} 取得集合，不链接任何端专属类。
 */
public final class RealmSets {
    private static final Map<ResourceKey<Level>, RealmSet> SERVER = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, RealmSet> CLIENT = new ConcurrentHashMap<>();

    private RealmSets() {
    }

    private static Map<ResourceKey<Level>, RealmSet> side(boolean client) {
        return client ? CLIENT : SERVER;
    }

    public static RealmSet of(Level level) {
        return side(level.isClientSide).computeIfAbsent(level.dimension(), key -> new RealmSet());
    }

    /** 非 Level 的查询视图（区块网格区域、寻路区域等）返回空，由调用方自行提供 Level。 */
    public static @Nullable RealmSet ofGetter(BlockGetter getter) {
        return getter instanceof Level level ? of(level) : null;
    }

    public static Map<ResourceKey<Level>, RealmSet> server() {
        return SERVER;
    }

    public static void clearClient() {
        CLIENT.values().forEach(RealmSet::clear);
        CLIENT.clear();
    }

    public static void clearServer() {
        SERVER.values().forEach(RealmSet::clear);
        SERVER.clear();
    }
}
