package io.zershyan.avernus.gametest;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmCooldowns;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.registry.AvernusAttachments;
import io.zershyan.avernus.registry.attachment.RealmCooldown;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.data_component.MirrorForm;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Objects;

@GameTestHolder(Avernus.MODID)
@PrefixGameTestTemplate(false)
public final class OriginalRequirementsScenarios {
    private static final BlockPos TARGET = new BlockPos(8, 2, 8);

    @GameTest(template = "empty", batch = "re_audit_tnt", timeoutTicks = 250)
    public static void protectedRedstoneTntDoesNotCreateEntity(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.setBlock(TARGET, Blocks.TNT);
        var realm = open(helper, owner(helper));
        BlockPos target = helper.absolutePos(TARGET);
        helper.startSequence().thenWaitUntil(() -> covered(helper, realm, target))
                .thenExecute(() -> {
                    try {
                        var player = helper.makeMockPlayer(GameType.SURVIVAL);
                        var charge = new ItemStack(Items.FIRE_CHARGE, 3);
                        var hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
                        var use = level.getBlockState(target).useItemOn(charge, level, player, InteractionHand.MAIN_HAND, hit);
                        helper.assertTrue(use == ItemInteractionResult.FAIL && charge.getCount() == 3, "受保护 TNT 不应消耗引燃物");
                        helper.setBlock(TARGET.east(), Blocks.REDSTONE_BLOCK);
                        helper.assertBlockPresent(Blocks.TNT, TARGET);
                        var primed = level.getEntitiesOfClass(PrimedTnt.class, new AABB(target).inflate(2));
                        int count = primed.size();
                        primed.forEach(PrimedTnt::discard);
                        helper.assertTrue(count == 0, "保护区 TNT 方块未移除却生成了引燃实体，数量=" + count);
                    } finally {
                        RealmManager.emergencyClose(level, realm);
                    }
                    helper.setBlock(TARGET.east(), Blocks.AIR);
                    helper.setBlock(TARGET.east(), Blocks.REDSTONE_BLOCK);
                    var outside = level.getEntitiesOfClass(PrimedTnt.class, new AABB(target).inflate(2));
                    helper.assertTrue(outside.size() == 1 && level.isEmptyBlock(target), "关境后红石应正常引燃 TNT");
                    outside.forEach(PrimedTnt::discard);
                }).thenSucceed();
    }

    @GameTest(template = "empty", batch = "re_audit_container", timeoutTicks = 250)
    public static void canceledPlacementLeavesNoContainerDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.setBlock(TARGET, Blocks.CHEST);
        BlockPos target = helper.absolutePos(TARGET);
        var original = (ChestBlockEntity) level.getBlockEntity(target);
        Objects.requireNonNull(original).setItem(0, new ItemStack(Items.DIAMOND, 12));
        var realm = open(helper, owner(helper));
        helper.startSequence().thenWaitUntil(() -> covered(helper, realm, target))
                .thenExecute(() -> {
                    try {
                        level.captureBlockSnapshots = true;
                        boolean changed = level.setBlock(target, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                        helper.assertFalse(changed, "捕获阶段就应拒绝容器替换");
                        helper.assertTrue(level.capturedBlockSnapshots.isEmpty(), "拒写不得留下可提交快照");
                        var restored = (ChestBlockEntity) level.getBlockEntity(target);
                        helper.assertTrue(restored != null && restored.getItem(0).getCount() == 12, "箱子库存应已回滚");
                        int extra = level.getEntitiesOfClass(ItemEntity.class, new AABB(target).inflate(2)).stream().filter(item -> item.getItem().is(Items.DIAMOND)).mapToInt(item -> {
                            int count = item.getItem().getCount();
                            item.discard();
                            return count;
                        }).sum();
                        helper.assertTrue(extra == 0, "取消放置后箱内恢复 12 颗，但另掉出 " + extra + " 颗钻石");
                    } finally {
                        level.captureBlockSnapshots = false;
                        level.restoringBlockSnapshots = false;
                        level.capturedBlockSnapshots.clear();
                        RealmManager.emergencyClose(level, realm);
                    }
                }).thenSucceed();
    }

