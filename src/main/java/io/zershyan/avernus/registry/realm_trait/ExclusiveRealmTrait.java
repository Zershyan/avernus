package io.zershyan.avernus.registry.realm_trait;

import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;

import java.util.function.Supplier;

/** 与指定类型互斥的类型。 */
public class ExclusiveRealmTrait extends RealmTrait {
    private final Supplier<? extends RealmTrait> rival;

    public ExclusiveRealmTrait(Supplier<? extends RealmTrait> rival) {
        this.rival = rival;
    }

    @Override
    public boolean conflictsWith(RealmTrait other) {
        return other == rival.get();
    }
}
