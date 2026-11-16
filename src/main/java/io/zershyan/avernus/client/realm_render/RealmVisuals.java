package io.zershyan.avernus.client.realm_render;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

/** 读取 {@code realm_visual/} 下的境相外观声明，键为境相 ID。 */
public final class RealmVisuals extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "realm_visual";
    private static volatile Map<ResourceLocation, RealmVisualProfile> profiles = Map.of();

    public RealmVisuals() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, RealmVisualProfile> loaded = new HashMap<>();
        entries.forEach((id, json) -> RealmVisualProfile.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> Avernus.LOGGER.error("境相外观 {} 解析失败: {}", id, error))
                .ifPresent(profile -> loaded.put(id, profile)));
        profiles = Map.copyOf(loaded);
    }

    public static RealmVisualProfile get(RealmAspect aspect) {
        ResourceLocation id = aspect.getId();
        return id == null ? RealmVisualProfile.FALLBACK : profiles.getOrDefault(id, RealmVisualProfile.FALLBACK);
    }
}
