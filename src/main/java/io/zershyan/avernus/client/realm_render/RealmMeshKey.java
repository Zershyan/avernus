package io.zershyan.avernus.client.realm_render;

import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmTerrain;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import net.minecraft.world.level.block.state.BlockState;

/** 同一表层形态可在不同半径、波面与领域之间复用。 */
public record RealmMeshKey(int layerY, boolean weakWater, boolean boundless, RealmAspect aspect) {
    public static RealmMeshKey of(Realm realm) {
        return new RealmMeshKey(realm.height() - 1, realm.hasTrait(AvernusRealmTraits.WEAK_WATER.get()),
                realm.hasTrait(AvernusRealmTraits.BOUNDLESS.get()), realm.aspect());
    }

    public RealmTerrain.Kind kind(BlockState real) {
        return RealmTerrain.classify(layerY + 1, weakWater, boundless, real, layerY);
    }
}
