package io.zershyan.avernus.registry;

import com.mojang.serialization.Codec;
import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.particle.RealmParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** 领域粒子：三种境相环境粒子、碎裂火花与絮团，颜色均由生成方按境相外观传入。 */
public final class AvernusParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(Registries.PARTICLE_TYPE, Avernus.MODID);

    public static final RegistryObject<ParticleType<RealmParticleOptions>> WISP = colored("wisp");
    public static final RegistryObject<ParticleType<RealmParticleOptions>> EMBER = colored("ember");
    public static final RegistryObject<ParticleType<RealmParticleOptions>> FROST = colored("frost");
    public static final RegistryObject<ParticleType<RealmParticleOptions>> SPARK = colored("spark");
    public static final RegistryObject<ParticleType<RealmParticleOptions>> FLOCK = colored("flock");

    private AvernusParticles() {
    }

    @SuppressWarnings("unchecked")
    private static RegistryObject<ParticleType<RealmParticleOptions>> colored(String name) {
        return (RegistryObject<ParticleType<RealmParticleOptions>>) (RegistryObject<?>) REGISTER.register(name,
                () -> new ParticleType<RealmParticleOptions>(false, RealmParticleOptions.DESERIALIZER) {
                    @Override
                    public Codec<RealmParticleOptions> codec() {
                        return RealmParticleOptions.codec(this);
                    }
                });
    }

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
