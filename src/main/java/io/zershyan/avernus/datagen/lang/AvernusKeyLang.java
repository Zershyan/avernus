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

    public static final MutableComponent CreativeTab = entry("itemGroup." + Avernus.MODID, "Avernus", "厄渊之境");

    public static final MutableComponent TooltipLore1 = entry(TOOLTIP + "lore.1",
            "Beyond the glass lies Avernus, the lake no bird may cross;",
            "镜后是阿维尔诺，飞鸟不渡的湖；");
    public static final MutableComponent TooltipLore2 = entry(TOOLTIP + "lore.2",
            "shatter it, and the world beneath your feet is rewritten in its likeness.",
            "击碎它，脚下的世界便照它的模样重写。");
    public static final MutableComponent TooltipHint = entry(TOOLTIP + "hint",
            "Hold Shift: aspect · Alt: special effects · Ctrl: usage",
            "按住 Shift 查看境相 · Alt 查看特殊效果 · Ctrl 查看用法");
    public static final LazyComponent TooltipAspect = entryLazy(TOOLTIP + "aspect", "Aspect: %s", "境相：%s");
    public static final MutableComponent TooltipEffects = entry(TOOLTIP + "effects", "Special effects:", "特殊效果：");
    public static final LazyComponent TooltipEffect = entryLazy(TOOLTIP + "effect", " %s: %s", " %s：%s");
    public static final MutableComponent TooltipNoEffects = entry(TOOLTIP + "no_effects", "No special effects", "无特殊效果");
    public static final MutableComponent TooltipInvalidForm = entry(TOOLTIP + "invalid_form",
            "Invalid form: unknown or conflicting entries", "形态无效：包含未知或互斥的条目");
    public static final MutableComponent TooltipUse = entry(TOOLTIP + "use",
            "Use to open or close your realm", "右键开启或关闭自己的领域");
    public static final LazyComponent TooltipCurioKey = entryLazy(TOOLTIP + "curio_key",
            "Worn as body charm: press %s to toggle", "佩戴于胸饰：按 %s 开关领域");
    public static final MutableComponent TooltipUnbound = entry(TOOLTIP + "unbound", "Unbound", "未绑定");
    public static final MutableComponent TooltipSmithingHint = entry(TOOLTIP + "smithing_hint",
            "Smithing: a sigil with an empty template engraves its effect; with a feather it is removed",
            "锻造：模板留空放入印记即刻入其效果，模板放羽毛则移除");
    public static final MutableComponent TooltipAspectHint = entry(TOOLTIP + "aspect_hint",
            "Crafting: surround the mirror with eight aspect materials to change its aspect",
            "工作台：镜子四周围满八个境相材料即可转换境相");
    public static final MutableComponent TooltipSigilUsage = entry(TOOLTIP + "sigil_usage",
            "Smithing table: leave the template empty to engrave it into a mirror; a feather removes it",
            "锻造台：模板留空即可刻入镜子；模板放羽毛则移除");

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
