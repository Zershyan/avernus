package io.zershyan.avernus.gametest;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.data_component.MirrorForm;
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
        helper.assertTrue(RealmManager.open(owner, form) == RealmManager.Result.OPENED, "开境失败");
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
