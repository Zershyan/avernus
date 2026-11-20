package io.zershyan.avernus.datagen.provider;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.AvernusItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/** 镜子可佩戴于 Curios 胸饰槽；标签按 ID 声明，不链接 Curios 类型。 */
public class ModItemTagsProvider extends ItemTagsProvider {
    private static final TagKey<Item> CURIOS_BODY = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "body"));

    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
        super(output, lookup, CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()), Avernus.MODID, helper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        AvernusItems.MIRRORS_BY_ASPECT.values().forEach(mirror -> tag(AvernusItems.MIRRORS).add(mirror.get()));
        tag(CURIOS_BODY).addTag(AvernusItems.MIRRORS);
    }
}
