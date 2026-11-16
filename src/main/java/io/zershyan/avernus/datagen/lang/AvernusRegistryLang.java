package io.zershyan.avernus.datagen.lang;

import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.neoforged.neoforge.registries.DeferredHolder;

public class AvernusRegistryLang extends AvernusLang {

    private void aspect(DeferredHolder<RealmAspect, ?> holder, String enUs, String zhCn) {
        addFinalEntry(RealmAspect.languageKey(holder.getId()), enUs, zhCn);
    }

    private void trait(DeferredHolder<RealmTrait, ?> holder, String enUs, String zhCn) {
        addFinalEntry(RealmTrait.languageKey(holder.getId()), enUs, zhCn);
    }

    @Override
    protected void init() {
        addFinalEntry(AvernusItems.MIRROR.get(), "Mirror of Avernus", "阿维尔诺之镜");

        aspect(AvernusRealmAspects.UMBRAL, "Umbral", "幽冥");
        aspect(AvernusRealmAspects.SILENT, "Silent", "寂静");
        aspect(AvernusRealmAspects.INFERNAL, "Infernal", "炼狱");

        trait(AvernusRealmTraits.ISOLATION, "Isolation", "隔绝");
        trait(AvernusRealmTraits.ANNIHILATION, "Annihilation", "湮没");
        trait(AvernusRealmTraits.ILLUSION, "Illusion", "幻化");
        trait(AvernusRealmTraits.REBIRTH, "Rebirth", "往生");
        trait(AvernusRealmTraits.WEAK_WATER, "Weak Water", "弱水");
        trait(AvernusRealmTraits.BOUNDLESS, "Boundless", "无垠");
        trait(AvernusRealmTraits.UNREALITY, "Unreality", "虚妄");
        trait(AvernusRealmTraits.ETERNITY, "Eternity", "永恒");
    }
}
