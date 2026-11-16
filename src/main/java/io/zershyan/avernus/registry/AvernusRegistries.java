package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/** 自定义注册表声明：境相与类型，第三方可通过对应注册表键注册条目。 */
public class AvernusRegistries {
    public static final ResourceKey<Registry<RealmAspect>> REALM_ASPECT_KEY =
            ResourceKey.createRegistryKey(Avernus.id("realm_aspect"));
    public static final ResourceKey<Registry<RealmTrait>> REALM_TRAIT_KEY =
            ResourceKey.createRegistryKey(Avernus.id("realm_trait"));

    public static final Registry<RealmAspect> REALM_ASPECT = new RegistryBuilder<>(REALM_ASPECT_KEY).sync(true).create();
    public static final Registry<RealmTrait> REALM_TRAIT = new RegistryBuilder<>(REALM_TRAIT_KEY).sync(true).create();

    private static void createRegistries(NewRegistryEvent event) {
        event.register(REALM_ASPECT);
        event.register(REALM_TRAIT);
    }

    public static void doRegister(IEventBus modBus) {
        modBus.addListener(AvernusRegistries::createRegistries);
    }
}
