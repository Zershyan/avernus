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
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

public class DataGeneratorHandler {
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), ModLangProvider.runEnUs(output));
        generator.addProvider(event.includeClient(), ModLangProvider.runZhCn(output));
        generator.addProvider(event.includeClient(), new PackMetadataProvider(output));
        generator.addProvider(event.includeClient(), new ModItemModelProvider(output, helper));
        if (event.includeClient()) {
            generator.addProvider(true, new RealmShaderProvider(output));
            generator.addProvider(true, new RealmVisualProvider(output));
            generator.addProvider(true, new ModParticleProvider(output, helper));
            generator.addProvider(true, new ModSoundProvider(output, helper));
        }
        generator.addProvider(event.includeServer(), new ModRecipeProvider(output));
        generator.addProvider(event.includeServer(), new ModItemTagsProvider(output, event.getLookupProvider(), helper));
        generator.addProvider(event.includeServer(), new CuriosSlotProvider(output));
    }
}
