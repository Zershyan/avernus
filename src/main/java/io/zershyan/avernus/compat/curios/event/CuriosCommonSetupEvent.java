package io.zershyan.avernus.compat.curios.event;

import io.zershyan.avernus.compat.curios.registry.CuriosMirrorItems;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** Curios 公共初始化：把全部镜子登记为胸饰。 */
public final class CuriosCommonSetupEvent {
    private CuriosCommonSetupEvent() {
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CuriosMirrorItems::doRegister);
    }
}
