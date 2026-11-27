package io.zershyan.avernus.datagen.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.AvernusShaders;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class RealmShaderProvider implements DataProvider {
    private final PackOutput.PathProvider paths;

    public RealmShaderProvider(PackOutput output) {
        paths = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "shaders");
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (String layer : List.of("solid", "cutout", "cutout_mipped", "translucent")) {
            String name = "rendertype_realm_" + layer;
            writes.add(DataProvider.saveStable(output, terrain(name), paths.json(Avernus.id("core/" + name))));
        }
        post(writes, output, AvernusShaders.Effect.POST_FOG, List.of("DepthSampler", "NoiseSampler"),
                "InvViewProj", "CameraPos", "Downscale", "FogSteps", "Time", "Quality", "Strength", "Atmosphere", "Tint", "Glow", "FogTint",
                "FogDistance", "FogDensity", "SurfaceY", "Bounds", "RealmShape", "RealmSource", "RealmLook", "RealmCount", "Shock");
        post(writes, output, AvernusShaders.Effect.POST_FX, List.of("DepthSampler", "NoiseSampler"),
                "InvViewProj", "Downscale", "Time", "Glow", "RealmShape", "RealmSource", "RealmLook", "RealmCount", "Shock");
        post(writes, output, AvernusShaders.Effect.POST_SCENE, List.of("DiffuseSampler", "DepthSampler", "FogSampler", "FxSampler", "NoiseSampler"),
                "InvViewProj", "Downscale", "Time", "Quality", "Strength", "Atmosphere", "SurfaceY", "Tint", "Glow", "FogTint", "FogDistance", "Bounds", "Source",
                "RealmShape", "RealmSource", "RealmLook", "RealmCount");
        post(writes, output, AvernusShaders.Effect.POST_BLOOM_DOWN, List.of("SourceSampler"), "Prefilter", "Strength");
        post(writes, output, AvernusShaders.Effect.POST_BLOOM_UP, List.of("SourceSampler"));
        post(writes, output, AvernusShaders.Effect.POST_COMPOSITE, List.of("SceneSampler", "BloomSampler"),
                "Quality", "Strength", "Atmosphere", "Time", "Flash");
        String mirror = AvernusShaders.Effect.MIRROR.path;
        String ghost = AvernusShaders.Effect.MIRROR_GHOST.path;
        writes.add(DataProvider.saveStable(output, mirror(mirror, false), paths.json(Avernus.id("core/" + mirror))));
        writes.add(DataProvider.saveStable(output, mirror(mirror, true), paths.json(Avernus.id("core/" + ghost))));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private static JsonObject terrain(String fragment) {
        var program = program("rendertype_realm", fragment, List.of("Position", "Color", "UV0", "UV2", "Normal"),
                "Sampler0", "Sampler2", "RealmColumns");
        blend(program, "srcalpha", "1-srcalpha");
        var uniforms = program.getAsJsonArray("uniforms");
        matrix(uniforms, "ModelViewMat");
        matrix(uniforms, "ProjMat");
        uniform(uniforms, "ChunkOffset", "float", 0, 0, 0);
        uniform(uniforms, "ColorModulator", "float", 1, 1, 1, 1);
        uniform(uniforms, "FogStart", "float", 0);
        uniform(uniforms, "FogEnd", "float", 1);
        uniform(uniforms, "FogColor", "float", 0, 0, 0, 0);
        uniform(uniforms, "FogShape", "int", 0);
        uniform(uniforms, "RealmBounds", "int", 0, 0, 0, 0);
        uniform(uniforms, "RealmTableOrigin", "int", 0, 0);
        uniform(uniforms, "RealmTableSize", "int", 1);
        uniform(uniforms, "RealmSectionOrigin", "int", 0, 0, 0);
        uniform(uniforms, "RealmMeshMode", "int", 0);
        uniform(uniforms, "RealmStyle", "int", 0);
        uniform(uniforms, "RealmEncloseA", "float", 0, 0, -1, 0);
        uniform(uniforms, "RealmEncloseB", "float", 0, 0, -1, 0);
        return program;
    }

    private static JsonObject mirror(String name, boolean ghost) {
        var program = program(name, name, List.of("Position", "Color", "UV0", "UV1", "UV2", "Normal"), "Sampler0");
        blend(program, "srcalpha", ghost ? "one" : "1-srcalpha");
        var uniforms = program.getAsJsonArray("uniforms");
        matrix(uniforms, "ModelViewMat");
        matrix(uniforms, "ProjMat");
        uniform(uniforms, "ColorModulator", "float", 1, 1, 1, 1);
        uniform(uniforms, "Light0_Direction", "float", 0, 0, 0);
        uniform(uniforms, "Light1_Direction", "float", 0, 0, 0);
        uniform(uniforms, "FogStart", "float", 0);
        uniform(uniforms, "FogEnd", "float", 1);
        uniform(uniforms, "FogColor", "float", 0, 0, 0, 0);
        uniform(uniforms, "FogShape", "int", 0);
        uniform(uniforms, "GameTime", "float", 0);
        return program;
    }

    private void post(List<CompletableFuture<?>> writes, CachedOutput output, AvernusShaders.Effect effect, List<String> samplers, String... uniforms) {
        writes.add(DataProvider.saveStable(output, post(effect.path, samplers, uniforms), paths.json(Avernus.id("core/" + effect.path))));
    }

    /** 特效 Shader 只声明实际用到的 uniform，类型与默认值统一在此登记。 */
    private static JsonObject post(String fragment, List<String> samplers, String... uniformNames) {
        var program = program("realm_post", fragment, List.of("Position"), samplers.toArray(String[]::new));
        if (fragment.equals(AvernusShaders.Effect.POST_BLOOM_UP.path)) blend(program, "one", "one");
        var uniforms = program.getAsJsonArray("uniforms");
        for (String name : uniformNames) {
            Spec spec = POST_UNIFORMS.get(name);
            if (spec == null) throw new IllegalArgumentException("未登记的 uniform: " + name);
            uniform(uniforms, name, spec.type, spec.defaults);
        }
        return program;
    }

    private record Spec(String type, int... defaults) {
    }

    private static final int[] IDENTITY = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    private static final int[] ZERO_MATRIX = new int[16];
    private static final Map<String, Spec> POST_UNIFORMS = Map.ofEntries(
            Map.entry("ViewProj", new Spec("matrix4x4", IDENTITY)),
            Map.entry("InvViewProj", new Spec("matrix4x4", IDENTITY)),
            Map.entry("CameraPos", new Spec("float", 0, 0, 0)),
            Map.entry("Time", new Spec("float", 0)),
            Map.entry("Quality", new Spec("int", 0)),
            Map.entry("Downscale", new Spec("int", 2)),
            Map.entry("FogSteps", new Spec("int", 0)),
            Map.entry("Prefilter", new Spec("int", 0)),
            Map.entry("Strength", new Spec("float", 0)),
            Map.entry("Atmosphere", new Spec("int", 0)),
            Map.entry("Tint", new Spec("float", 1, 1, 1)),
            Map.entry("Glow", new Spec("float", 1, 1, 1)),
            Map.entry("FogTint", new Spec("float", 0, 0, 0)),
            Map.entry("FogDistance", new Spec("float", 64)),
            Map.entry("FogDensity", new Spec("float", 1)),
            Map.entry("SurfaceY", new Spec("float", 0)),
            Map.entry("Bounds", new Spec("float", 0, 0, 0, -1)),
            Map.entry("Source", new Spec("float", 0, 0)),
            Map.entry("RealmShape", new Spec("matrix4x4", ZERO_MATRIX)),
            Map.entry("RealmSource", new Spec("matrix4x4", ZERO_MATRIX)),
            Map.entry("RealmLook", new Spec("matrix4x4", ZERO_MATRIX)),
            Map.entry("RealmCount", new Spec("int", 0)),
            Map.entry("Shock", new Spec("float", 0, 0, 0, -1)),
            Map.entry("Flash", new Spec("float", 0, 0, 0, 0)));

    private static JsonObject program(String vertex, String fragment, List<String> attributes, String... samplers) {
        JsonObject json = new JsonObject();
        json.addProperty("vertex", Avernus.id(vertex).toString());
        json.addProperty("fragment", Avernus.id(fragment).toString());
        JsonArray attributeNames = new JsonArray();
        attributes.forEach(attributeNames::add);
        json.add("attributes", attributeNames);
        JsonArray inputs = new JsonArray();
        for (String name : samplers) {
            JsonObject sampler = new JsonObject();
            sampler.addProperty("name", name);
            inputs.add(sampler);
        }
        json.add("samplers", inputs);
        json.add("uniforms", new JsonArray());
        return json;
    }

    private static void blend(JsonObject program, String source, String target) {
        JsonObject blend = new JsonObject();
        blend.addProperty("func", "add");
        blend.addProperty("srcrgb", source);
        blend.addProperty("dstrgb", target);
        program.add("blend", blend);
    }

    private static void matrix(JsonArray uniforms, String name) {
        uniform(uniforms, name, "matrix4x4", IDENTITY);
    }

    private static void uniform(JsonArray uniforms, String name, String type, int... defaults) {
        JsonObject uniform = new JsonObject();
        uniform.addProperty("name", name);
        uniform.addProperty("type", type);
        uniform.addProperty("count", defaults.length);
        JsonArray values = new JsonArray();
        for (int value : defaults) values.add(value);
        uniform.add("values", values);
        uniforms.add(uniform);
    }

    @Override
    public @NotNull String getName() { return "Avernus Realm Shaders"; }
}
