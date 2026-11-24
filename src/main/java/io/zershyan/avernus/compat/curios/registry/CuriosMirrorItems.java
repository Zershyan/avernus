package io.zershyan.avernus.compat.curios.registry;

import io.zershyan.avernus.api.AvernusApi;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** 把全部镜子登记为 Curios 胸饰。 */
public final class CuriosMirrorItems {
    private CuriosMirrorItems() {
    }

    public static void doRegister() {
        AvernusApi.mirrorItems().forEach(mirror -> CuriosApi.registerCurio(mirror, new ICurioItem() {}));
    }
}
