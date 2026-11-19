package io.zershyan.avernus.compat;

import java.util.function.Supplier;

public interface ICompatUtils {
    boolean isModLoaded();

    default void testLoadedAndRun(Runnable action) {
        if (isModLoaded()) action.run();
    }

    default <T> T testLoadedAndCall(Supplier<T> action, T fallback) {
        return isModLoaded() ? action.get() : fallback;
    }
}
