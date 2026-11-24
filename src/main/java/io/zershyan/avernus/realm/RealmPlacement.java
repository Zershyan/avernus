package io.zershyan.avernus.realm;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Set;
import java.util.function.Supplier;

/** 区块前置拒写后，撤回同一次物品使用已成功的其他写入。 */
public final class RealmPlacement {
    private static final ThreadLocal<RealmPlacement> CURRENT = new ThreadLocal<>();
    private final ServerLevel level;
    private boolean denied;

    private RealmPlacement(ServerLevel level) {
        this.level = level;
    }

    public static void deny(Level level) {
        var transaction = CURRENT.get();
        if (transaction != null && transaction.level == level) transaction.denied = true;
    }

    public static InteractionResult capture(UseOnContext context, Supplier<InteractionResult> use) {
        if (!(context.getLevel() instanceof ServerLevel level) || !level.captureBlockSnapshots || RealmSets.of(level).isEmpty()) return use.get();
        var parent = CURRENT.get();
        var transaction = new RealmPlacement(level);
        int start = level.capturedBlockSnapshots.size();
        ItemStack stack = context.getItemInHand();
        ItemStack before = stack.copy();
        CURRENT.set(transaction);
        try {
            InteractionResult result = use.get();
            return transaction.denied ? InteractionResult.FAIL : result;
        } finally {
            if (parent == null) CURRENT.remove(); else CURRENT.set(parent);
            if (transaction.denied) {
                transaction.rollback(start);
                stack.setCount(before.getCount());
                stack.setTag(before.hasTag() ? before.getTag().copy() : null);
                if (parent != null && parent.level == level) parent.denied = true;
            }
        }
    }

    private void rollback(int start) {
        boolean capturing = level.captureBlockSnapshots, restoring = level.restoringBlockSnapshots;
        level.captureBlockSnapshots = false;
        level.restoringBlockSnapshots = true;
        try {
            var snapshots = level.capturedBlockSnapshots;
            while (snapshots.size() > start) {
                var snapshot = snapshots.remove(snapshots.size() - 1);
                snapshot.restore();
            }
        } finally {
            level.restoringBlockSnapshots = restoring;
            level.captureBlockSnapshots = capturing;
        }
    }
}
