package io.zershyan.avernus.registry.config;

import io.zershyan.avernus.datagen.lang.AvernusConfigLang;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 客户端画面配置，运行中修改立即生效。 */
public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.EnumValue<RealmFxQuality> QUALITY;
    public static final ModConfigSpec.BooleanValue SCREEN_SHAKE;
    public static final ModConfigSpec.BooleanValue SCREEN_FLASH;

    static {
        BUILDER.push(AvernusConfigLang.Visual.name());
        QUALITY = BUILDER.comment("领域特效档位：关闭时保留基础染色与雾").defineEnum(AvernusConfigLang.EffectQuality.name(), RealmFxQuality.HIGH);
        SCREEN_SHAKE = BUILDER.comment("镜子碎裂与波前经过时的镜头震动").define(AvernusConfigLang.ScreenShake.name(), true);
        SCREEN_FLASH = BUILDER.comment("镜子碎裂与波前经过时的闪屏").define(AvernusConfigLang.ScreenFlash.name(), true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static RealmFxQuality quality() {
        return QUALITY.get();
    }
}
