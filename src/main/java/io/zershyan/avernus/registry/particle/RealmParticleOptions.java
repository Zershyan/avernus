package io.zershyan.avernus.registry.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

/** 1.20.1 领域粒子选项：颜色与透明度随每一颗粒子传入，类型由注册的粒子决定。 */
public record RealmParticleOptions(ParticleType<RealmParticleOptions> type, int rgb, float alpha) implements ParticleOptions {
    public static final ParticleOptions.Deserializer<RealmParticleOptions> DESERIALIZER =
            new ParticleOptions.Deserializer<>() {
                @Override
                public RealmParticleOptions fromCommand(ParticleType<RealmParticleOptions> type, StringReader reader) {
                    return new RealmParticleOptions(type, reader.readInt(), 1.0F);
                }

                @Override
                public RealmParticleOptions fromNetwork(ParticleType<RealmParticleOptions> type, FriendlyByteBuf buf) {
                    return new RealmParticleOptions(type, buf.readInt(), buf.readFloat());
                }
            };

    public static Codec<RealmParticleOptions> codec(ParticleType<RealmParticleOptions> type) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("color").forGetter(RealmParticleOptions::rgb),
                Codec.FLOAT.optionalFieldOf("alpha", 1.0F).forGetter(RealmParticleOptions::alpha)
        ).apply(instance, (rgb, alpha) -> new RealmParticleOptions(type, rgb, alpha)));
    }

    @Override
    public ParticleType<?> getType() {
        return type;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeInt(rgb);
        buffer.writeFloat(alpha);
    }

    @Override
    public String writeToString() {
        return rgb + " " + alpha;
    }
}
