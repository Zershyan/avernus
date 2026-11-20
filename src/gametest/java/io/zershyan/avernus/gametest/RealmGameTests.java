package io.zershyan.avernus.gametest;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.api.realm.MirrorForm;
import io.zershyan.avernus.api.realm.RealmResult;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 领域物理与保护的服务端自动验证。
 * 场地：y=1 为石头地面，所有者站在 y=2，故领域高度 H 对应相对 y=2；H 及以上的真实方块应被抹平，世界本身不得改变。
 * 各测试独占批次，避免 128 格领域互相干扰。
 */
@GameTestHolder(Avernus.MODID)
@PrefixGameTestTemplate(false)
public final class RealmGameTests {
    private static final String TEMPLATE = "empty";
    private static final int TIMEOUT = 400;
    private static final int SIZE_X = 12;
    private static final int SIZE_Z = 16;
    private static final int H = 2;

    private RealmGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "avernus_stand", timeoutTicks = TIMEOUT)
    public static void standOnFlattenedTop(GameTestHelper helper) {
        floor(helper);
        fill(helper, Blocks.STONE, 6, H, 8, 6, H + 4, 8);
        helper.setBlock(3, H - 1, 8, Blocks.AIR);
        ArmorStand onPillar = helper.spawn(EntityType.ARMOR_STAND, new Vec3(6.5, H + 5, 8.5));
        Realm realm = open(helper);
        List<ArmorStand> inHole = new ArrayList<>(1);
        helper.startSequence()
                .thenWaitUntil(() -> assertCovered(helper, realm, 3, 8, 6, 8))
                .thenExecute(() -> inHole.add(helper.spawn(EntityType.ARMOR_STAND, new Vec3(3.5, H + 0.5, 8.5))))
                .thenWaitUntil(() -> {
                    assertStandsAt(helper, onPillar, H);
                    assertStandsAt(helper, inHole.getFirst(), H - 1);
                })
                .thenExecute(() -> {
                    for (int y = H; y <= H + 4; y++) helper.assertBlockPresent(Blocks.STONE, 6, y, 8);
                    helper.assertBlockPresent(Blocks.AIR, 3, H - 1, 8);
                })
                .thenExecute(() -> close(helper, realm))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_swim", timeoutTicks = TIMEOUT)
    public static void swimInVirtualWater(GameTestHelper helper) {
        floor(helper);
        Realm realm = open(helper, AvernusRealmTraits.WEAK_WATER.getId());
        List<Pig> pig = new ArrayList<>(1);
        helper.startSequence()
                .thenWaitUntil(() -> assertCovered(helper, realm, 6, 8, 6, 8))
                .thenExecute(() -> pig.add(helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, H - 0.8, 8.5))))
                .thenWaitUntil(() -> {
                    Pig p = pig.getFirst();
                    helper.assertTrue(p.isInWater(), "猪应处于虚拟水中");
                    helper.assertTrue(p.isEyeInFluid(FluidTags.WATER), "猪的眼部应在虚拟水中");
                    helper.assertFalse(p.isInWall(), "猪不应在真实地面中窒息");
                })
                .thenExecute(() -> helper.assertBlockPresent(Blocks.STONE, 6, H - 1, 8))
                .thenExecute(() -> close(helper, realm))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_arrow", timeoutTicks = TIMEOUT)
    public static void arrowPassesFlattenedWall(GameTestHelper helper) {
        floor(helper);
        fill(helper, Blocks.STONE, 3, H, 9, 9, H + 3, 9);
        Realm realm = open(helper);
        List<Arrow> arrow = new ArrayList<>(1);
        double wallZ = helper.absoluteVec(new Vec3(0, 0, 10)).z;
        helper.startSequence()
                .thenWaitUntil(() -> assertCovered(helper, realm, 3, 5, 9, 12))
                .thenExecute(() -> {
                    Arrow a = helper.spawn(EntityType.ARROW, new Vec3(6.5, H + 1.5, 5.5));
                    a.setDeltaMovement(0.0, 0.0, 1.5);
                    arrow.add(a);
                })
                .thenWaitUntil(() -> helper.assertTrue(arrow.getFirst().getZ() > wallZ + 1.0, "箭矢应穿过被抹平的墙"))
                .thenExecute(() -> helper.assertBlockPresent(Blocks.STONE, 6, H + 1, 9))
                .thenExecute(() -> close(helper, realm))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_path", timeoutTicks = TIMEOUT)
    public static void pathThroughFlattenedPen(GameTestHelper helper) {
        floor(helper);
        fill(helper, Blocks.STONE, 3, H, 5, 9, H + 2, 5);
        fill(helper, Blocks.STONE, 3, H, 11, 9, H + 2, 11);
        fill(helper, Blocks.STONE, 3, H, 5, 3, H + 2, 11);
        fill(helper, Blocks.STONE, 9, H, 5, 9, H + 2, 11);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, H, 8.5));
        Realm realm = open(helper);
        BlockPos goal = helper.absolutePos(new BlockPos(6, H, 14));
        helper.startSequence()
                .thenWaitUntil(() -> assertCovered(helper, realm, 0, 0, SIZE_X - 1, SIZE_Z - 1))
                .thenExecute(() -> {
                    helper.assertTrue(pig.getNavigation().moveTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 1.0),
                            "应能规划出穿过被抹平围栏的路径");
                    helper.assertTrue(pig.getNavigation().getPath().canReach(), "路径应能到达围栏外的目标");
                })
                .thenWaitUntil(() -> helper.assertTrue(pig.getNavigation().isDone() && pig.getZ() > goal.getZ() - 2, "猪应走出被抹平的围栏"))
                .thenExecute(() -> helper.assertBlockPresent(Blocks.STONE, 6, H, 11))
                .thenExecute(() -> close(helper, realm))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_path_flat", timeoutTicks = TIMEOUT)
    public static void walkOverFlattenedStepsWithoutJumping(GameTestHelper helper) {
        floor(helper);
        for (int z = 7; z <= 13; z += 3) fill(helper, Blocks.STONE, 3, H, z, 9, H, z);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, H, 4.5));
        Realm realm = open(helper);
        BlockPos goal = helper.absolutePos(new BlockPos(6, H, 15));
        double ground = helper.absoluteVec(new Vec3(0, H, 0)).y;
        double[] highest = {Double.NEGATIVE_INFINITY};
        helper.onEachTick(() -> highest[0] = Math.max(highest[0], pig.getY()));
        helper.startSequence()
                .thenWaitUntil(() -> assertCovered(helper, realm, 0, 0, SIZE_X - 1, SIZE_Z - 1))
                .thenExecute(() -> {
                    highest[0] = pig.getY();
                    helper.assertTrue(pig.getNavigation().moveTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 1.0), "应能规划出平地路径");
                })
                .thenWaitUntil(() -> helper.assertTrue(pig.getNavigation().isDone() && pig.getZ() > goal.getZ() - 1.5, "猪应走到目标"))
                .thenExecute(() -> helper.assertTrue(highest[0] <= ground + 0.1, "穿过被抹平的台阶时不得起跳，最高 y=" + highest[0]))
                .thenExecute(() -> close(helper, realm))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_protect", timeoutTicks = TIMEOUT)
    public static void protectAgainstExplosionAndFalling(GameTestHelper helper) {
        floor(helper);
        fill(helper, Blocks.STONE, 6, H, 10, 6, H + 1, 10);
        Realm realm = open(helper);
        List<PrimedTnt> tnt = new ArrayList<>(1);
        helper.startSequence()
                .thenWaitUntil(() -> assertCovered(helper, realm, 2, 8, 8, 13))
                .thenExecute(() -> {
                    helper.setBlock(3, H + 3, 10, Blocks.SAND);
                    PrimedTnt t = helper.spawn(EntityType.TNT, new Vec3(6.5, H, 11.5));
                    t.setFuse(5);
                    tnt.add(t);
                })
                .thenWaitUntil(() -> helper.assertTrue(tnt.getFirst().isRemoved(), "TNT 应已爆炸"))
                .thenIdle(5)
                .thenExecute(() -> {
                    helper.assertBlockPresent(Blocks.STONE, 6, H, 10);
                    helper.assertBlockPresent(Blocks.STONE, 6, H + 1, 10);
                    helper.assertBlockPresent(Blocks.STONE, 6, H - 1, 11);
                    helper.assertBlockPresent(Blocks.SAND, 3, H + 3, 10);
                })
                .thenExecute(() -> close(helper, realm))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_decay", timeoutTicks = TIMEOUT)
    public static void protectLeafDecayAndDirectBreak(GameTestHelper helper) {
        BlockPos rel = new BlockPos(8, H, 8);
        helper.setBlock(rel, Blocks.OAK_LEAVES);
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(rel);
        Realm realm = open(helper);
        helper.startSequence()
                .thenWaitUntil(() -> assertCovered(helper, realm, 8, 8, 8, 8))
                .thenExecute(() -> {
                    try {
                        var state = level.getBlockState(pos);
                        var drops = new net.minecraft.world.phys.AABB(pos).inflate(2);
                        state.randomTick(level, pos, level.getRandom());
                        helper.assertFalse(level.destroyBlock(pos, true), "保护区内不得直接破坏方块");
                        helper.assertFalse(level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()), "保护区内不得替换原方块");
                        helper.assertBlockPresent(Blocks.OAK_LEAVES, rel);
                        helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, drops).isEmpty(), "没有破坏就不能产生掉落");
                    } finally {
                        close(helper, realm);
                    }
                    level.getBlockState(pos).randomTick(level, pos, level.getRandom());
                    helper.assertBlockPresent(Blocks.AIR, rel);
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_place_multi", timeoutTicks = TIMEOUT)
    public static void rejectReplacementInLaterPlacementSnapshot(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(7, H, 8));
        BlockPos second = first.south();
        level.setBlockAndUpdate(first.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(second.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(second, Blocks.SHORT_GRASS.defaultBlockState());
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setYRot(0);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        var bed = new ItemStack(Items.RED_BED, 2);
        player.setItemInHand(hand, bed);
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(first.below()), net.minecraft.core.Direction.UP, first.below(), false);
        var use = new net.minecraft.world.item.context.UseOnContext(player, hand, hit);
        Realm realm = open(helper);
        helper.startSequence().thenWaitUntil(() -> assertCovered(helper, realm, 7, 8, 7, 9))
                .thenExecute(() -> {
                    try {
                        helper.assertFalse(net.neoforged.neoforge.common.CommonHooks.onPlaceItemIntoWorld(use).consumesAction(), "领域内如冒险模式，床不得放置");
                        helper.assertTrue(level.isEmptyBlock(first) && level.getBlockState(second).is(Blocks.SHORT_GRASS), "床尾应回滚，草不应被替换");
                        helper.assertTrue(bed.getCount() == 2, "失败不能消耗物品");
                    } finally {
                        close(helper, realm);
                    }
                    helper.assertTrue(net.neoforged.neoforge.common.CommonHooks.onPlaceItemIntoWorld(use).consumesAction(), "关境后应恢复正常双格放置");
                    helper.assertTrue(level.getBlockState(first).is(Blocks.RED_BED) && level.getBlockState(second).is(Blocks.RED_BED) && bed.getCount() == 1, "区外两半床与物品消耗应一致");
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_fall_once", timeoutTicks = TIMEOUT)
    public static void firstContactOnLandingIsGuardedOnlyOnce(GameTestHelper helper) {
        floor(helper);
        Realm realm = open(helper);
        helper.startSequence()
                .thenWaitUntil(() -> assertCovered(helper, realm, 8, 8, 8, 8))
                .thenExecute(() -> {
                    var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(8.5, H + 3, 8.5));
                    Vec3 origin = pig.position();
                    float health = pig.getHealth();
                    try {
                        pig.fallDistance = 8.0F;
                        pig.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, -4, 0));
                        helper.assertTrue(pig.onGround() && pig.getHealth() == health, "首次接触和落地同刻应免伤");
                        pig.setPos(origin.x, origin.y, origin.z);
                        pig.fallDistance = 8.0F;
                        pig.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, -4, 0));
                        helper.assertTrue(pig.onGround() && pig.getHealth() < health, "同一次入境的第二次下落应受伤");
                    } finally {
                        close(helper, realm);
                    }
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_rebirth_opening", timeoutTicks = TIMEOUT)
    public static void rebirthWaitsUntilCovered(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        var owner = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, H, 2.5));
        Realm realm = openFor(helper, owner, AvernusRealmTraits.REBIRTH.getId());
        helper.assertTrue(realm.phase(level.getGameTime()) == io.zershyan.avernus.api.realm.RealmPhase.OPENING, "测试须在开场中");
        owner.hurt(level.damageSources().generic(), 100.0F);
        helper.assertFalse(owner.isAlive(), "波纹未覆盖时不能触发往生");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_rebirth_active", timeoutTicks = TIMEOUT)
    public static void rebirthDoesNotConsumeTotem(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        var owner = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, H, 2.5));
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
        Realm realm = openFor(helper, owner, AvernusRealmTraits.REBIRTH.getId());
        helper.startSequence()
                .thenWaitUntil(() -> assertCovered(helper, realm, 6, 2, 6, 2))
                .thenExecute(() -> {
                    owner.hurt(level.damageSources().generic(), 100.0F);
                    helper.assertTrue(owner.getHealth() == 1.0F, "覆盖内应触发往生");
                    helper.assertTrue(owner.getMainHandItem().is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING), "不应同时消耗图腾");
                    helper.assertTrue(RealmManager.find(level.getServer(), owner.getUUID()) == null, "往生须立即关境");
                    helper.assertTrue(io.zershyan.avernus.realm.RealmCooldowns.remaining(owner) > 0, "往生关境须结算冷却");
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_restore_edge", timeoutTicks = TIMEOUT)
    public static void restoreVehicleWhenOnlyAdjacentColumnChanges(GameTestHelper helper) {
        floor(helper);
        fill(helper, Blocks.STONE, 9, H, 8, 9, H + 1, 9);
        var level = helper.getLevel();
        var owner = helper.spawn(EntityType.ARMOR_STAND, new Vec3(7.5, H, 8.5));
        var form = new MirrorForm(AvernusRealmAspects.UMBRAL.getId(), List.of(AvernusRealmTraits.ETERNITY.getId()));
        var timeline = io.zershyan.avernus.realm.RealmTimeline.fromConfig();
        Vec3 original = owner.position();
        long now = level.getGameTime();
        var motion = new Realm.Motion(0, -1, original.x, original.z, original, original.y + 2, 0);
        var state = new Realm.Snapshot(java.util.UUID.randomUUID(), owner.getUUID(), owner.getId(), level.dimension(), form,
                owner.getBlockY(), 4, Long.MAX_VALUE, now - timeline.shatterDelay() - timeline.waveTicks(), 42, timeline,
                original, 0, 0, 0, motion);
        Realm realm = state.instantiate();
        if (realm == null) throw new AssertionError("测试境相无效");
        var realms = RealmSets.of(level);
        realms.put(realm);
        try {
            var boat = helper.spawn(EntityType.BOAT, new Vec3(8.9, H, 8.8));
            var feet = boat.position();
            io.zershyan.avernus.realm.RealmPresenceTracker.tick(boat);
            realm.moveAnchor(original.add(-2.5, 0, 0), original.y + 2, false);
            helper.assertTrue(realm.covers(boat.getBlockX(), boat.getBlockZ(), now), "车辆中心仍在同一个领域");
            helper.assertFalse(realm.covers(helper.absolutePos(new BlockPos(9, H, 8)).getX(), boat.getBlockZ(), now), "旁边墙列应已恢复");
            io.zershyan.avernus.realm.RealmPresenceTracker.tick(boat);
            helper.assertTrue(Math.abs(boat.getY() - (feet.y + 2.1)) < 1.0E-5 && boat.getX() == feet.x && boat.getZ() == feet.z,
                    "车辆只沿 Y 轴恢复到能支撑完整碰撞箱的位置，并略微抬高防止再次卡入");
        } finally {
            realms.remove(realm.id());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_illusion", timeoutTicks = TIMEOUT)
    public static void illusionKeepsIndividualRetaliation(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        Realm realm = open(helper, AvernusRealmTraits.ILLUSION.getId());
        var attacker = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var bystander = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        attacker.setPos(helper.absoluteVec(new Vec3(7.5, H, 7.5)));
        bystander.setPos(helper.absoluteVec(new Vec3(9.5, H, 7.5)));
        var zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(8.0, H, 8.5));
        var piglin = helper.spawnWithNoFreeWill(EntityType.PIGLIN, new Vec3(8.8, H, 8.5));
        piglin.setImmuneToZombification(true);
        zombie.setTarget(bystander);
        piglin.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET, bystander);
        helper.startSequence()
                .thenWaitUntil(() -> assertCovered(helper, realm, 7, 7, 9, 8))
                .thenIdle(2)
                .thenExecute(() -> {
                    try {
                        helper.assertTrue(zombie.getTarget() == null, "僵尸的旧目标必须在入境后清掉");
                        helper.assertFalse(piglin.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET), "猪灵的攻击记忆必须清掉");
                        helper.assertTrue(piglin.hurt(level.damageSources().playerAttack(attacker), 1), "攻击者须实际造成伤害");
                        helper.assertFalse(io.zershyan.avernus.realm.RealmTraitEffects.forbidsTarget(piglin, attacker), "猪灵可以对实际攻击者反击");
                        helper.assertTrue(io.zershyan.avernus.realm.RealmTraitEffects.forbidsTarget(piglin, bystander), "反击许可不能扩张给其他玩家");
                        zombie.setTarget(bystander);
                        helper.assertTrue(zombie.getTarget() == null, "幻化阻止新建未获许可的目标");
                    } finally {
                        close(helper, realm);
                    }
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_observe_order", timeoutTicks = TIMEOUT)
    public static void rejectStaleObservationPackets(GameTestHelper helper) {
        var set = new io.zershyan.avernus.realm.RealmSet();
        set.beginUpdates(3);
        helper.assertTrue(set.acceptUpdate(3, 1), "首次状态应接收");
        helper.assertTrue(set.acceptUpdate(3, 3), "新状态应接收");
        helper.assertFalse(set.acceptUpdate(3, 2), "迟到更新不能复活已移除的领域");
        set.beginUpdates(4);
        set.beginUpdates(2);
        helper.assertFalse(set.acceptUpdate(3, 100), "上一维度观察流的迟到更新必须丢弃");
        helper.assertTrue(set.acceptUpdate(4, 1), "新的观察流应独立接收状态");
        helper.succeed();
    }
    @GameTest(template = TEMPLATE, batch = "avernus_rebirth_overlap", timeoutTicks = TIMEOUT)
    public static void newerRealmOverridesRebirth(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        var owner = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, H, 2.5));
        var other = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, H, 2.5));
        Realm protectedRealm = openFor(helper, owner, AvernusRealmTraits.REBIRTH.getId());
        Realm newer = openFor(helper, other);
        helper.startSequence()
                .thenWaitUntil(() -> assertCovered(helper, newer, 6, 2, 6, 2))
                .thenExecute(() -> {
                    try {
                        helper.assertTrue(RealmSets.of(level).effectAt(owner.getBlockX(), owner.getBlockZ(), level.getGameTime()) == newer,
                                "新境应赢得同列覆盖");
                        owner.hurt(level.damageSources().generic(), 100.0F);
                        helper.assertFalse(owner.isAlive(), "被其他境盖过时不能从旧境触发往生");
                    } finally {
                        close(helper, protectedRealm);
                        close(helper, newer);
                    }
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_rebirth_outside", timeoutTicks = TIMEOUT)
    public static void rebirthDoesNotProtectOwnerOutsideRealm(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        var owner = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, H, 2.5));
        Realm realm = openFor(helper, owner, AvernusRealmTraits.REBIRTH.getId());
        helper.startSequence()
                .thenWaitUntil(() -> assertCovered(helper, realm, 6, 2, 6, 2))
                .thenExecute(() -> {
                    try {
                        owner.setPos(realm.exactCenterX() + realm.radius() + 2, owner.getY(), realm.exactCenterZ());
                        owner.hurt(level.damageSources().generic(), 100.0F);
                        helper.assertFalse(owner.isAlive(), "离开固定境后不能获得往生保护");
                    } finally {
                        close(helper, realm);
                    }
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_snapshot", timeoutTicks = TIMEOUT)
    public static void snapshotFreezesMotionAndRoundTrips(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        var owner = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, H, 2.5));
        Realm realm = openFor(helper, owner, AvernusRealmTraits.ETERNITY.getId());
        var before = realm.snapshot();
        var bytes = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
        try {
            Realm.Snapshot.STREAM_CODEC.encode(bytes, before);
            var decoded = Realm.Snapshot.STREAM_CODEC.decode(bytes);
            helper.assertTrue(before.equals(decoded), "完整领域快照编码后须逐项一致");
            double height = realm.height();
            owner.teleportTo(owner.getX() + 5, owner.getY() + 3, owner.getZ() + 2);
            helper.assertTrue(realm.teleportSeq() == before.motion().teleportSeq() + 1, "短距离传送也必须记录瞬贴序号");
            helper.assertTrue(realm.height() == height && realm.sequence() == before.sequence(), "移动不得改变抹平高度或优先级");
            helper.assertTrue(!before.motion().equals(realm.motion()), "发送后的不可变快照不得跟着服务端改变");
            Realm.Motion.STREAM_CODEC.encode(bytes, realm.motion());
            var moved = Realm.Motion.STREAM_CODEC.decode(bytes);
            Realm copy = decoded.instantiate();
            if (copy == null) throw new AssertionError("无法重建领域快照");
            copy.applyMotion(moved);
            copy.applyMotion(before.motion());
            helper.assertTrue(copy.motion().equals(moved), "旧修订不能覆盖最新增量");
            helper.assertTrue(bytes.readableBytes() == 0, "Codec 应完整消费数据");
        } finally {
            bytes.release();
            close(helper, realm);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "avernus_coverage", timeoutTicks = TIMEOUT)
    public static void coverageTilesMatchAuthoritativeColumns(GameTestHelper helper) {
        floor(helper);
        Realm realm = open(helper);
        var level = helper.getLevel();
        long base = realm.startTime();
        long[] times = {base, base + realm.timeline().shatterDelay() + 1, base + realm.timeline().shatterDelay() + 20,
                base + realm.timeline().shatterDelay() + realm.timeline().waveTicks()};
        try {
            int cx = realm.centerX(), cz = realm.centerZ();
            for (long time : times) {
                var coverage = realm.coverage(time);
                for (int x = cx - 144; x <= cx + 144; x += 16) {
                    for (int z = cz - 144; z <= cz + 144; z += 16) {
                        int relation = coverage.tile(x, z, 16);
                        for (int dx = 0; dx < 16; dx++) {
                            for (int dz = 0; dz < 16; dz++) {
                                boolean expected = realm.covers(x + dx, z + dz, time);
                                helper.assertTrue(coverage.covers(x + dx, z + dz) == expected, "列覆盖快照须与权威覆盖一致");
                                helper.assertTrue(relation == 0 || expected == (relation == 1), "整区块覆盖快路径不能跳过边界格");
                            }
                        }
                    }
                }
            }
        } finally {
            close(helper, realm);
        }
        helper.succeed();
    }

    private static Realm openFor(GameTestHelper helper, net.minecraft.world.entity.LivingEntity owner, ResourceLocation... traits) {
        MirrorForm form = new MirrorForm(AvernusRealmAspects.UMBRAL.getId(), List.of(traits));
        helper.assertTrue(RealmManager.open(owner, form) == RealmResult.OPENED, "开境失败");
        return java.util.Objects.requireNonNull(RealmManager.find(helper.getLevel().getServer(), owner.getUUID()));
    }

    private static void floor(GameTestHelper helper) {
        fill(helper, Blocks.STONE, 0, H - 1, 0, SIZE_X - 1, H - 1, SIZE_Z - 1);
    }

    private static void fill(GameTestHelper helper, Block block, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (BlockPos pos : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) helper.setBlock(pos, block);
    }

    /** 清掉先前失败测试遗留的领域后，由场地南侧的盔甲架开境。 */
    private static Realm open(GameTestHelper helper, ResourceLocation... traits) {
        for (Realm stale : List.copyOf(RealmSets.of(helper.getLevel()).all())) {
            RealmManager.emergencyClose(helper.getLevel(), stale);
        }
        ArmorStand owner = helper.spawn(EntityType.ARMOR_STAND, new Vec3(6.5, H, 2.5));
        MirrorForm form = new MirrorForm(AvernusRealmAspects.UMBRAL.getId(), List.of(traits));
        helper.assertTrue(RealmManager.open(owner, form) == RealmResult.OPENED, "开境失败");
        return RealmManager.find(helper.getLevel().getServer(), owner.getUUID());
    }

    private static void close(GameTestHelper helper, Realm realm) {
        RealmManager.emergencyClose(helper.getLevel(), realm);
    }

    private static void assertCovered(GameTestHelper helper, Realm realm, int x0, int z0, int x1, int z1) {
        long time = helper.getLevel().getGameTime();
        for (BlockPos rel : BlockPos.betweenClosed(x0, 0, z0, x1, 0, z1)) {
            BlockPos pos = helper.absolutePos(rel);
            helper.assertTrue(realm.covers(pos.getX(), pos.getZ(), time), "波纹尚未覆盖 " + rel.toShortString());
        }
    }

    private static void assertStandsAt(GameTestHelper helper, Entity entity, int relY) {
        double expected = helper.absoluteVec(new Vec3(0, relY, 0)).y;
        helper.assertTrue(entity.onGround() && Math.abs(entity.getY() - expected) < 1.0E-3,
                "实体应站在相对 y=" + relY + "，实际 " + (entity.getY() - expected + relY));
    }
}
