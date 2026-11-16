package io.zershyan.avernus.realm;

import io.zershyan.avernus.registry.AvernusRealmTraits;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * 有效地形规则：只描述某领域覆盖列中某高度应呈现的状态，不读写世界。
 * 物理代理统一为石头（完整立方体碰撞），虚拟流体统一为水源。
 */
public final class RealmTerrain {
    public static final BlockState SOLID = Blocks.STONE.defaultBlockState();
    public static final BlockState WATER = Blocks.WATER.defaultBlockState();
    public static final BlockState AIR = Blocks.AIR.defaultBlockState();
    public static final FluidState WATER_FLUID = Fluids.WATER.getSource(false);

    private RealmTerrain() {
    }

    public enum Kind {
        /** 空气，无碰撞无流体。 */
        EMPTY,
        /** 完整立方体。 */
        SOLID,
        /** 一格虚拟水源。 */
        WATER,
        /** 仅物理存在的保底支撑，不显示。 */
        HIDDEN_SOLID
    }

    /** 统一规则：原状态在领域覆盖列的 y 高度应表现为何种类型。 */
    public static Kind classify(Realm realm, BlockState real, int y) {
        return classify(realm.height(), realm.hasTrait(AvernusRealmTraits.WEAK_WATER.get()),
                realm.hasTrait(AvernusRealmTraits.BOUNDLESS.get()), real, y);
    }

    /** 不依赖可变领域对象的同一规则，供区块网格线程使用快照参数。 */
    public static Kind classify(int height, boolean weakWater, boolean boundless, BlockState real, int y) {
        if (y >= height) return Kind.EMPTY;
        if (y == height - 1) {
            if (weakWater) return Kind.WATER;
            if (boundless) return Kind.SOLID;
            if (real.isAir()) return Kind.EMPTY;
            if (isPureFluid(real)) return Kind.WATER;
            return Kind.SOLID;
        }
        if (real.isAir()) return Kind.HIDDEN_SOLID;
        return Kind.SOLID;
    }

    /** 纯流体：流体占据且方块本身无其他形态（水、熔岩等液体方块）。 */
    public static boolean isPureFluid(BlockState state) {
        return state.getBlock() instanceof LiquidBlock;
    }

    /** 物理视图使用的方块状态。 */
    public static BlockState physicsState(Kind kind) {
        return switch (kind) {
            case EMPTY -> AIR;
            case SOLID, HIDDEN_SOLID -> SOLID;
            case WATER -> WATER;
        };
    }

    /** 显示视图使用的方块状态；保底支撑不绘制。 */
    public static BlockState displayState(Kind kind) {
        return switch (kind) {
            case EMPTY, HIDDEN_SOLID -> AIR;
            case SOLID -> SOLID;
            case WATER -> WATER;
        };
    }
}
