package io.zershyan.avernus.datagen;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.datagen.provider.CuriosSlotProvider;
import io.zershyan.avernus.datagen.provider.ModItemModelProvider;
import io.zershyan.avernus.datagen.provider.ModItemTagsProvider;
import io.zershyan.avernus.datagen.provider.ModLangProvider;
import io.zershyan.avernus.datagen.provider.ModRecipeProvider;
import io.zershyan.avernus.datagen.provider.PackMetadataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = Avernus.MODID)
public class DataGeneratorHandler {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        ExistingFileHelper helper = event.getExistingFileHelper();
        PackOutput packOutput = event.getGenerator().getPackOutput();

        event.createProvider(ModLangProvider::runEnUs);
        event.createProvider(ModLangProvider::runZhCn);
        event.createProvider(PackMetadataProvider::new);
        event.addProvider(new ModItemModelProvider(packOutput, helper));
        event.addProvider(new ModRecipeProvider(packOutput, event.getLookupProvider()));
        event.addProvider(new ModItemTagsProvider(packOutput, event.getLookupProvider(), helper));
        event.addProvider(new CuriosSlotProvider(packOutput));
    }
}
