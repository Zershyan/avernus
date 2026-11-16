package io.zershyan.avernus.datagen.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** 语言数据基类，聚合各子类并统一对外提供。 */
public abstract class AvernusLang {
    protected final List<FinalEntry<?>> entries = new ArrayList<>();

    public static List<FinalEntry<?>> getAllLang() {
        List<FinalEntry<?>> all = new ArrayList<>();
        Stream.of(new AvernusKeyLang(), new AvernusConfigLang(), new AvernusRegistryLang())
                .map(AvernusLang::initLang)
                .forEach(all::addAll);
        return all;
    }

    protected <T> void addFinalEntry(T key, String enUs, String zhCn) {
        entries.add(new FinalEntry<>(key, new Lang(enUs, zhCn)));
    }

    private List<FinalEntry<?>> initLang() {
        init();
        return entries;
    }

    protected abstract void init();

    public record Lang(String enUs, String zhCn) {
    }

    public record FinalEntry<T>(T key, Lang lang) {
    }
}
