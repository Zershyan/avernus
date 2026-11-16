package io.zershyan.avernus.registry.config;

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
    public static final ModConfigSpec.IntValue SHARD_SCATTER_TICKS;
    public static final ModConfigSpec.IntValue WAVE_TICKS;
    public static final ModConfigSpec.IntValue REASSEMBLE_TICKS;

    public static final ModConfigSpec.IntValue REBIRTH_REGENERATION_TICKS;
    public static final ModConfigSpec.IntValue REBIRTH_REGENERATION_LEVEL;
    public static final ModConfigSpec.IntValue REBIRTH_ABSORPTION_TICKS;
    public static final ModConfigSpec.IntValue REBIRTH_ABSORPTION_LEVEL;
    public static final ModConfigSpec.IntValue REBIRTH_FIRE_RESISTANCE_TICKS;

    static {
        BUILDER.push("realm");
        REALM_RADIUS = BUILDER.comment("领域水平半径（格）").defineInRange("realm_radius", 128, 4, 512);
        CLOSE_COOLDOWN_TICKS = BUILDER.comment("关闭领域后使用者的冷却（tick）").defineInRange("close_cooldown_ticks", 1200, 0, 72000);
        BUILDER.pop();

        BUILDER.push("animation");
        MIRROR_FLY_DISTANCE = BUILDER.comment("普通镜飞出距离（格）").defineInRange("mirror_fly_distance", 4.0, 1.0, 16.0);
        MIRROR_FLY_TICKS = BUILDER.comment("镜子飞出/升起时长").defineInRange("mirror_fly_ticks", 20, 1, 200);
        MIRROR_TURN_TICKS = BUILDER.comment("镜子转向使用者时长").defineInRange("mirror_turn_ticks", 8, 1, 200);
        SHARD_SCATTER_TICKS = BUILDER.comment("镜片飞射时长").defineInRange("shard_scatter_ticks", 12, 1, 200);
        WAVE_TICKS = BUILDER.comment("波纹覆盖完整领域的时长").defineInRange("wave_ticks", 60, 1, 1200);
        REASSEMBLE_TICKS = BUILDER.comment("关闭时镜片重组并飞回的时长").defineInRange("reassemble_ticks", 20, 1, 200);
        BUILDER.pop();

        BUILDER.push("rebirth");
        REBIRTH_REGENERATION_TICKS = BUILDER.defineInRange("regeneration_ticks", 900, 0, 72000);
        REBIRTH_REGENERATION_LEVEL = BUILDER.defineInRange("regeneration_level", 2, 1, 10);
        REBIRTH_ABSORPTION_TICKS = BUILDER.defineInRange("absorption_ticks", 100, 0, 72000);
        REBIRTH_ABSORPTION_LEVEL = BUILDER.defineInRange("absorption_level", 2, 1, 10);
        REBIRTH_FIRE_RESISTANCE_TICKS = BUILDER.defineInRange("fire_resistance_ticks", 800, 0, 72000);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