    @GameTest(template = "empty", batch = "re_audit_water", timeoutTicks = 250)
    public static void hiddenRealWaterDoesNotRaiseVirtualSurface(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos feet = helper.absolutePos(TARGET.below());
        var realm = open(helper, owner(helper), AvernusRealmTraits.WEAK_WATER.getId());
        helper.startSequence().thenWaitUntil(() -> covered(helper, realm, feet))
                .thenExecute(() -> {
                    try {
                        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, Vec3.atBottomCenterOf(TARGET.below()).add(0, 0.95, 0));
                        pig.setNoGravity(true);
                        var water = NeoForgeMod.WATER_TYPE.value();
                        pig.updateFluidHeightAndDoFluidPushing();
                        helper.assertTrue(pig.getFluidTypeHeight(water) == 0.0, "上方无真实水时，实体高于虚拟水面");
                        level.setBlock(feet.above(), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                        pig.updateFluidHeightAndDoFluidPushing();
                        double measured = pig.getFluidTypeHeight(water);
                        helper.assertTrue(measured == 0.0, "被消隐的上一格真实水抬高了虚拟水面，接触高度=" + measured);
                    } finally {
                        RealmManager.emergencyClose(level, realm);
                    }
                }).thenSucceed();
    }

    @GameTest(template = "empty", batch = "re_audit_cooldown", timeoutTicks = 250)
    public static void reloadDoesNotRestartAlreadySettledCooldown(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = owner(helper);
        var realm = open(helper, owner);
        helper.startSequence().thenWaitUntil(() -> covered(helper, realm, owner.blockPosition()))
                .thenExecute(() -> helper.assertTrue(RealmManager.requestClose(level, realm), "必须进入主动收束"))
                .thenIdle(12).thenExecute(() -> {
                    long previous = RealmCooldowns.remaining(owner);
                    helper.assertTrue(previous > 0, "主动关闭应已登记冷却");
                    RealmSets.of(level).remove(realm.id());
                    RealmManager.PendingCooldowns.apply(owner);
                    long after = RealmCooldowns.remaining(owner);
                    helper.assertTrue(after == previous, "用相同附件模拟重新加载，冷却从 " + previous + " 重置为 " + after);
                    RealmManager.PendingCooldowns.apply(owner);
                    helper.assertTrue(RealmCooldowns.remaining(owner) == previous, "重复恢复不得改变截止时间");
                }).thenSucceed();
    }

    @GameTest(template = "empty", batch = "re_audit_interrupted", timeoutTicks = 250)
    public static void interruptedOpenSettlesOnlyOnce(GameTestHelper helper) {
        var owner = owner(helper);
        var level = helper.getLevel();
        var realm = open(helper, owner);
        RealmSets.of(level).remove(realm.id());
        RealmManager.PendingCooldowns.apply(owner);
        long remaining = RealmCooldowns.remaining(owner);
        helper.assertTrue(remaining == io.zershyan.avernus.registry.config.ServerConfig.CLOSE_COOLDOWN_TICKS.get(), "未结算的中断开境应补足一次冷却");
        owner.setData(AvernusAttachments.REALM_COOLDOWN, RealmCooldown.NONE);
        RealmManager.PendingCooldowns.apply(owner);
        helper.assertTrue(RealmCooldowns.remaining(owner) == 0, "已结算的冷却到期后不能复活");
        helper.succeed();
    }
    private static LivingEntity owner(GameTestHelper helper) {
        helper.setBlock(6, 1, 3, Blocks.STONE);
        return helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, 2.0, 3.5));
    }

    private static Realm open(GameTestHelper helper, LivingEntity owner, ResourceLocation... traits) {
        var form = new MirrorForm(AvernusRealmAspects.UMBRAL.getId(), List.of(traits));
        helper.assertTrue(RealmManager.open(owner, form) == RealmManager.Result.OPENED, "测试领域应开启");
        return Objects.requireNonNull(RealmManager.find(helper.getLevel().getServer(), owner.getUUID()));
    }

    private static void covered(GameTestHelper helper, Realm realm, BlockPos pos) {
        helper.assertTrue(realm.covers(pos.getX(), pos.getZ(), helper.getLevel().getGameTime()), "测试方块须被波纹覆盖");
    }
}
