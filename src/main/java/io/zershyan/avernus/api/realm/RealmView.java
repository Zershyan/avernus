package io.zershyan.avernus.api.realm;

import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.UUID;

/** 领域的只读视图；坐标均为方块坐标。 */
public interface RealmView {
    UUID id();

    UUID ownerId();

    ResourceKey<Level> dimension();

    RealmAspect aspect();

    Set<RealmTrait> traits();

    default boolean hasTrait(RealmTrait trait) {
        return traits().contains(trait);
    }

    /** 开境时使用者脚部所在方块高度；该高度及以上的方块被消隐。 */
    int height();

    int radius();

    int centerX();

    int centerZ();

    /** 开启先后序号，越大越优先。 */
    long sequence();

    RealmPhase phase(long gameTime);

    /** 指定列（方块中心）当前是否已被波纹覆盖。 */
    boolean covers(int x, int z, long gameTime);
}
