package io.zershyan.avernus.client.realm_render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.zershyan.avernus.Avernus;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.Optional;

/**
 * 境相客户端外观，由资源包 {@code assets/<ns>/realm_visual/<境相路径>.json} 声明。
 * 颜色写作 {@code #RRGGBB}；未声明 glow_color 时取 tint。
 */
public record RealmVisualProfile(ResourceLocation solidTexture, ResourceLocation stillTexture, ResourceLocation flowTexture, ResourceLocation mirrorTexture,
                                 int waterColor, int tint, int fogColor, float fogDistance, RealmAtmosphere atmosphere, int glowColor) {
    private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final Codec<Integer> HEX_COLOR = Codec.STRING.comapFlatMap(
            text -> {
                try {
                    return DataResult.success(Integer.parseInt(text.startsWith("#") ? text.substring(1) : text, 16) & 0xFFFFFF);
                } catch (NumberFormatException e) {
                    return DataResult.error(() -> "非法颜色: " + text);
                }
            },
            color -> String.format("#%06X", color));

    public static final Codec<RealmVisualProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("solid_texture").forGetter(RealmVisualProfile::solidTexture),
            ResourceLocation.CODEC.optionalFieldOf("still_texture", STILL).forGetter(RealmVisualProfile::stillTexture),
            ResourceLocation.CODEC.optionalFieldOf("flow_texture", FLOW).forGetter(RealmVisualProfile::flowTexture),
            ResourceLocation.CODEC.fieldOf("mirror_texture").forGetter(RealmVisualProfile::mirrorTexture),
            HEX_COLOR.fieldOf("water_color").forGetter(RealmVisualProfile::waterColor),
            HEX_COLOR.fieldOf("tint").forGetter(RealmVisualProfile::tint),
            HEX_COLOR.fieldOf("fog_color").forGetter(RealmVisualProfile::fogColor),
            Codec.floatRange(1.0F, 1024.0F).fieldOf("fog_distance").forGetter(RealmVisualProfile::fogDistance),
            RealmAtmosphere.CODEC.optionalFieldOf("atmosphere", RealmAtmosphere.WISP).forGetter(RealmVisualProfile::atmosphere),
            HEX_COLOR.optionalFieldOf("glow_color").forGetter(profile -> Optional.of(profile.glowColor()))
    ).apply(instance, (solid, still, flow, mirror, water, tint, fog, distance, atmosphere, glow) ->
            new RealmVisualProfile(solid, still, flow, mirror, water, tint, fog, distance, atmosphere, glow.orElse(tint))));

    /** 未声明外观的境相使用的回退。 */
    public static final RealmVisualProfile FALLBACK = new RealmVisualProfile(
            Avernus.id("block/realm/umbral"), STILL, FLOW, Avernus.id("textures/entity/mirror/umbral.png"),
            0x3F76E4, 0xB0A0FF, 0x201830, 64.0F, RealmAtmosphere.WISP, 0xB0A0FF);

    public static Vector3f rgb(int color) {
        return new Vector3f((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
    }
}
