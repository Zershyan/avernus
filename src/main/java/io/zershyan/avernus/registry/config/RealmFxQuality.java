package io.zershyan.avernus.registry.config;

import io.zershyan.avernus.datagen.lang.AvernusConfigLang;
import io.zershyan.avernus.datagen.util.ConfigLangEntry;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

/** 领域特效档位；序号即着色器里的 Quality。 */
public enum RealmFxQuality implements TranslatableEnum {
    OFF(AvernusConfigLang.QualityOff, 0, 0, 0.0F, 0),
    LOW(AvernusConfigLang.QualityLow, 0, 0, 0.35F, 0),
    MEDIUM(AvernusConfigLang.QualityMedium, 4, 2, 0.6F, 0),
    HIGH(AvernusConfigLang.QualityHigh, 6, 4, 1.0F, 3),
    ULTRA(AvernusConfigLang.QualityUltra, 24, 5, 1.6F, 5);

    private final ConfigLangEntry label;
    public final int fogSteps;
    public final int bloomLevels;
    public final float particleScale;
    public final int mirrorGhosts;

    RealmFxQuality(ConfigLangEntry label, int fogSteps, int bloomLevels, float particleScale, int mirrorGhosts) {
        this.label = label;
        this.fogSteps = fogSteps;
        this.bloomLevels = bloomLevels;
        this.particleScale = particleScale;
        this.mirrorGhosts = mirrorGhosts;
    }

    public boolean atLeast(RealmFxQuality other) {
        return ordinal() >= other.ordinal();
    }

    @Override
    public Component getTranslatedName() {
        return Component.translatable(label.key());
    }
}
