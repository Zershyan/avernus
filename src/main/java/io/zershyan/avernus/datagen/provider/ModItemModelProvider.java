package io.zershyan.avernus.datagen.provider;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Avernus.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(AvernusItems.MIRROR.get());
        AvernusRealmAspects.REGISTER.getEntries().forEach(aspect -> {
            ResourceLocation model = aspect.getId().withPrefix("item/mirror/");
            getBuilder(model.toString())
                    .parent(new ModelFile.UncheckedModelFile("item/generated"))
                    .texture("layer0", model);
        });
    }
}
