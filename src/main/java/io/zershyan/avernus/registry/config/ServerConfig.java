package io.zershyan.avernus.registry.config;

import io.zershyan.avernus.datagen.lang.AvernusConfigLang;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 服务端玩法配置；半径与演出在开境时冻结，境相属性按当前配置实时计算。 */
public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue REALM_RADIUS;
    public static final ModConfigSpec.IntValue CLOSE_COOLDOWN_TICKS;

    public static final ModConfigSpec.DoubleValue UMBRAL_OWNER_SPEED_BONUS;
    public static final ModConfigSpec.DoubleValue UMBRAL_OWNER_JUMP_BONUS;
    public static final ModConfigSpec.DoubleValue UMBRAL_OTHER_SPEED_PENALTY;
    public static final ModConfigSpec.DoubleValue INFERNAL_OWNER_ATTACK_BONUS;
    public static final ModConfigSpec.DoubleValue INFERNAL_OTHER_ATTACK_PENALTY;
    public static final ModConfigSpec.DoubleValue SILENT_OWNER_ARMOR_BONUS;
    public static final ModConfigSpec.DoubleValue SILENT_OWNER_TOUGHNESS_BONUS;
    public static final ModConfigSpec.DoubleValue SILENT_OTHER_ARMOR_PENALTY;
    public static final ModConfigSpec.DoubleValue SILENT_OTHER_TOUGHNESS_PENALTY;

    public static final ModConfigSpec.DoubleValue MIRROR_FLY_DISTANCE;
    public static final ModConfigSpec.IntValue MIRROR_FLY_TICKS;
    public static final ModConfigSpec.IntValue MIRROR_TURN_TICKS;
    public static final ModConfigSpec.IntValue MIRROR_CRACK_TICKS;
    public static final ModConfigSpec.IntValue SHARD_SCATTER_TICKS;
    public static final ModConfigSpec.IntValue WAVE_TICKS;

    public static final ModConfigSpec.IntValue REBIRTH_REGENERATION_TICKS;
    public static final ModConfigSpec.IntValue REBIRTH_REGENERATION_LEVEL;
    public static final ModConfigSpec.IntValue REBIRTH_ABSORPTION_TICKS;
    public static final ModConfigSpec.IntValue REBIRTH_ABSORPTION_LEVEL;
    public static final ModConfigSpec.IntValue REBIRTH_FIRE_RESISTANCE_TICKS;

    static {
        BUILDER.push(AvernusConfigLang.Realm.name());
        REALM_RADIUS = BUILDER.comment("领域水平半径（格）").defineInRange(AvernusConfigLang.Radius.name(), 128, 4, 512);
        CLOSE_COOLDOWN_TICKS = BUILDER.comment("关闭领域后使用者的冷却（tick）").defineInRange(AvernusConfigLang.CloseCooldown.name(), 1200, 0, 72000);
        BUILDER.pop();

        BUILDER.comment("属性修饰均为独立总乘区；0.2 表示 20%，0 表示禁用，修改后影响已开启的领域")
                .push(AvernusConfigLang.AspectAttributes.name());
        BUILDER.push(AvernusConfigLang.Umbral.name());
        UMBRAL_OWNER_SPEED_BONUS = BUILDER.defineInRange(AvernusConfigLang.OwnerSpeedBonus.name(), 0.2, 0.0, 10.0);
        UMBRAL_OWNER_JUMP_BONUS = BUILDER.comment("起跳强度而非实际跳跃高度的增幅")
                .defineInRange(AvernusConfigLang.OwnerJumpBonus.name(), 0.2, 0.0, 10.0);
        UMBRAL_OTHER_SPEED_PENALTY = BUILDER.defineInRange(AvernusConfigLang.OtherSpeedPenalty.name(), 0.2, 0.0, 1.0);
        BUILDER.pop();
        BUILDER.push(AvernusConfigLang.Infernal.name());
        INFERNAL_OWNER_ATTACK_BONUS = BUILDER.defineInRange(AvernusConfigLang.OwnerAttackBonus.name(), 0.2, 0.0, 10.0);
        INFERNAL_OTHER_ATTACK_PENALTY = BUILDER.defineInRange(AvernusConfigLang.OtherAttackPenalty.name(), 0.2, 0.0, 1.0);
        BUILDER.pop();
        BUILDER.push(AvernusConfigLang.Silent.name());
        SILENT_OWNER_ARMOR_BONUS = BUILDER.defineInRange(AvernusConfigLang.OwnerArmorBonus.name(), 0.25, 0.0, 10.0);
        SILENT_OWNER_TOUGHNESS_BONUS = BUILDER.defineInRange(AvernusConfigLang.OwnerToughnessBonus.name(), 0.25, 0.0, 10.0);
        SILENT_OTHER_ARMOR_PENALTY = BUILDER.defineInRange(AvernusConfigLang.OtherArmorPenalty.name(), 0.25, 0.0, 1.0);
        SILENT_OTHER_TOUGHNESS_PENALTY = BUILDER.defineInRange(AvernusConfigLang.OtherToughnessPenalty.name(), 0.25, 0.0, 1.0);
        BUILDER.pop(2);

        BUILDER.push(AvernusConfigLang.Animation.name());
        MIRROR_FLY_DISTANCE = BUILDER.comment("普通镜飞出距离（格）").defineInRange(AvernusConfigLang.FlyDistance.name(), 4.0, 1.0, 16.0);
        MIRROR_FLY_TICKS = BUILDER.comment("镜子飞出/升起时长，也是收回时飞回的时长").defineInRange(AvernusConfigLang.FlyTicks.name(), 20, 1, 200);
        MIRROR_TURN_TICKS = BUILDER.comment("镜子飞到位后停稳、开始龟裂前的停顿").defineInRange(AvernusConfigLang.TurnTicks.name(), 4, 0, 200);
        MIRROR_CRACK_TICKS = BUILDER.comment("镜子停稳后龟裂、透光直至碎裂的蓄势时长，收回时裂纹以同样时长愈合").defineInRange(AvernusConfigLang.CrackTicks.name(), 16, 1, 200);
        SHARD_SCATTER_TICKS = BUILDER.comment("镜片飞射至停住的时长，收回时倒放").defineInRange(AvernusConfigLang.Scatter.name(), 36, 1, 400);
        WAVE_TICKS = BUILDER.comment("波纹覆盖完整领域的时长").defineInRange(AvernusConfigLang.Wave.name(), 60, 1, 1200);
        BUILDER.pop();

        BUILDER.push(AvernusConfigLang.Rebirth.name());
        REBIRTH_REGENERATION_TICKS = BUILDER.defineInRange(AvernusConfigLang.RegenerationTicks.name(), 900, 0, 72000);
        REBIRTH_REGENERATION_LEVEL = BUILDER.defineInRange(AvernusConfigLang.RegenerationLevel.name(), 2, 1, 10);
        REBIRTH_ABSORPTION_TICKS = BUILDER.defineInRange(AvernusConfigLang.AbsorptionTicks.name(), 100, 0, 72000);
        REBIRTH_ABSORPTION_LEVEL = BUILDER.defineInRange(AvernusConfigLang.AbsorptionLevel.name(), 2, 1, 10);
        REBIRTH_FIRE_RESISTANCE_TICKS = BUILDER.defineInRange(AvernusConfigLang.FireResistance.name(), 800, 0, 72000);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
