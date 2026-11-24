package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

/** 内置境相条目；基础属性由境相效果服务读取配置并经事件修正。 */
public class AvernusRealmAspects {
    public static final DeferredRegister<RealmAspect> REGISTER =
            DeferredRegister.create(AvernusRegistries.REALM_ASPECT_KEY, Avernus.MODID);

    public static final RegistryObject<RealmAspect> UMBRAL = REGISTER.register("umbral", RealmAspect::new);
    public static final RegistryObject<RealmAspect> SILENT = REGISTER.register("silent", RealmAspect::new);
    public static final RegistryObject<RealmAspect> INFERNAL = REGISTER.register("infernal", RealmAspect::new);

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
