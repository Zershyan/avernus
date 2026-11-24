package io.zershyan.avernus.compat.curios.event;

import io.zershyan.avernus.compat.curios.registry.CuriosKeyMappings;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Curios 客户端初始化：胸饰镜开关键。 */
public final class CuriosClientSetupEvent {
    private CuriosClientSetupEvent() {
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        CuriosKeyMappings.doRegister(event);
    }
}
