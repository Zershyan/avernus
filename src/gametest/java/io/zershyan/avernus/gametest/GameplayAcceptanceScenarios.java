package io.zershyan.avernus.gametest;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.api.AvernusApi;
import io.zershyan.avernus.compat.AvernusCompatFactory;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.registry.AvernusDataComponents;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.data_component.MirrorForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@GameTestHolder(Avernus.MODID)
@PrefixGameTestTemplate(false)
public final class GameplayAcceptanceScenarios {
    @GameTest(template = "empty", batch = "re_audit_path_target", timeoutTicks = 250)
    public static void groundNavigationAcceptsVirtualAirAtGoal(GameTestHelper helper) {
        for (int x = 3; x <= 9; x++) for (int z = 2; z <= 12; z++) helper.setBlock(x, 1, z, Blocks.STONE);
        for (int y = 2; y <= 6; y++) helper.setBlock(6, y, 10, Blocks.STONE);
        var owner = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, 2.0, 3.5));
        var walker = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, 2.0, 6.5));
        var level = helper.getLevel();
        var form = new MirrorForm(AvernusRealmAspects.UMBRAL.getId(), List.of());
        helper.assertTrue(RealmManager.open(owner, form) == RealmManager.Result.OPENED, "测试领域应开启");
        var realm = Objects.requireNonNull(RealmManager.find(level.getServer(), owner.getUUID()));
        BlockPos target = helper.absolutePos(new BlockPos(6, 2, 10));
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(realm.covers(target.getX(), target.getZ(), level.getGameTime()), "目标列须已覆盖"))
                .thenExecute(() -> {
                    try {
                        helper.assertTrue(walker.onGround(), "寻路生物须已站稳");
                        var path = walker.getNavigation().createPath(target, 0);
                        helper.assertTrue(path != null && target.equals(path.getTarget()) && path.canReach(),
                                "有效空气列的地面目标不能被真实石柱抬高：目标=" + target + "，计算目标=" + (path == null ? "无" : path.getTarget()));
                    } finally {
                        RealmManager.emergencyClose(level, realm);
                    }
                }).thenSucceed();
    }

    @GameTest(template = "empty", batch = "re_audit_recipes", timeoutTicks = 250)
    public static void recipesPreserveNameAndRejectConflicts(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var smith = new SmithingMenu(1, player.getInventory());
        var empty = new ItemStack(AvernusApi.mirrorItem());
        var name = Component.literal("复审镜");
        empty.set(DataComponents.CUSTOM_NAME, name);
        helper.assertTrue(smith.getSlot(0).mayPlace(new ItemStack(Items.FEATHER)) && smith.getSlot(1).mayPlace(empty), "应能放入羽毛和镜子");
        player.getInventory().setItem(0, empty);
        player.getInventory().setItem(1, new ItemStack(Items.HEART_OF_THE_SEA, 2));
        helper.assertTrue(!smith.quickMoveStack(player, 31).isEmpty() && !smith.quickMoveStack(player, 32).isEmpty(), "快捷移入失败");
        var weakWater = AvernusRealmTraits.WEAK_WATER.getId();
        var output = smith.quickMoveStack(player, smith.getResultSlot());
        helper.assertTrue(AvernusApi.isMirror(output) && AvernusApi.formOf(output).hasTrait(weakWater) && output.getHoverName().equals(name), "模板留空应添加弱水并保留名字");
        helper.assertTrue(smith.getSlot(2).getItem().getCount() == 1 && !smith.getSlot(1).hasItem(), "快捷取出时只消耗一份材料和输入镜");
        smith.getSlot(1).set(output.copy());
        helper.assertFalse(smith.getSlot(3).hasItem(), "重复添加弱水不得有结果");
        smith.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
        helper.assertFalse(smith.getSlot(3).hasItem(), "弱水和无垠不能共存");
        smith.getSlot(2).set(new ItemStack(Items.HEART_OF_THE_SEA));
        smith.getSlot(0).set(new ItemStack(Items.FEATHER));
        var stripped = smith.quickMoveStack(player, 3);
        helper.assertTrue(AvernusApi.isMirror(stripped) && !AvernusApi.formOf(stripped).hasTrait(weakWater)
                && stripped.getHoverName().equals(name), "添加羽毛应移除弱水并保留名字");
        helper.assertTrue(!smith.getSlot(0).hasItem() && !smith.getSlot(1).hasItem() && !smith.getSlot(2).hasItem(), "拆除必须消耗三种输入");
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(i == 4 ? output : new ItemStack(Items.BLAZE_POWDER));
        var input = CraftingInput.of(3, 3, grid);
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
        var infernal = recipe.value().assemble(input, level.registryAccess());
        helper.assertTrue(AvernusApi.formOf(infernal).aspect().equals(AvernusRealmAspects.INFERNAL.getId())
                && AvernusApi.formOf(infernal).hasTrait(weakWater) && infernal.getHoverName().equals(name), "工作台只改境相");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "re_audit_curios_body", timeoutTicks = 250)
    public static void optionalCuriosFindsOnlyWornChestMirror(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var mirror = new ItemStack(AvernusApi.mirrorItem());
        player.getInventory().setItem(0, mirror);
        if (!AvernusCompatFactory.CURIOS.isModLoaded()) {
            helper.assertTrue(AvernusCompatFactory.CURIOS.findWornMirror(player).isEmpty(), "未装 Curios 时只降级为空");
            helper.succeed();
            return;
        }
        CuriosFixture.verify(helper, player, mirror);
    }

    private static final class CuriosFixture {
        static void verify(GameTestHelper helper, net.minecraft.world.entity.player.Player player, ItemStack mirror) {
            var stack = mirror.copy();
            stack.set(AvernusDataComponents.MIRROR_FORM.get(), new MirrorForm(AvernusRealmAspects.SILENT.getId(), List.of()));
            var inventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow();
            var body = inventory.getStacksHandler("body").orElseThrow();
            helper.assertTrue(body.getStacks().getSlots() > 0 && body.getStacks().isItemValid(0, stack), "数据生成的标签和槽允许佩戴镜子");
            var head = inventory.getStacksHandler("head").orElseThrow();
            helper.assertTrue(head.getStacks().getSlots() > 0 && head.getStacks().isItemValid(0, mirror), "测试须允许额外的头饰槽佩戴镜子");
            body.getStacks().setStackInSlot(0, stack);
            helper.assertTrue(ItemStack.matches(AvernusCompatFactory.CURIOS.findWornMirror(player), stack), "胸饰独占时应返回胸饰镜的形态");
            inventory.setSlotActive("body", 0, false);
            helper.assertTrue(AvernusCompatFactory.CURIOS.findWornMirror(player).isEmpty(), "禁用胸饰不生效");
            inventory.setSlotActive("body", 0, true);
            body.getStacks().setStackInSlot(0, ItemStack.EMPTY);
            if (body.getCosmeticStacks().getSlots() > 0) {
                body.getCosmeticStacks().setStackInSlot(0, stack);
                helper.assertTrue(AvernusCompatFactory.CURIOS.findWornMirror(player).isEmpty(), "装饰胸饰不生效");
            }
            helper.runAfterDelay(2, () -> {
                body.getStacks().setStackInSlot(0, ItemStack.EMPTY);
                head.getStacks().setStackInSlot(0, mirror.copy());
                helper.runAfterDelay(2, () -> {
                    var found = AvernusCompatFactory.CURIOS.findWornMirror(player);
                    helper.assertTrue(found.isEmpty(), "仅头饰佩戴镜子却被胸饰查找返回：" + AvernusApi.formOf(found).aspect());
                    helper.succeed();
                });
            });
        }
    }
}
