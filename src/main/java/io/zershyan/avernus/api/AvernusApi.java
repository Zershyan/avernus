package io.zershyan.avernus.api;

import io.zershyan.avernus.api.realm.RealmView;
import io.zershyan.avernus.realm.RealmCooldowns;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.registry.AvernusDataComponents;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.data_component.MirrorForm;
import io.zershyan.avernus.registry.item.MirrorItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 对外稳定入口；兼容层与第三方只经由此类访问厄渊，不引用内部实现。开关操作仅限服务端。 */
public final class AvernusApi {
    private AvernusApi() {
    }

    /** 以该形态开境；已有领域或冷却中则失败。 */
    public static RealmManager.Result open(LivingEntity owner, MirrorForm form) {
        return RealmManager.open(owner, form);
    }

    /** 请求关闭该主体的领域（进入收束演出并结算冷却）。 */
    public static boolean close(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) return false;
        var realm = RealmManager.find(level.getServer(), owner.getUUID());
        return realm != null && RealmManager.requestClose(level, realm);
    }

    /** 与镜子使用一致的开关行为。 */
    public static RealmManager.Result toggle(LivingEntity owner, MirrorForm form) {
        return RealmManager.toggle(owner, form);
    }

    public static @Nullable RealmView realmOf(LivingEntity owner) {
        return owner.level() instanceof ServerLevel level ? RealmManager.find(level.getServer(), owner.getUUID()) : null;
    }

    /** 该维度当前全部领域（客户端为已同步的视图）。 */
    public static List<? extends RealmView> realmsIn(Level level) {
        return RealmSets.of(level).all();
    }

    /** 该列对任意观察者生效的最新领域。 */
    public static @Nullable RealmView realmAt(Level level, int x, int z) {
        return RealmSets.of(level).effectAt(x, z, level.getGameTime());
    }

    public static long cooldownRemaining(LivingEntity entity) {
        return RealmCooldowns.remaining(entity);
    }

    public static boolean isMirror(ItemStack stack) {
        return stack.getItem() instanceof MirrorItem;
    }

    public static Item mirrorItem() {
        return AvernusItems.MIRROR.get();
    }

    public static MirrorForm formOf(ItemStack stack) {
        return stack.getOrDefault(AvernusDataComponents.MIRROR_FORM, MirrorItem.DEFAULT_FORM);
    }
}
