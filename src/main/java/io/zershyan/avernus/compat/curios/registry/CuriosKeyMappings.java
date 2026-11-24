package io.zershyan.avernus.compat.curios.registry;

import com.mojang.blaze3d.platform.InputConstants;
import io.zershyan.avernus.api.AvernusApi;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.jetbrains.annotations.Nullable;

/** 胸饰镜开关键，仅在安装 Curios 时注册，默认未绑定。 */
public final class CuriosKeyMappings {
    private static final String NAME = "key." + AvernusApi.MODID + ".toggle_realm";
    private static final String CATEGORY = "key." + AvernusApi.MODID + ".category";
    private static @Nullable KeyMapping toggleRealm;

    private CuriosKeyMappings() {
    }

    public static @Nullable KeyMapping toggleRealm() {
        return toggleRealm;
    }

    public static void doRegister(RegisterKeyMappingsEvent event) {
        toggleRealm = new KeyMapping(NAME, InputConstants.UNKNOWN.getValue(), CATEGORY);
        event.register(toggleRealm);
    }
}
