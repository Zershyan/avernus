package io.zershyan.avernus.compat;

import io.zershyan.avernus.compat.curios.CuriosCompat;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashSet;
import java.util.Set;

/** 兼容工厂：持有全部兼容实现，由主类与网络注册处统一触发。 */
public final class AvernusCompatFactory {
    private static final Set<ICompatUtils> COMPAT = new HashSet<>();

    public static final CuriosCompat CURIOS = addCompat(new CuriosCompat());

    private AvernusCompatFactory() {
    }

    private static <T extends ICompatUtils> T addCompat(T utils) {
        if (COMPAT.add(utils)) return utils;
        throw new IllegalStateException("兼容实现重复注册：" + utils.getClass().getName());
    }

    public static void register(IEventBus forgeBus, IEventBus modBus) {
        COMPAT.forEach(utils -> utils.initial(forgeBus, modBus));
    }

    public static void registerNetwork(SimpleChannel channel) {
        COMPAT.forEach(utils -> utils.initNetwork(channel));
    }
}
