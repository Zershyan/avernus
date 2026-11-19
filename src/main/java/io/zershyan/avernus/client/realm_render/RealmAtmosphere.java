package io.zershyan.avernus.client.realm_render;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** 境相特效形态：决定天空、雾、境面脉络与环境粒子的样式；序号即着色器里的 Atmosphere。 */
public enum RealmAtmosphere implements StringRepresentable {
    WISP,
    EMBER,
    FROST;

    public static final Codec<RealmAtmosphere> CODEC = StringRepresentable.fromEnum(RealmAtmosphere::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
