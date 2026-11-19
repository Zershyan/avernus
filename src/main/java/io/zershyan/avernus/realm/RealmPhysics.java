package io.zershyan.avernus.realm;

import io.zershyan.avernus.util.mixin.avernus.common.IMixinPathNavigationRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import org.jetbrains.annotations.Nullable;

/**
 * 有效世界查询：只在 Mixin 指定的调用路径上替换读到的方块/流体，绝不改写真实世界。
 * 碰撞视图让 H−2 及以下保底为实心；内部视图只消隐 H 及以上、规整 H−1 层，保留下方真实方块效果。
 */
public final class RealmPhysics {
    private static final FluidState EMPTY_FLUID = Fluids.EMPTY.defaultFluidState();

    private RealmPhysics() {
    }

    public static @Nullable Level levelOf(BlockGetter getter) {
        if (getter instanceof Level level) return level;
        if (getter instanceof IMixinPathNavigationRegion region) return region.avernus$level();
        return null;
    }

    public static @Nullable Entity subjectOf(CollisionContext context) {
        return context instanceof EntityCollisionContext entityContext ? entityContext.getEntity() : null;
    }

    /** 该位置对主体生效的领域；无领域时快速返回。 */
    public static @Nullable Realm realmAt(@Nullable Level level, @Nullable Entity subject, BlockPos pos) {
        if (level == null) return null;
        RealmSet set = RealmSets.of(level);
        if (set.isEmpty()) return null;
        return set.geometryAt(subject, pos.getX(), pos.getZ(), level.getGameTime());
    }

    private static RealmTerrain.Kind kind(Realm realm, BlockState real, BlockPos pos) {
        return RealmTerrain.classify(realm, real, pos.getY());
    }

    /** 碰撞、寻路与投射物入地使用的状态。 */
    public static BlockState collision(@Nullable Level level, @Nullable Entity subject, BlockPos pos, BlockState real) {
        Realm realm = realmAt(level, subject, pos);
        return realm == null ? real : RealmTerrain.physicsState(kind(realm, real, pos));
    }

    /** 实体内部效果（窒息、蛛网、方块速度、脚下方块、屏幕遮罩）使用的状态。 */
    public static BlockState interior(@Nullable Level level, @Nullable Entity subject, BlockPos pos, BlockState real) {
        Realm realm = realmAt(level, subject, pos);
        if (realm == null || pos.getY() < realm.height() - 1) return real;
        return RealmTerrain.physicsState(kind(realm, real, pos));
    }

    /** 仅在虚拟层或紧邻其下的流体表面接管高度；负数表示沿用原实现。 */
    public static float surfaceHeight(BlockGetter getter, @Nullable Entity subject, BlockPos pos, FluidState state) {
        Realm realm = realmAt(levelOf(getter), subject, pos);
        if (realm == null || state.isEmpty() || !(state.getType() instanceof FlowingFluid)) return -1.0F;
        int layer = realm.height() - 1;
        if (pos.getY() == layer) return state.getOwnHeight();
        if (pos.getY() != layer - 1) return -1.0F;
        BlockPos above = pos.above();
        return kind(realm, getter.getBlockState(above), above) == RealmTerrain.Kind.WATER && state.getType().isSame(Fluids.WATER)
                ? 1.0F : state.getOwnHeight();
    }
    /** 流体接触、浮力、眼部与相机使用的流体。 */
    public static FluidState fluid(@Nullable Level level, @Nullable Entity subject, BlockPos pos, FluidState real) {
        Realm realm = realmAt(level, subject, pos);
        if (realm == null || pos.getY() < realm.height() - 1) return real;
        if (pos.getY() >= realm.height()) return EMPTY_FLUID;
        return kind(realm, level.getBlockState(pos), pos) == RealmTerrain.Kind.WATER ? RealmTerrain.WATER_FLUID : EMPTY_FLUID;
    }
}
