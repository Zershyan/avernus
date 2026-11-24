package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

/** 自定义注册表声明：境相与境印，第三方可通过对应注册表键注册条目。 */
public class AvernusRegistries {
    public static final ResourceKey<Registry<RealmAspect>> REALM_ASPECT_KEY =
            ResourceKey.createRegistryKey(Avernus.id("realm_aspect"));
    public static final ResourceKey<Registry<RealmTrait>> REALM_TRAIT_KEY =
            ResourceKey.createRegistryKey(Avernus.id("realm_trait"));

    public static Supplier<IForgeRegistry<RealmAspect>> REALM_ASPECT;
    public static Supplier<IForgeRegistry<RealmTrait>> REALM_TRAIT;

    private static void createRegistries(NewRegistryEvent event) {
        REALM_ASPECT = event.create(new RegistryBuilder<RealmAspect>().setName(Avernus.id("realm_aspect")));
        REALM_TRAIT = event.create(new RegistryBuilder<RealmTrait>().setName(Avernus.id("realm_trait")));
    }

    public static void doRegister(IEventBus modBus) {
        modBus.addListener(AvernusRegistries::createRegistries);
    }
}
