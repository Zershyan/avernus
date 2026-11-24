package io.zershyan.avernus.datagen.lang;

import io.zershyan.avernus.datagen.util.ConfigLangEntry;

import java.util.ArrayList;
import java.util.List;

public class AvernusConfigLang extends AvernusLang {
    private static final List<FinalEntry<String>> LABELS = new ArrayList<>();

    public static final ConfigLangEntry Realm = entry("realm", "Realm", "领域");
    public static final ConfigLangEntry Radius = entry("realm_radius", "Realm Radius", "领域半径");
    public static final ConfigLangEntry CloseCooldown = entry("close_cooldown_ticks", "Close Cooldown (ticks)", "关境冷却（tick）");
    public static final ConfigLangEntry AspectAttributes = entry("aspect_attributes", "Aspect Attributes", "境相属性");
    public static final ConfigLangEntry Umbral = entry("umbral", "Umbral", "幽冥境相");
    public static final ConfigLangEntry Infernal = entry("infernal", "Infernal", "炼狱境相");
    public static final ConfigLangEntry Silent = entry("silent", "Silent", "寂静境相");
    public static final ConfigLangEntry OwnerSpeedBonus = entry("owner_speed_bonus", "Owner Movement Speed Bonus", "开境者移速增幅");
    public static final ConfigLangEntry OwnerJumpBonus = entry("owner_jump_bonus", "Owner Jump Strength Bonus", "开境者跳跃强度增幅");
    public static final ConfigLangEntry OtherSpeedPenalty = entry("other_speed_penalty", "Other Movement Speed Reduction", "非开境者移速降幅");
    public static final ConfigLangEntry OwnerAttackBonus = entry("owner_attack_bonus", "Owner Attack Damage Bonus", "开境者攻击伤害增幅");
    public static final ConfigLangEntry OtherAttackPenalty = entry("other_attack_penalty", "Other Attack Damage Reduction", "非开境者攻击伤害降幅");
    public static final ConfigLangEntry OwnerArmorBonus = entry("owner_armor_bonus", "Owner Armor Bonus", "开境者护甲增幅");
    public static final ConfigLangEntry OwnerToughnessBonus = entry("owner_toughness_bonus", "Owner Armor Toughness Bonus", "开境者盔甲韧性增幅");
    public static final ConfigLangEntry OtherArmorPenalty = entry("other_armor_penalty", "Other Armor Reduction", "非开境者护甲降幅");
    public static final ConfigLangEntry OtherToughnessPenalty = entry("other_toughness_penalty", "Other Armor Toughness Reduction", "非开境者盔甲韧性降幅");
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
