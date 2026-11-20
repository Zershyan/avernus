package io.zershyan.avernus.gametest;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.zershyan.avernus.api.AvernusApi;
import io.zershyan.avernus.api.event.RealmEvent;
import io.zershyan.avernus.api.realm.MirrorForm;
import io.zershyan.avernus.api.realm.RealmPhase;
import io.zershyan.avernus.api.realm.RealmResult;
import io.zershyan.avernus.api.realm.RealmView;
import io.zershyan.avernus.api.recipe.MirrorTraitRecipeView;
import io.zershyan.avernus.realm.MirrorForms;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.registry.data_component.MirrorTraits;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@GameTestHolder(AvernusApi.MODID)
@PrefixGameTestTemplate(false)
public final class ApiAcceptanceScenarios {
    private static final ResourceLocation UMBRAL = id("umbral");
    private static final ResourceLocation EQUALITY = id("equality");
    private static final ResourceLocation FREEDOM = id("freedom");
    private static final ResourceLocation MISSING = id("missing_api_test");

    @GameTest(template = "empty", batch = "api_signatures")
    public static void publicSignaturesDoNotExposeInternals(GameTestHelper helper) {
        for (Class<?> api : List.of(AvernusApi.class, MirrorForm.class, RealmView.class, RealmResult.class,
                RealmPhase.class, RealmEvent.class, MirrorTraitRecipeView.class)) {
            checkSignatures(helper, api);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "api_form")
    public static void immutableFormsPreserveWireAndComponentFormat(GameTestHelper helper) {
        var input = new ArrayList<>(List.of(FREEDOM, EQUALITY, FREEDOM));
        var form = new MirrorForm(UMBRAL, input);
        input.clear();
        helper.assertTrue(form.traits().equals(List.of(EQUALITY, FREEDOM)), "形态必须独立拷贝、去重排序");
        helper.assertTrue(form.hasTrait(EQUALITY), "类型 ID 查询必须有效");
        assertReadOnly(helper, form.traits()::clear);

        var encoded = Unpooled.buffer();
        var previousFormat = Unpooled.buffer();
        try {
            MirrorForms.STREAM_CODEC.encode(encoded, form);
            ResourceLocation.STREAM_CODEC.encode(previousFormat, form.aspect());
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MirrorForm.MAX_TRAITS)).encode(previousFormat, form.traits());
            helper.assertTrue(ByteBufUtil.equals(encoded, previousFormat), "API 迁移不得改变形态网络字节布局");
            helper.assertTrue(form.equals(MirrorForms.STREAM_CODEC.decode(encoded)) && !encoded.isReadable(), "形态编解码必须完整往返");
            encoded.clear();
            var traits = new MirrorTraits(form.traits());
            MirrorTraits.STREAM_CODEC.encode(encoded, traits);
            helper.assertTrue(traits.equals(MirrorTraits.STREAM_CODEC.decode(encoded)), "数据组件网络格式必须往返");
            var json = MirrorTraits.CODEC.encodeStart(JsonOps.INSTANCE, traits).getOrThrow();
            helper.assertTrue(traits.equals(MirrorTraits.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow()), "数据组件持久化格式必须往返");
        } finally {
            encoded.release();
            previousFormat.release();
        }
        var tooMany = new ArrayList<ResourceLocation>();
        for (int i = 0; i <= MirrorForm.MAX_TRAITS; i++) tooMany.add(id("unknown_trait_" + i));
        helper.assertTrue(MirrorForms.resolve(new MirrorForm(UMBRAL, tooMany)).isEmpty(), "超出类型上限必须被拒绝");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "api_mirrors")
    public static void mirrorsAreCreatedOnlyFromValidPublicForms(GameTestHelper helper) {
        var items = AvernusApi.mirrorItems();
        helper.assertTrue(!items.isEmpty(), "镜子物品查询不得为空");
        assertReadOnly(helper, items::clear);
        for (var item : items) {
            var base = new ItemStack(item);
            var baseForm = AvernusApi.formOf(base).orElseThrow();
            helper.assertTrue(AvernusApi.isMirror(base) && AvernusApi.mirrorItem(baseForm.aspect()) == item, "境相 ID 必须查到对应镜子");
            var form = new MirrorForm(baseForm.aspect(), List.of(EQUALITY));
            var first = AvernusApi.createMirror(form);
            var second = AvernusApi.createMirror(form);
            helper.assertTrue(first.is(item) && form.equals(AvernusApi.formOf(first).orElseThrow()), "创建镜子必须保留公开形态");
            first.setCount(0);
            helper.assertTrue(second.getCount() == 1 && AvernusApi.formOf(base).orElseThrow().traits().isEmpty(), "新物品栈不能共享可变状态");
        }
        helper.assertTrue(AvernusApi.formOf(new ItemStack(Items.STONE)).isEmpty(), "非镜子不得返回形态");
        helper.assertTrue(AvernusApi.mirrorItem(MISSING) == null && AvernusApi.createMirror(new MirrorForm(MISSING, List.of())).isEmpty(), "未知境相不得创建镜子");
        helper.assertTrue(AvernusApi.createMirror(new MirrorForm(UMBRAL, List.of(MISSING))).isEmpty(), "未知类型不得创建镜子");
        helper.assertTrue(AvernusApi.createMirror(new MirrorForm(UMBRAL, List.of(id("boundless"), id("weak_water")))).isEmpty(), "互斥类型不得创建镜子");
        long recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.SMITHING).stream()
                .filter(holder -> holder.value() instanceof MirrorTraitRecipeView).count();
        helper.assertTrue(recipes > 0, "类型配方必须可经公开只读接口访问");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "api_lifecycle")
    public static void lifecycleUsesPublicResultsAndViews(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = helper.spawn(EntityType.ARMOR_STAND, new Vec3(8.5, 2, 8.5));
        owner.setNoGravity(true);
        var form = new MirrorForm(UMBRAL, List.of(EQUALITY));
        try {
            helper.assertTrue(AvernusApi.open(owner, new MirrorForm(MISSING, List.of())) == RealmResult.INVALID_FORM, "无效境相必须返回公开失败结果");
            helper.assertTrue(AvernusApi.realmOf(owner) == null, "无效开境不能留下领域");
            helper.assertTrue(AvernusApi.open(owner, form) == RealmResult.OPENED, "公开入口应成功开境");
            var view = Objects.requireNonNull(AvernusApi.realmOf(owner));
            helper.assertTrue(view.aspectId().equals(UMBRAL) && view.traitIds().equals(form.traits()) && view.hasTrait(EQUALITY), "领域视图只读 ID 必须对应原形态");
            helper.assertTrue(view.ownerId().equals(owner.getUUID()) && view.dimension().equals(level.dimension()), "视图必须保留所有者和维度");
            assertReadOnly(helper, view.traitIds()::clear);
            var snapshot = AvernusApi.realmsIn(level);
            helper.assertTrue(snapshot.contains(view), "维度查询必须包含该领域");
            assertReadOnly(helper, snapshot::clear);
            helper.assertTrue(AvernusApi.open(owner, form) == RealmResult.INVALID_SUBJECT, "重复开境必须被拒绝");
            helper.assertTrue(AvernusApi.toggle(owner, form) == RealmResult.CLOSING && view.phase(level.getGameTime()) == RealmPhase.CLOSING, "切换应请求收境并更新视图");
            helper.assertTrue(AvernusApi.toggle(owner, form) == RealmResult.ALREADY_CLOSING && !AvernusApi.close(owner), "重复收境必须幂等");
            RealmManager.emergencyCloseOwned(level.getServer(), owner.getUUID());
            helper.assertTrue(snapshot.contains(view) && AvernusApi.realmOf(owner) == null, "领域列表必须为不可变快照");
            helper.assertTrue(AvernusApi.cooldownRemaining(owner) > 0 && AvernusApi.open(owner, form) == RealmResult.COOLDOWN, "收境后应返回冷却结果");
            owner.discard();
            helper.assertTrue(AvernusApi.open(owner, form) == RealmResult.INVALID_SUBJECT, "失效主体不能开境");
        } finally {
            RealmManager.emergencyCloseOwned(level.getServer(), owner.getUUID());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "api_coverage", timeoutTicks = 250)
    public static void realmQueriesFollowActualCoverage(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = helper.spawn(EntityType.ARMOR_STAND, new Vec3(8.5, 2, 8.5));
        owner.setNoGravity(true);
        helper.assertTrue(AvernusApi.open(owner, new MirrorForm(UMBRAL, List.of())) == RealmResult.OPENED, "公开入口应开境");
        var view = Objects.requireNonNull(AvernusApi.realmOf(owner));
        helper.assertTrue(AvernusApi.realmAt(level, owner.getBlockX(), owner.getBlockZ()) == null, "开场时未覆盖列不得生效");
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                AvernusApi.realmAt(level, owner.getBlockX(), owner.getBlockZ()) == view, "波纹覆盖后应查询到同一只读视图"))
                .thenExecute(() -> {
                    helper.assertTrue(view.covers(owner.getBlockX(), owner.getBlockZ(), level.getGameTime()), "视图覆盖查询必须有效");
                    RealmManager.emergencyCloseOwned(level.getServer(), owner.getUUID());
                    helper.assertTrue(AvernusApi.realmAt(level, owner.getBlockX(), owner.getBlockZ()) == null, "移除领域后不得残留查询结果");
                }).thenSucceed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AvernusApi.MODID, path);
    }

    private static void assertReadOnly(GameTestHelper helper, Runnable mutation) {
        boolean rejected = false;
        try {
            mutation.run();
        } catch (UnsupportedOperationException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "公开集合必须不可修改");
    }

    private static void checkSignatures(GameTestHelper helper, Class<?> api) {
        var checked = new HashSet<Type>();
        checkType(helper, api.getGenericSuperclass(), checked);
        for (Type parent : api.getGenericInterfaces()) checkType(helper, parent, checked);
        for (Type parameter : api.getTypeParameters()) checkType(helper, parameter, checked);
        for (var method : api.getDeclaredMethods()) {
            if (!visible(method.getModifiers())) continue;
            checkType(helper, method.getGenericReturnType(), checked);
            for (Type parameter : method.getGenericParameterTypes()) checkType(helper, parameter, checked);
            for (Type parameter : method.getTypeParameters()) checkType(helper, parameter, checked);
            for (Type exception : method.getGenericExceptionTypes()) checkType(helper, exception, checked);
        }
        for (var field : api.getDeclaredFields()) {
            if (visible(field.getModifiers())) checkType(helper, field.getGenericType(), checked);
        }
        for (var constructor : api.getDeclaredConstructors()) {
            if (!visible(constructor.getModifiers())) continue;
            for (Type parameter : constructor.getGenericParameterTypes()) checkType(helper, parameter, checked);
            for (Type exception : constructor.getGenericExceptionTypes()) checkType(helper, exception, checked);
        }
        for (Class<?> nested : api.getDeclaredClasses()) {
            if (visible(nested.getModifiers())) checkSignatures(helper, nested);
        }
    }

    private static boolean visible(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    private static void checkType(GameTestHelper helper, Type type, Set<Type> checked) {
        if (type == null || !checked.add(type)) return;
        if (type instanceof Class<?> clazz) {
            helper.assertFalse(clazz.getName().startsWith("io.zershyan.avernus.") && !clazz.getName().startsWith("io.zershyan.avernus.api."),
                    "API 签名泄漏内部类型：" + clazz.getName());
            if (clazz.isArray()) checkType(helper, clazz.getComponentType(), checked);
        } else if (type instanceof ParameterizedType parameterized) {
            checkType(helper, parameterized.getRawType(), checked);
            checkType(helper, parameterized.getOwnerType(), checked);
            for (Type argument : parameterized.getActualTypeArguments()) checkType(helper, argument, checked);
        } else if (type instanceof GenericArrayType array) {
            checkType(helper, array.getGenericComponentType(), checked);
        } else if (type instanceof WildcardType wildcard) {
            for (Type bound : wildcard.getUpperBounds()) checkType(helper, bound, checked);
            for (Type bound : wildcard.getLowerBounds()) checkType(helper, bound, checked);
        } else if (type instanceof TypeVariable<?> variable) {
            for (Type bound : variable.getBounds()) checkType(helper, bound, checked);
        }
    }
}
