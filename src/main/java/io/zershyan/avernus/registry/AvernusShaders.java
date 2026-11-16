package io.zershyan.avernus.registry;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.client.realm_render.RealmEnvironment;
import io.zershyan.avernus.client.realm_render.RealmVisuals;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 领域客户端渲染资源：区块 Shader 与境相外观重载。区块 Shader 在原版区块图层上叠加领域覆盖裁剪，按图层一一对应。 */
public final class AvernusShaders {
    private static final Map<String, RenderType> LAYERS = Map.of(
            "solid", RenderType.solid(),
            "cutout_mipped", RenderType.cutoutMipped(),
            "cutout", RenderType.cutout(),
            "translucent", RenderType.translucent(),
            "tripwire", RenderType.tripwire());
    private static final Map<RenderType, ShaderInstance> REALM_CHUNK = new ConcurrentHashMap<>();

    private AvernusShaders() {
    }

    public static @Nullable ShaderInstance realmChunk(RenderType layer) {
        return REALM_CHUNK.get(layer);
    }

    private static void register(RegisterShadersEvent event) throws IOException {
        for (var entry : LAYERS.entrySet()) {
            // 绊线与 cutout 共用片元规则
            String program = entry.getKey().equals("tripwire") ? "cutout" : entry.getKey();
            RenderType layer = entry.getValue();
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                            Avernus.id("rendertype_realm_" + program), DefaultVertexFormat.BLOCK),
                    shader -> REALM_CHUNK.put(layer, shader));
        }
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new RealmVisuals());
        event.registerReloadListener(RealmEnvironment.reloadListener());
    }

    public static void doRegister(IEventBus modBus) {
        modBus.addListener(AvernusShaders::registerReloadListeners);
        modBus.addListener(RegisterShadersEvent.class, event -> {
            try {
                register(event);
            } catch (IOException e) {
                throw new IllegalStateException("领域区块 Shader 加载失败", e);
            }
        });
    }
}
