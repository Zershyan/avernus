package io.zershyan.avernus.registry;

import io.zershyan.avernus.client.mirror.MirrorItemOverrides;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

import java.util.HashMap;
import java.util.Map;

/**
 * 镜子境相模型：境相 {@code ns:path} 对应模型 {@code ns:item/mirror/path}，
 * 第三方境相只需在资源包中提供该模型即可获得专属外观。
 */
public class AvernusItemModels {
    private static final String MODEL_DIR = "models/item/mirror/";

    private static ResourceLocation modelOf(ResourceLocation aspect) {
        return aspect.withPrefix("item/mirror/");
    }

    private static void registerAdditional(ModelEvent.RegisterAdditional event) {
        Minecraft.getInstance().getResourceManager()
                .listResources(MODEL_DIR.substring(0, MODEL_DIR.length() - 1), file -> file.getPath().endsWith(".json"))
                .keySet()
                .forEach(file -> event.register(ModelResourceLocation.standalone(
                        file.withPath(path -> path.substring("models/".length(), path.length() - ".json".length())))));
    }

    private static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        ModelResourceLocation baseKey = ModelResourceLocation.inventory(AvernusItems.MIRROR.getId());
        BakedModel base = models.get(baseKey);
        if (base == null) return;
        Map<ResourceLocation, BakedModel> aspectModels = new HashMap<>();
        AvernusRegistries.REALM_ASPECT.keySet().forEach(aspect -> {
            BakedModel model = models.get(ModelResourceLocation.standalone(modelOf(aspect)));
            if (model != null) aspectModels.put(aspect, model);
        });
        ItemOverrides overrides = new MirrorItemOverrides(aspectModels);
        models.put(baseKey, new BakedModelWrapper<>(base) {
            @Override
            public ItemOverrides getOverrides() {
                return overrides;
            }
        });
    }

    public static void doRegister(IEventBus modBus) {
        modBus.addListener(AvernusItemModels::registerAdditional);
        modBus.addListener(AvernusItemModels::modifyBakingResult);
    }
}
