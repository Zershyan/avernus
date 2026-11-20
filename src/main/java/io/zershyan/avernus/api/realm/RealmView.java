package io.zershyan.avernus.api.realm;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/** 领域的只读视图；坐标均为方块坐标，境相和类型只暴露稳定资源 ID。 */
public interface RealmView {
    UUID id();

    UUID ownerId();

    ResourceKey<Level> dimension();

    ResourceLocation aspectId();

    /** 去重排序的不可修改类型 ID 列表。 */
    List<ResourceLocation> traitIds();

    default boolean hasTrait(ResourceLocation trait) {
        return traitIds().contains(trait);
    }

    /** 开境时使用者脚部所在的方块高度。 */
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
