package io.zershyan.avernus.handler.common;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.realm.RealmProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

/** 领域覆盖区的全来源方块保护：玩家、实体、爆炸、放置替换与活塞。 */
@EventBusSubscriber(modid = Avernus.MODID)
public final class RealmProtectionHandler {
    private RealmProtectionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && RealmProtection.isProtected(level, event.getPos())) event.setCanceled(true);
    }

    /** 客户端同样可判定，挖掘进度直接为零，避免播放破坏动画。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        event.getPosition().ifPresent(pos -> {
            if (RealmProtection.isProtected(event.getEntity().level(), pos)) event.setCanceled(true);
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDestroy(LivingDestroyBlockEvent event) {
        if (RealmProtection.isProtected(event.getEntity().level(), event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDetonate(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> RealmProtection.isProtected(event.getLevel(), pos));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level
                && RealmProtection.protectsReplacement(level, event.getPos(), event.getBlockSnapshot().getState())) {
            event.setCanceled(true);
        }
    }

    /** 活塞推动或挤碎任一受保护方块，或把方块推入受保护列，都整体取消。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPiston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof Level level)) return;
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
