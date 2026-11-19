package io.zershyan.avernus.datagen.provider;

import com.mojang.serialization.JsonOps;
import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.client.realm_render.RealmAtmosphere;
import io.zershyan.avernus.client.realm_render.RealmVisualProfile;
import io.zershyan.avernus.client.realm_render.RealmVisuals;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** 内置境相外观；与运行时共用同一 Codec 编码。 */
public final class RealmVisualProvider implements DataProvider {
    private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

    private final PackOutput.PathProvider paths;

    public RealmVisualProvider(PackOutput output) {
        paths = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, RealmVisuals.DIRECTORY);
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput output) {
        var profiles = Map.of(
                AvernusRealmAspects.UMBRAL.getId(), profile("umbral", 0x2E8A5E, 0x5CE0A0, 0x0E2A20, 48.0F, RealmAtmosphere.WISP, 0x7DFFC4),
                AvernusRealmAspects.INFERNAL.getId(), profile("infernal", 0xC2501A, 0xFF8A3A, 0x3A1206, 40.0F, RealmAtmosphere.EMBER, 0xFFB347),
                AvernusRealmAspects.SILENT.getId(), profile("silent", 0x2A5AA8, 0x78AAFF, 0x0C1630, 56.0F, RealmAtmosphere.FROST, 0xCFEAFF));
        return CompletableFuture.allOf(profiles.entrySet().stream().map(entry -> DataProvider.saveStable(output,
                RealmVisualProfile.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(),
                paths.json(entry.getKey()))).toArray(CompletableFuture[]::new));
    }

    private static RealmVisualProfile profile(String aspect, int water, int tint, int fog, float distance, RealmAtmosphere atmosphere, int glow) {
        return new RealmVisualProfile(Avernus.id("block/realm/" + aspect), STILL, FLOW, Avernus.id("textures/entity/mirror/" + aspect + ".png"),
                water, tint, fog, distance, atmosphere, glow);
    }

    @Override
    public @NotNull String getName() { return "Avernus Realm Visuals"; }
}
