package io.zershyan.avernus.compat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/** 可选依赖兼容工具：第三方调用一律经 testLoadedAndRun / testLoadedAndCall，类型隔离在依赖专属实现中。 */
public interface ICompatUtils {
    Logger LOGGER = LoggerFactory.getLogger(ICompatUtils.class);

    default boolean testLoadedAndRun(Runnable runnable) {
        if (!isModLoaded()) return false;
        runnable.run();
        return true;
    }

    default <T> T testLoadedAndCall(Callable<T> callable, T errorResult) {
        try {
            if (isModLoaded()) return callable.call();
        } catch (Exception ignored) {
        }
        return errorResult;
    }

    default void addCommonListener(IEventBus forgeBus, IEventBus modBus) {
    }

    default void addClientListener(IEventBus forgeBus, IEventBus modBus) {
    }

    default void init(IEventBus forgeBus, IEventBus modBus) {
        addCommonListener(forgeBus, modBus);
        if (FMLLoader.getDist() == Dist.CLIENT) addClientListener(forgeBus, modBus);
    }

    default void initial(IEventBus forgeBus, IEventBus modBus) {
        try {
            testLoadedAndRun(() -> init(forgeBus, modBus));
        } catch (Exception e) {
            LOGGER.error("兼容初始化失败：{}", getClass().getSimpleName(), e);
        }
    }

    default void initNetwork(PayloadRegistrar registrar) {
        try {
            testLoadedAndRun(() -> registerNetwork(registrar));
        } catch (Exception e) {
            LOGGER.error("兼容网络注册失败：{}", getClass().getSimpleName(), e);
        }
    }

    default void registerNetwork(PayloadRegistrar registrar) {
    }

    boolean isModLoaded();
}
