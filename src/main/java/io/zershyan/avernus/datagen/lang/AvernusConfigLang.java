package io.zershyan.avernus.datagen.lang;

import io.zershyan.avernus.datagen.util.ConfigLangEntry;

import java.util.ArrayList;
import java.util.List;

public class AvernusConfigLang extends AvernusLang {
    private static final List<FinalEntry<String>> LABELS = new ArrayList<>();

    public static final ConfigLangEntry Realm = entry("realm", "Realm", "领域");
    public static final ConfigLangEntry Radius = entry("realm_radius", "Realm Radius", "领域半径");
    public static final ConfigLangEntry CloseCooldown = entry("close_cooldown_ticks", "Close Cooldown (ticks)", "关境冷却（tick）");
    public static final ConfigLangEntry Animation = entry("animation", "Animation", "演出");
    public static final ConfigLangEntry FlyDistance = entry("mirror_fly_distance", "Mirror Fly Distance", "镜子飞出距离");
    public static final ConfigLangEntry FlyTicks = entry("mirror_fly_ticks", "Mirror Fly Ticks", "镜子飞出时长");
    public static final ConfigLangEntry TurnTicks = entry("mirror_turn_ticks", "Mirror Settle Ticks", "镜子停稳时长");
    public static final ConfigLangEntry CrackTicks = entry("mirror_crack_ticks", "Mirror Crack Ticks", "镜子龟裂蓄势时长");
    public static final ConfigLangEntry Scatter = entry("shard_scatter_ticks", "Shard Scatter Ticks", "镜片飞射时长");
    public static final ConfigLangEntry Wave = entry("wave_ticks", "Wave Ticks", "波纹展开时长");
    public static final ConfigLangEntry Rebirth = entry("rebirth", "Rebirth", "往生");
    public static final ConfigLangEntry RegenerationTicks = entry("regeneration_ticks", "Regeneration Ticks", "生命恢复时长");
    public static final ConfigLangEntry RegenerationLevel = entry("regeneration_level", "Regeneration Level", "生命恢复等级");
    public static final ConfigLangEntry AbsorptionTicks = entry("absorption_ticks", "Absorption Ticks", "伤害吸收时长");
    public static final ConfigLangEntry AbsorptionLevel = entry("absorption_level", "Absorption Level", "伤害吸收等级");
    public static final ConfigLangEntry FireResistance = entry("fire_resistance_ticks", "Fire Resistance Ticks", "抗火时长");
    public static final ConfigLangEntry Visual = entry("visual", "Visual Effects", "视觉特效");
    public static final ConfigLangEntry EffectQuality = entry("effect_quality", "Effect Quality", "特效档位");
    public static final ConfigLangEntry QualityOff = entry("effect_quality.off", "Off", "关");
    public static final ConfigLangEntry QualityLow = entry("effect_quality.low", "Low", "低");
    public static final ConfigLangEntry QualityMedium = entry("effect_quality.medium", "Medium", "中");
    public static final ConfigLangEntry QualityHigh = entry("effect_quality.high", "High", "高");
    public static final ConfigLangEntry QualityUltra = entry("effect_quality.ultra", "Ultra", "极致");
    public static final ConfigLangEntry ScreenShake = entry("screen_shake", "Screen Shake", "镜头震动");
    public static final ConfigLangEntry ScreenFlash = entry("screen_flash", "Screen Flash", "闪屏");

    private static ConfigLangEntry entry(String path, String enUs, String zhCn) {
        var entry = new ConfigLangEntry(path);
        LABELS.add(new FinalEntry<>(entry.key(), new Lang(enUs, zhCn)));
        return entry;
    }

    @Override
    protected void init() {
        entries.addAll(LABELS);
    }
}
