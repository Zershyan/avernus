package io.zershyan.avernus.compat;

import io.zershyan.avernus.compat.curios.CuriosCompat;
import net.neoforged.bus.api.IEventBus;

public final class AvernusCompatFactory {
    public static final CuriosCompat CURIOS = new CuriosCompat();

    public static void doRegister(IEventBus modBus) {
        CURIOS.testLoadedAndRun(() -> CURIOS.register(modBus));
    }
}
