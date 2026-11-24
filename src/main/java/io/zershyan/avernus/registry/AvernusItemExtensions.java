package io.zershyan.avernus.registry;

import io.zershyan.avernus.client.mirror.MirrorItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;

/** 镜子的客户端物品渲染扩展注册；仅在客户端调用。 */
public final class AvernusItemExtensions {
    private AvernusItemExtensions() {
    }

    public static void doRegister(IEventBus modBus) {
        modBus.addListener(AvernusItemExtensions::register);
    }

    private static void register(RegisterClientExtensionsEvent event) {
        IClientItemExtensions extensions = new IClientItemExtensions() {
            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return MirrorItemRenderer.instance();
            }
        };
        AvernusItems.MIRRORS_BY_ASPECT.values().forEach(mirror -> event.registerItem(extensions, mirror.get()));
    }
}
