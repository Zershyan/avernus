package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.datagen.lang.AvernusKeyLang;
import io.zershyan.avernus.registry.item.MirrorItem;
import io.zershyan.avernus.registry.item.TraitSigilItem;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

public class AvernusItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(Avernus.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Avernus.MODID);

    /** 全部境相的镜子：配方中心槽、冷却与胸饰槽按此标签识别。 */
    public static final TagKey<Item> MIRRORS = TagKey.create(Registries.ITEM, Avernus.id("mirrors"));

    public static final Map<RegistryObject<RealmAspect, RealmAspect>, RegistryObject<MirrorItem>> MIRRORS_BY_ASPECT = new LinkedHashMap<>();
    public static final Map<RegistryObject<RealmTrait, RealmTrait>, RegistryObject<TraitSigilItem>> SIGILS = new LinkedHashMap<>();

    public static final RegistryObject<MirrorItem> UMBRAL_MIRROR = mirror(AvernusRealmAspects.UMBRAL);
    public static final RegistryObject<MirrorItem> INFERNAL_MIRROR = mirror(AvernusRealmAspects.INFERNAL);
    public static final RegistryObject<MirrorItem> SILENT_MIRROR = mirror(AvernusRealmAspects.SILENT);

    static {
        AvernusRealmTraits.REGISTER.getEntries().forEach(AvernusItems::sigil);
    }

    public static final RegistryObject<CreativeModeTab, CreativeModeTab> TAB = TABS.register("avernus", () -> CreativeModeTab.builder()
            .title(AvernusKeyLang.CreativeTab)
            .icon(() -> UMBRAL_MIRROR.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                MIRRORS_BY_ASPECT.values().forEach(output::accept);
                SIGILS.values().forEach(output::accept);
            })
            .build());

    private static RegistryObject<MirrorItem> mirror(RegistryObject<RealmAspect, RealmAspect> aspect) {
        RegistryObject<MirrorItem> item = REGISTER.register(aspect.getId().getPath() + "_mirror", () -> new MirrorItem(aspect));
        MIRRORS_BY_ASPECT.put(aspect, item);
        return item;
    }

    @SuppressWarnings("unchecked")
    private static void sigil(RegistryObject<RealmTrait, ? extends RealmTrait> trait) {
        var holder = (RegistryObject<RealmTrait, RealmTrait>) trait;
        SIGILS.put(holder, REGISTER.register(holder.getId().getPath() + "_sigil", () -> new TraitSigilItem(holder)));
    }

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
        TABS.register(modBus);
    }
}
