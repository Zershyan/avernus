package io.zershyan.avernus.datagen;

import io.zershyan.avernus.datagen.provider.CuriosSlotProvider;
import io.zershyan.avernus.datagen.provider.ModItemModelProvider;
import io.zershyan.avernus.datagen.provider.ModItemTagsProvider;
import io.zershyan.avernus.datagen.provider.ModLangProvider;
import io.zershyan.avernus.datagen.provider.ModParticleProvider;
import io.zershyan.avernus.datagen.provider.ModRecipeProvider;
import io.zershyan.avernus.datagen.provider.ModSoundProvider;
import io.zershyan.avernus.datagen.provider.PackMetadataProvider;
import io.zershyan.avernus.datagen.provider.RealmShaderProvider;
import io.zershyan.avernus.datagen.provider.RealmVisualProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class DataGeneratorHandler {
    public static void gatherData(GatherDataEvent event) {
        ExistingFileHelper helper = event.getExistingFileHelper();
        PackOutput packOutput = event.getGenerator().getPackOutput();

        event.createProvider(ModLangProvider::runEnUs);
        event.createProvider(ModLangProvider::runZhCn);
        event.createProvider(PackMetadataProvider::new);
        event.addProvider(new ModItemModelProvider(packOutput, helper));
        if (event.includeClient()) {
            event.addProvider(new RealmShaderProvider(packOutput));
            event.addProvider(new RealmVisualProvider(packOutput));
            event.addProvider(new ModParticleProvider(packOutput, helper));
            event.addProvider(new ModSoundProvider(packOutput, helper));
        }
        event.addProvider(new ModRecipeProvider(packOutput, event.getLookupProvider()));
        event.addProvider(new ModItemTagsProvider(packOutput, event.getLookupProvider(), helper));
        event.addProvider(new CuriosSlotProvider(packOutput));
    }
}
