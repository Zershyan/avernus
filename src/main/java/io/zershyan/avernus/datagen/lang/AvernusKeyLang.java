package io.zershyan.avernus.datagen.lang;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.datagen.util.LazyComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public class AvernusKeyLang extends AvernusLang {
    private static final List<FinalEntry<String>> TRANSLATABLE = new ArrayList<>();
    private static final String TOOLTIP = "tooltip." + Avernus.MODID + ".";
    private static final String MESSAGE = "message." + Avernus.MODID + ".";
    private static final String KEY = "key." + Avernus.MODID + ".";

    public static final MutableComponent Resource = entry(Avernus.MODID + ".resources", "Avernus Resources", "厄渊资源");

    public static final String KeyCategory = entryString(KEY + "category", "Avernus", "厄渊");
    public static final String KeyToggleRealm = entryString(KEY + "toggle_realm", "Toggle Worn Mirror Realm", "开关胸饰镜领域");

    public static final LazyComponent TooltipAspect = entryLazy(TOOLTIP + "aspect", "Aspect: %s", "境相：%s");
    public static final LazyComponent TooltipTrait = entryLazy(TOOLTIP + "trait", " - %s", " - %s");
    public static final MutableComponent TooltipNoTraits = entry(TOOLTIP + "no_traits", "No traits", "无附加类型");
    public static final MutableComponent TooltipInvalidForm = entry(TOOLTIP + "invalid_form",
            "Invalid form: unknown or conflicting entries", "形态无效：包含未知或互斥的条目");
    public static final MutableComponent TooltipUse = entry(TOOLTIP + "use",
            "Use to open or close your realm", "右键开启或关闭自己的领域");
    public static final LazyComponent TooltipCurioKey = entryLazy(TOOLTIP + "curio_key",
            "Worn as body charm: press %s to toggle", "佩戴于胸饰：按 %s 开关领域");
    public static final MutableComponent TooltipUnbound = entry(TOOLTIP + "unbound", "Unbound", "未绑定");
    public static final MutableComponent TooltipSmithingHint = entry(TOOLTIP + "smithing_hint",
            "Smithing: empty template adds a trait; feather removes it", "锻造：模板留空添加类型，放羽毛移除类型");

    public static final LazyComponent MessageCooldown = entryLazy(MESSAGE + "cooldown",
            "The mirror is still recovering (%s s)", "镜子尚未复原（%s 秒）");
    public static final MutableComponent MessageInvalidForm = entry(MESSAGE + "invalid_form",
            "This mirror's form is invalid", "这面镜子的形态无效");

    private static String entryString(String key, String enUs, String zhCn) {
        TRANSLATABLE.add(new FinalEntry<>(key, new Lang(enUs, zhCn)));
        return key;
    }

    private static MutableComponent entry(String key, String enUs, String zhCn) {
        return Component.translatable(entryString(key, enUs, zhCn));
    }

    private static LazyComponent entryLazy(String key, String enUs, String zhCn) {
        return new LazyComponent(entryString(key, enUs, zhCn));
    }

    @Override
    protected void init() {
        entries.addAll(TRANSLATABLE);
    }
}
