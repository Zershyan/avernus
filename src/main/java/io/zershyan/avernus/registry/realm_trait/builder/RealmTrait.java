package io.zershyan.avernus.registry.realm_trait.builder;

import io.zershyan.avernus.api.realm.RealmView;
import io.zershyan.avernus.registry.AvernusRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 类型：叠加在境相上的规则。普通可扩展类，第三方可继承并覆写钩子与冲突声明。
 */
public class RealmTrait {

    /** 声明与另一类型互斥；校验时双向检查，任一方声明即视为冲突。 */
    public boolean conflictsWith(RealmTrait other) {
        return false;
    }

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
        return AvernusRegistries.REALM_TRAIT.getKey(this);
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
        return id.toLanguageKey("realm_trait");
    }
}
