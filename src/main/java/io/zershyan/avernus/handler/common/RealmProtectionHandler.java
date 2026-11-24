package io.zershyan.avernus.handler.common;

import io.zershyan.avernus.realm.RealmProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.level.BlockDropsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;

import java.util.List;

/** 领域覆盖区的全来源方块保护兜底：玩家的挖掘与放置已在 MixinPlayer/MixinItemStack 中如冒险模式般禁止。 */
public final class RealmProtectionHandler {
    private RealmProtectionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && RealmProtection.isProtected(level, event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDestroy(LivingDestroyBlockEvent event) {
        if (RealmProtection.isProtected(event.getEntity().level(), event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockDrops(BlockDropsEvent event) {
        var level = event.getLevel();
        if (RealmProtection.isProtected(level, event.getPos())
                && level.getBlockState(event.getPos()).is(event.getState().getBlock())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDetonate(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> RealmProtection.isProtected(event.getLevel(), pos));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) return;
        var snapshots = event instanceof BlockEvent.EntityMultiPlaceEvent multi
                ? multi.getReplacedBlockSnapshots() : List.of(event.getBlockSnapshot());
        for (var snapshot : snapshots) {
            if (RealmProtection.blocksMutation(level, snapshot.getPos(), snapshot.getState(), snapshot.getCurrentState())) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /** 活塞推动或挤碎任一受保护方块，或把方块推入受保护列，都整体取消。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPiston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof Level level)) return;
        if (RealmProtection.isProtected(level, event.getPos()) || RealmProtection.isProtected(level, event.getFaceOffsetPos())) {
            event.setCanceled(true);
            return;
        }
        PistonStructureResolver resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) return;
        var push = event.getPistonMoveType().isExtend ? event.getDirection() : event.getDirection().getOpposite();
        for (BlockPos pos : resolver.getToPush()) {
            if (RealmProtection.isProtected(level, pos) || RealmProtection.isProtected(level, pos.relative(push))) {
                event.setCanceled(true);
                return;
            }
        }
        for (BlockPos pos : resolver.getToDestroy()) {
            if (RealmProtection.isProtected(level, pos)) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
