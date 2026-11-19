package io.zershyan.avernus.registry;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.client.mirror.MirrorSolid;
import io.zershyan.avernus.client.realm_render.RealmEnvironment;
import io.zershyan.avernus.client.realm_render.RealmRenderState;
import io.zershyan.avernus.client.realm_render.RealmVisuals;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 领域客户端渲染资源：区块 Shader、特效 Shader 与境相外观重载。
 * 区块 Shader 在原版区块图层上叠加领域覆盖裁剪，按图层一一对应，加载失败即报错；特效 Shader 加载失败只降级。
 */
public final class AvernusShaders {
    private static final Map<String, RenderType> LAYERS = Map.of(
            "solid", RenderType.solid(),
            "cutout_mipped", RenderType.cutoutMipped(),
            "cutout", RenderType.cutout(),
            "translucent", RenderType.translucent());
    private static final Map<RenderType, ShaderInstance> REALM_CHUNK = new ConcurrentHashMap<>();

    public enum Effect {
        POST_FOG("realm_post_fog", DefaultVertexFormat.POSITION),
        POST_FX("realm_post_fx", DefaultVertexFormat.POSITION),
        POST_SCENE("realm_post_scene", DefaultVertexFormat.POSITION),
        POST_BLOOM_DOWN("realm_post_bloom_down", DefaultVertexFormat.POSITION),
        POST_BLOOM_UP("realm_post_bloom_up", DefaultVertexFormat.POSITION),
        POST_COMPOSITE("realm_post_composite", DefaultVertexFormat.POSITION),
        MIRROR("rendertype_realm_mirror", DefaultVertexFormat.NEW_ENTITY);

        public final String path;
        private final VertexFormat format;
        private volatile @Nullable ShaderInstance shader;

        Effect(String path, VertexFormat format) {
            this.path = path;
            this.format = format;
        }

        public @Nullable ShaderInstance get() {
            return shader;
        }
    }

    private AvernusShaders() {
    }

    public static @Nullable ShaderInstance realmChunk(RenderType layer) {
        return REALM_CHUNK.get(layer);
    }

    private static void register(RegisterShadersEvent event) throws IOException {
        for (var entry : LAYERS.entrySet()) {
            RenderType layer = entry.getValue();
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                            Avernus.id("rendertype_realm_" + entry.getKey()), DefaultVertexFormat.BLOCK), shader -> {
                REALM_CHUNK.put(layer, shader);
                if (layer == RenderType.cutout()) REALM_CHUNK.put(RenderType.tripwire(), shader);
            });
        }
        for (Effect effect : Effect.values()) {
            effect.shader = null;
            try {
                event.registerShader(new ShaderInstance(event.getResourceProvider(), Avernus.id(effect.path), effect.format),
                        shader -> effect.shader = shader);
            } catch (IOException e) {
                Avernus.LOGGER.error("领域特效 Shader {} 加载失败，相关特效已停用", effect.path, e);
            }
        }
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new RealmVisuals());
        event.registerReloadListener(RealmEnvironment.reloadListener());
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            RealmRenderState.reset();
            MirrorSolid.clear();
        });
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
