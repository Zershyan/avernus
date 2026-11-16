package io.zershyan.avernus.client.mirror;

import io.zershyan.avernus.registry.item.MirrorItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** 按镜子境相选择模型，未提供专属模型的境相回退通用镜。 */
public class MirrorItemOverrides extends ItemOverrides {
    private final Map<ResourceLocation, BakedModel> aspectModels;

    public MirrorItemOverrides(Map<ResourceLocation, BakedModel> aspectModels) {
        this.aspectModels = Map.copyOf(aspectModels);
    }

    @Override
    public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        return aspectModels.getOrDefault(MirrorItem.getForm(stack).aspect(), model);
    }
}
