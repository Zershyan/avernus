package io.zershyan.avernus.datagen.provider;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.item.MirrorItem;
import io.zershyan.avernus.registry.item.TraitSigilItem;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import io.zershyan.avernus.registry.recipe.MirrorAspectRecipe;
import io.zershyan.avernus.registry.recipe.MirrorTraitRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, AvernusItems.UMBRAL_MIRROR.get())
                .pattern("VDV")
                .pattern("DSD")
                .pattern("ODO")
                .define('V', Items.TWISTING_VINES)
                .define('D', Items.DIAMOND)
                .define('S', Items.NETHER_STAR)
                .define('O', Items.OBSIDIAN)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                .save(output);

        aspect(output, AvernusItems.UMBRAL_MIRROR.get(), Items.ECHO_SHARD);
        aspect(output, AvernusItems.SILENT_MIRROR.get(), Items.AMETHYST_SHARD);
        aspect(output, AvernusItems.INFERNAL_MIRROR.get(), Items.BLAZE_POWDER);

        trait(output, AvernusRealmTraits.ISOLATION, Items.ENDER_PEARL);
        trait(output, AvernusRealmTraits.ANNIHILATION, Items.GUNPOWDER);
        trait(output, AvernusRealmTraits.ILLUSION, Items.GHAST_TEAR);
        trait(output, AvernusRealmTraits.REBIRTH, Items.TOTEM_OF_UNDYING);
        trait(output, AvernusRealmTraits.WEAK_WATER, Items.HEART_OF_THE_SEA);
        trait(output, AvernusRealmTraits.BOUNDLESS, Items.NETHERITE_INGOT);
        trait(output, AvernusRealmTraits.UNREALITY, Items.PHANTOM_MEMBRANE);
        trait(output, AvernusRealmTraits.ETERNITY, Items.NETHER_STAR);
        trait(output, AvernusRealmTraits.EQUALITY, Items.EMERALD);
        trait(output, AvernusRealmTraits.FREEDOM, Items.ELYTRA);
    }

    private static void aspect(RecipeOutput output, MirrorItem mirror, ItemLike material) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(mirror);
        output.accept(id.withPrefix("mirror/aspect/"), new MirrorAspectRecipe(Ingredient.of(material), new ItemStack(mirror)), null);
    }

    /** 原版材料一比一合成印记，印记在锻造台增删类型。 */
    private static void trait(RecipeOutput output, DeferredHolder<RealmTrait, RealmTrait> trait, ItemLike material) {
        TraitSigilItem sigil = AvernusItems.SIGILS.get(trait).get();
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, sigil)
                .requires(material)
                .unlockedBy("has_material", has(material))
                .save(output);
        String path = trait.getId().getPath();
        output.accept(Avernus.id("mirror/trait/add_" + path), new MirrorTraitRecipe(trait.getId(), Ingredient.of(sigil), false), null);
        output.accept(Avernus.id("mirror/trait/remove_" + path), new MirrorTraitRecipe(trait.getId(), Ingredient.of(sigil), true), null);
    }
}
