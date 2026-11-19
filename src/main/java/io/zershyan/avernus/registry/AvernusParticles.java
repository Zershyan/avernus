package io.zershyan.avernus.registry;

import com.mojang.serialization.MapCodec;
import io.zershyan.avernus.Avernus;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 领域粒子：三种境相环境粒子、碎裂火花与絮团，颜色均由生成方按境相外观传入。 */
public final class AvernusParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(Registries.PARTICLE_TYPE, Avernus.MODID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> WISP = colored("wisp");
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> EMBER = colored("ember");
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> FROST = colored("frost");
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> SPARK = colored("spark");
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> FLOCK = colored("flock");

    private AvernusParticles() {
    }

    private static DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> colored(String name) {
        return REGISTER.register(name, () -> new ParticleType<ColorParticleOption>(false) {
            @Override
            public MapCodec<ColorParticleOption> codec() {
                return ColorParticleOption.codec(this);
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
                return ColorParticleOption.streamCodec(this);
            }
        });
    }

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
