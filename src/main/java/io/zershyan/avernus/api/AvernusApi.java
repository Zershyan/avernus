package io.zershyan.avernus.api;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.api.realm.MirrorForm;
import io.zershyan.avernus.api.realm.RealmResult;
import io.zershyan.avernus.api.realm.RealmView;
import io.zershyan.avernus.realm.MirrorForms;
import io.zershyan.avernus.realm.RealmCooldowns;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.data_component.MirrorTraits;
import io.zershyan.avernus.registry.item.MirrorItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/** 稳定的对外入口；开关领域只能在服务端主线程调用，物品查询需等待注册完成。 */
public final class AvernusApi {
    public static final String MODID = Avernus.MODID;

    private AvernusApi() {
    }

    /** 以该形态开境；已有领域、冷却中或形态无效时返回失败结果。 */
    public static RealmResult open(LivingEntity owner, MirrorForm form) {
        return RealmManager.open(owner, form);
    }

    /** 请求收境并结算冷却；无领域、已在收境或非服务端调用时返回 false。 */
    public static boolean close(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) return false;
        var realm = RealmManager.find(level.getServer(), owner.getUUID());
        return realm != null && RealmManager.requestClose(level, realm);
    }

    /** 已有领域则请求收境，否则以该形态开境；仅限服务端主线程。 */
    public static RealmResult toggle(LivingEntity owner, MirrorForm form) {
        return RealmManager.toggle(owner, form);
    }

    /** 所有者当前领域的只读视图；无领域或非服务端调用时返回 null。 */
    public static @Nullable RealmView realmOf(LivingEntity owner) {
        return owner.level() instanceof ServerLevel level ? RealmManager.find(level.getServer(), owner.getUUID()) : null;
    }

    /** 该维度领域的不可修改列表快照；其中视图反映实时状态，客户端只含已同步领域。 */
    public static List<? extends RealmView> realmsIn(Level level) {
        return RealmSets.of(level).all();
    }

    /** 该列已覆盖的最新领域视图，无覆盖时返回 null，不区分观察者。 */
    public static @Nullable RealmView realmAt(Level level, int x, int z) {
        return RealmSets.of(level).effectAt(x, z, level.getGameTime());
    }

    /** 实体当前适用的最新境；尊重覆盖、境面高度与隔绝境印，无境返回 null，客户端只含已同步领域。 */
    public static @Nullable RealmView realmAt(Entity entity) {
        if (entity.isRemoved()) return null;
        Level level = entity.level();
        return RealmSets.of(level).geometryAt(entity, entity.getBlockX(), entity.getBlockZ(), level.getGameTime());
    }

    /** 实体当前境的境相 ID（含第三方境相），无境返回 null；友军免疫属性不影响所在境查询。 */
    public static @Nullable ResourceLocation aspectAt(Entity entity) {
        RealmView realm = realmAt(entity);
        return realm == null ? null : realm.aspectId();
    }

    /** 剩余冷却 tick；无需冷却时返回 0。 */
    public static long cooldownRemaining(LivingEntity entity) {
        return RealmCooldowns.remaining(entity);
    }

    public static boolean isMirror(ItemStack stack) {
        return stack.getItem() instanceof MirrorItem;
    }

    /** 该境相 ID 对应的镜子物品；未注册专属镜子的境相返回 null。 */
    public static @Nullable Item mirrorItem(ResourceLocation aspect) {
        for (var entry : AvernusItems.MIRRORS_BY_ASPECT.entrySet()) {
            if (entry.getKey().getId().equals(aspect)) return entry.getValue().get();
        }
        return null;
    }

    /** 已注册镜子物品的不可修改列表，保持注册顺序。 */
    public static List<Item> mirrorItems() {
        return AvernusItems.MIRRORS_BY_ASPECT.values().stream().<Item>map(mirror -> mirror.get()).toList();
    }

    /** 按有效形态创建独立的新物品栈；无效形态或没有专属镜子时返回 EMPTY。 */
    public static ItemStack createMirror(MirrorForm form) {
        Item item = mirrorItem(form.aspect());
        if (item == null || MirrorForms.resolve(form).isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        MirrorTraits.write(stack, new MirrorTraits(form.traits()));
        return stack;
    }

    /** 提取镜子的不可变形态；非镜子物品返回空，形态的注册有效性在使用时校验。 */
    public static Optional<MirrorForm> formOf(ItemStack stack) {
        return stack.getItem() instanceof MirrorItem mirror ? Optional.of(mirror.form(stack)) : Optional.empty();
    }
}
