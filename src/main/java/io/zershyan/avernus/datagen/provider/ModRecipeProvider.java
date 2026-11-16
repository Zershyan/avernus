package io.zershyan.avernus.datagen.provider;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.recipe.MirrorAspectRecipe;
import io.zershyan.avernus.registry.recipe.MirrorTraitRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, AvernusItems.MIRROR.get())
                .pattern("VDV")
                .pattern("DSD")
                .pattern("ODO")
                .define('V', Items.TWISTING_VINES)
                .define('D', Items.DIAMOND)
                .define('S', Items.NETHER_STAR)
                .define('O', Items.OBSIDIAN)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                .save(output);

        aspect(output, AvernusRealmAspects.UMBRAL.getId(), Items.ECHO_SHARD);
        aspect(output, AvernusRealmAspects.SILENT.getId(), Items.AMETHYST_SHARD);
        aspect(output, AvernusRealmAspects.INFERNAL.getId(), Items.BLAZE_POWDER);

        trait(output, AvernusRealmTraits.ISOLATION.getId(), Items.ENDER_PEARL);
        trait(output, AvernusRealmTraits.ANNIHILATION.getId(), Items.GUNPOWDER);
        trait(output, AvernusRealmTraits.ILLUSION.getId(), Items.GHAST_TEAR);
        trait(output, AvernusRealmTraits.REBIRTH.getId(), Items.TOTEM_OF_UNDYING);
        trait(output, AvernusRealmTraits.WEAK_WATER.getId(), Items.HEART_OF_THE_SEA);
        trait(output, AvernusRealmTraits.BOUNDLESS.getId(), Items.NETHERITE_INGOT);
        trait(output, AvernusRealmTraits.UNREALITY.getId(), Items.PHANTOM_MEMBRANE);
        trait(output, AvernusRealmTraits.ETERNITY.getId(), Items.NETHER_STAR);
    }

    private static void aspect(RecipeOutput output, ResourceLocation aspect, ItemLike material) {
        output.accept(Avernus.id("mirror/aspect/" + aspect.getPath()), new MirrorAspectRecipe(aspect, Ingredient.of(material)), null);
    }

    private static void trait(RecipeOutput output, ResourceLocation trait, ItemLike material) {
        output.accept(Avernus.id("mirror/trait/add_" + trait.getPath()), new MirrorTraitRecipe(trait, Ingredient.of(material), false), null);
        output.accept(Avernus.id("mirror/trait/remove_" + trait.getPath()), new MirrorTraitRecipe(trait, Ingredient.of(material), true), null);
    }
}
