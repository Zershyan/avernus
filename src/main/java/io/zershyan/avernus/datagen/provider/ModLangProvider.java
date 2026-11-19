package io.zershyan.avernus.datagen.provider;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.datagen.lang.AvernusLang;
import net.minecraft.data.PackOutput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.function.Function;

public class ModLangProvider extends LanguageProvider {
    private final Function<AvernusLang.Lang, String> selector;

    private ModLangProvider(PackOutput output, String locale, Function<AvernusLang.Lang, String> selector) {
        super(output, Avernus.MODID, locale);
        this.selector = selector;
    }

    public static ModLangProvider runEnUs(PackOutput output) {
        return new ModLangProvider(output, "en_us", AvernusLang.Lang::enUs);
    }

    public static ModLangProvider runZhCn(PackOutput output) {
        return new ModLangProvider(output, "zh_cn", AvernusLang.Lang::zhCn);
    }

    @Override
    protected void addTranslations() {
        AvernusLang.getAllLang().forEach(entry -> {
            String text = selector.apply(entry.lang());
            switch (entry.key()) {
                case Item item -> add(item, text);
                case EntityType<?> type -> add(type, text);
                case String key -> add(key, text);
                default -> throw new IllegalArgumentException("Unknown lang key type: " + entry.key().getClass());
            }
        });
    }
}
