package io.zershyan.avernus.datagen.provider;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.AvernusItems;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    private static final ResourceLocation BUILTIN_ENTITY = ResourceLocation.withDefaultNamespace("builtin/entity");

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Avernus.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        AvernusItems.MIRRORS_BY_ASPECT.values().forEach(mirror -> mirror(mirror.getId()));
        AvernusItems.SIGILS.values().forEach(sigil -> basicItem(sigil.getId()));
    }

    /** 镜子由物品渲染器画立体模型，这里只给出贴图粒子与各视角变换。 */
    private void mirror(ResourceLocation item) {
        ItemModelBuilder model = getBuilder(item.toString()).parent(new ModelFile.UncheckedModelFile(BUILTIN_ENTITY))
                .texture("particle", ResourceLocation.withDefaultNamespace("block/glass"))
                .guiLight(BlockModel.GuiLight.FRONT);
        var transforms = model.transforms();
        transforms.transform(ItemDisplayContext.GUI).rotation(0, -20, 0).scale(0.95F);
        transforms.transform(ItemDisplayContext.GROUND).translation(0, 2, 0).scale(0.5F);
        transforms.transform(ItemDisplayContext.HEAD).rotation(0, 180, 0).translation(0, 13, 7);
        transforms.transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).translation(0, 3, 1).scale(0.55F);
        transforms.transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).translation(0, 3, 1).scale(0.55F);
        transforms.transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, -90, 25).translation(1.13F, 3.2F, 1.13F).scale(0.68F);
        transforms.transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 90, -25).translation(1.13F, 3.2F, 1.13F).scale(0.68F);
        transforms.transform(ItemDisplayContext.FIXED).rotation(0, 180, 0);
    }
}
