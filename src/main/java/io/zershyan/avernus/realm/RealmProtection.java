package io.zershyan.avernus.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** 全来源方块保护：波纹已覆盖的列内真实方块不得被破坏，隔绝不取消保护。 */
public final class RealmProtection {
    private RealmProtection() {
    }

    public static boolean isProtected(@Nullable Level level, BlockPos pos) {
        if (level == null) return false;
        RealmSet set = RealmSets.of(level);
        return !set.isEmpty() && set.anyCovers(pos.getX(), pos.getZ(), level.getGameTime());
    }

    public static boolean isProtected(BlockGetter getter, BlockPos pos) {
        return isProtected(RealmPhysics.levelOf(getter), pos);
    }

    /** 点击的方块或将放置的位置落在领域覆盖列内时，对方块使用物品被禁止。 */
    public static boolean forbidsUse(UseOnContext context) {
        BlockPos clicked = context.getClickedPos();
        return isProtected(context.getLevel(), clicked) || isProtected(context.getLevel(), clicked.relative(context.getClickedFace()));
    }

    /** 同类方块的属性更新不是破坏，空气与纯流体也允许替换。 */
    public static boolean blocksMutation(Level level, BlockPos pos, BlockState before, BlockState after) {
        return before.getBlock() != after.getBlock() && protectsReplacement(level, pos, before);
    }

    /** 放置会替换掉的原方块是否受保护：空气与纯流体不算原方块。 */
    public static boolean protectsReplacement(Level level, BlockPos pos, BlockState replaced) {
        return !replaced.isAir() && !RealmTerrain.isPureFluid(replaced) && isProtected(level, pos);
    }
}
