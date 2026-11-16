package io.zershyan.avernus.datagen.util;

import io.zershyan.avernus.Avernus;

/** 配置项语言键：<modid>.configuration.<name> */
public record ConfigLangEntry(String name) {
    public String key() {
        return Avernus.MODID + ".configuration." + name;
    }
}
