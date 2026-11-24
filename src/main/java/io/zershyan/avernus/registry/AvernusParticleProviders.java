package io.zershyan.avernus.registry;

import io.zershyan.avernus.client.particle.RealmParticle;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** 领域粒子提供器的客户端注册；仅在客户端调用。 */
public final class AvernusParticleProviders {
    private AvernusParticleProviders() {
    }

    public static void doRegister(IEventBus modBus) {
        modBus.addListener(RegisterParticleProvidersEvent.class, AvernusParticleProviders::register);
    }

    private static void register(RegisterParticleProvidersEvent event) {
        provider(event, AvernusParticles.WISP.get(), RealmParticle.Kind.WISP);
        provider(event, AvernusParticles.EMBER.get(), RealmParticle.Kind.EMBER);
        provider(event, AvernusParticles.FROST.get(), RealmParticle.Kind.FROST);
        provider(event, AvernusParticles.SPARK.get(), RealmParticle.Kind.SPARK);
        provider(event, AvernusParticles.FLOCK.get(), RealmParticle.Kind.FLOCK);
    }

    private static void provider(RegisterParticleProvidersEvent event, ParticleType<ColorParticleOption> type, RealmParticle.Kind kind) {
        event.registerSpriteSet(type, sprites -> new RealmParticle.Provider(kind, sprites));
    }
}
