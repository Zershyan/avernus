package io.zershyan.avernus.registry.config;

import io.zershyan.avernus.datagen.lang.AvernusConfigLang;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 服务端玩法配置；新开领域读取当时快照，热重载不改动已开启的领域。 */
public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue REALM_RADIUS;
    public static final ModConfigSpec.IntValue CLOSE_COOLDOWN_TICKS;

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
