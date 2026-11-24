package io.zershyan.avernus.datagen.provider;

import com.google.gson.JsonObject;
import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.AvernusRecipes;
import io.zershyan.avernus.registry.item.MirrorItem;
import io.zershyan.avernus.registry.item.TraitSigilItem;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> output) {
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

    private static void aspect(Consumer<FinishedRecipe> output, MirrorItem mirror, ItemLike material) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(mirror).withPrefix("mirror/aspect/");
        Ingredient ingredient = Ingredient.of(material);
        output.accept(new SimpleRecipe(id, AvernusRecipes.MIRROR_ASPECT.get(), json -> {
            json.add("material", ingredient.toJson());
            json.add("result", item(BuiltInRegistries.ITEM.getKey(mirror)));
        }));
    }

    /** 原版材料一比一合成印记，印记在锻造台增删境印。 */
    private static void trait(Consumer<FinishedRecipe> output, RegistryObject<RealmTrait> trait, ItemLike material) {
        TraitSigilItem sigil = AvernusItems.SIGILS.get(trait).get();
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, sigil)
                .requires(material)
                .unlockedBy("has_material", has(material))
                .save(output);
        Ingredient ingredient = Ingredient.of(sigil);
        String path = trait.getId().getPath();
        for (boolean remove : new boolean[]{false, true}) {
            ResourceLocation id = Avernus.id("mirror/trait/" + (remove ? "remove_" : "add_") + path);
            output.accept(new SimpleRecipe(id, AvernusRecipes.MIRROR_TRAIT.get(), json -> {
                json.addProperty("trait", trait.getId().toString());
                json.add("material", ingredient.toJson());
                if (remove) json.addProperty("remove", true);
            }));
        }
    }

    private static JsonObject item(ResourceLocation id) {
        JsonObject json = new JsonObject();
        json.addProperty("item", id.toString());
        return json;
    }

    private record SimpleRecipe(ResourceLocation id, RecipeSerializer<?> type, Consumer<JsonObject> data) implements FinishedRecipe {
        @Override
        public void serializeRecipeData(JsonObject json) {
            data.accept(json);
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return type;
        }

        @Override
        public @Nullable JsonObject serializeAdvancement() {
            return null;
        }

        @Override
        public @Nullable ResourceLocation getAdvancementId() {
            return null;
        }
    }
}
