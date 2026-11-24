package io.zershyan.avernus.registry.realm_aspect.builder;

import io.zershyan.avernus.api.realm.RealmView;
import io.zershyan.avernus.registry.AvernusRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 境相：领域的主体形态。普通可扩展类，第三方可继承并覆写服务端生命周期钩子；
 * 客户端外观由资源包中的 realm_visual 声明提供，不在此持有客户端类型。
 */
public class RealmAspect {

    /** 领域开始展开时于服务端调用一次。 */
    public void onRealmOpened(RealmView realm) {
    }

    /** 领域彻底结束时于服务端调用一次，含紧急关闭。 */
    public void onRealmClosed(RealmView realm) {
    }

    /** 领域存续期间每服务端 tick 调用。 */
    public void tickRealm(RealmView realm) {
    }

    public final @Nullable ResourceLocation getId() {
        return AvernusRegistries.REALM_ASPECT.get().getKey(this);
    }

    public Component getName() {
        ResourceLocation id = getId();
        return id == null ? Component.empty() : Component.translatable(languageKey(id));
    }

    /** 一句话说明，语言键为名称键加 {@code .desc}。 */
    public MutableComponent getDescription() {
        ResourceLocation id = getId();
        return id == null ? Component.empty() : Component.translatable(descriptionKey(id));
    }

    public static String descriptionKey(ResourceLocation id) {
        return languageKey(id) + ".desc";
    }

    public static String languageKey(ResourceLocation id) {
        return id.toLanguageKey("realm_aspect");
    }
}
