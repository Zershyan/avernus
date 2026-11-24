package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 内置境相条目；基础属性由境相效果服务读取配置并经事件修正。 */
public class AvernusRealmAspects {
    public static final DeferredRegister<RealmAspect> REGISTER =
            DeferredRegister.create(AvernusRegistries.REALM_ASPECT, Avernus.MODID);

    public static final DeferredHolder<RealmAspect, RealmAspect> UMBRAL = REGISTER.register("umbral", RealmAspect::new);
    public static final DeferredHolder<RealmAspect, RealmAspect> SILENT = REGISTER.register("silent", RealmAspect::new);
    public static final DeferredHolder<RealmAspect, RealmAspect> INFERNAL = REGISTER.register("infernal", RealmAspect::new);

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
