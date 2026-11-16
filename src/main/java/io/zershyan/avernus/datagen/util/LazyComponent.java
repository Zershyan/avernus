package io.zershyan.avernus.datagen.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** 带占位符的延迟组件，调用时传参。 */
public record LazyComponent(String key) {
    public MutableComponent get(Object... args) {
        return Component.translatable(key, args);
    }
}
