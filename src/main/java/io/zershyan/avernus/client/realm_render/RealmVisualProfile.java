package io.zershyan.avernus.client.realm_render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.zershyan.avernus.Avernus;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * 境相客户端外观，由资源包 {@code assets/<ns>/realm_visual/<境相路径>.json} 声明。
 * 颜色写作 {@code #RRGGBB}。
 */
public record RealmVisualProfile(ResourceLocation solidTexture, ResourceLocation mirrorTexture,
                                 int waterColor, int tint, int fogColor, float fogDistance) {
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
            ResourceLocation.CODEC.fieldOf("mirror_texture").forGetter(RealmVisualProfile::mirrorTexture),
            HEX_COLOR.fieldOf("water_color").forGetter(RealmVisualProfile::waterColor),
            HEX_COLOR.fieldOf("tint").forGetter(RealmVisualProfile::tint),
            HEX_COLOR.fieldOf("fog_color").forGetter(RealmVisualProfile::fogColor),
            Codec.floatRange(1.0F, 1024.0F).fieldOf("fog_distance").forGetter(RealmVisualProfile::fogDistance)
    ).apply(instance, RealmVisualProfile::new));

    /** 未声明外观的境相使用的回退。 */
    public static final RealmVisualProfile FALLBACK = new RealmVisualProfile(
            Avernus.id("block/realm/umbral"), Avernus.id("textures/item/mirror.png"),
            0x3F76E4, 0xB0A0FF, 0x201830, 64.0F);

    public static Vector3f rgb(int color) {
        return new Vector3f((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
    }
}
