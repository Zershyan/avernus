package io.zershyan.avernus.registry;

import com.mojang.blaze3d.platform.InputConstants;
import io.zershyan.avernus.compat.AvernusCompatFactory;
import io.zershyan.avernus.datagen.lang.AvernusKeyLang;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.jetbrains.annotations.Nullable;

/** 胸饰镜开关键，仅在安装 Curios 时注册，默认未绑定。 */
public final class AvernusKeyMappings {
    private static @Nullable KeyMapping toggleRealm;

    private AvernusKeyMappings() {
    }

    public static @Nullable KeyMapping toggleRealm() {
        return toggleRealm;
    }

    private static void register(RegisterKeyMappingsEvent event) {
        toggleRealm = new KeyMapping(AvernusKeyLang.KeyToggleRealm, InputConstants.UNKNOWN.getValue(), AvernusKeyLang.KeyCategory);
        event.register(toggleRealm);
    }

    public static void doRegister(IEventBus modBus) {
        AvernusCompatFactory.CURIOS.testLoadedAndRun(() -> modBus.addListener(AvernusKeyMappings::register));
    }
}
