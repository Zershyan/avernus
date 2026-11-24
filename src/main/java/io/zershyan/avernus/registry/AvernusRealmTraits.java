package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.realm_trait.ExclusiveRealmTrait;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

/** 内置境印条目；保留既有注册 ID 与第三方扩展入口。 */
public class AvernusRealmTraits {
    public static final DeferredRegister<RealmTrait> REGISTER =
            DeferredRegister.create(AvernusRegistries.REALM_TRAIT, Avernus.MODID);

    public static final RegistryObject<RealmTrait, RealmTrait> ISOLATION = REGISTER.register("isolation", RealmTrait::new);
    public static final RegistryObject<RealmTrait, RealmTrait> ANNIHILATION = REGISTER.register("annihilation", RealmTrait::new);
    public static final RegistryObject<RealmTrait, RealmTrait> ILLUSION = REGISTER.register("illusion", RealmTrait::new);
    public static final RegistryObject<RealmTrait, RealmTrait> REBIRTH = REGISTER.register("rebirth", RealmTrait::new);
    public static final RegistryObject<RealmTrait, RealmTrait> WEAK_WATER;
    public static final RegistryObject<RealmTrait, RealmTrait> BOUNDLESS;
    public static final RegistryObject<RealmTrait, RealmTrait> UNREALITY = REGISTER.register("unreality", RealmTrait::new);
    public static final RegistryObject<RealmTrait, RealmTrait> ETERNITY = REGISTER.register("eternity", RealmTrait::new);
    public static final RegistryObject<RealmTrait, RealmTrait> EQUALITY = REGISTER.register("equality", RealmTrait::new);
    public static final RegistryObject<RealmTrait, RealmTrait> FREEDOM = REGISTER.register("freedom", RealmTrait::new);

    static {
        WEAK_WATER = REGISTER.register("weak_water", () -> new ExclusiveRealmTrait(AvernusRealmTraits.BOUNDLESS));
        BOUNDLESS = REGISTER.register("boundless", () -> new ExclusiveRealmTrait(AvernusRealmTraits.WEAK_WATER));
    }

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
