package io.zershyan.avernus.registry.recipe;

import com.google.gson.JsonObject;
import io.zershyan.avernus.api.realm.MirrorForm;
import io.zershyan.avernus.api.recipe.MirrorTraitRecipeView;
import io.zershyan.avernus.realm.MirrorForms;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.AvernusRecipes;
import io.zershyan.avernus.registry.data_component.MirrorTraits;
import io.zershyan.avernus.registry.item.MirrorItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/** 锻造台增删境印：添加时模板槽留空，删除时模板槽放羽毛；附加槽为该境印的印记。 */
public class MirrorTraitRecipe implements MirrorTraitRecipeView, SmithingRecipe {
    private final ResourceLocation id;
    private final ResourceLocation trait;
    private final Ingredient material;
    private final boolean remove;

    public MirrorTraitRecipe(ResourceLocation id, ResourceLocation trait, Ingredient material, boolean remove) {
        this.id = id;
        this.trait = trait;
        this.material = material;
        this.remove = remove;
    }

    @Override
    public ResourceLocation trait() {
        return trait;
    }

    @Override
    public Ingredient material() {
        return material;
    }

    @Override
    public boolean remove() {
        return remove;
    }

    private Optional<MirrorTraits> next(ItemStack stack) {
        if (!(stack.getItem() instanceof MirrorItem mirror)) return Optional.empty();
        MirrorTraits traits = MirrorItem.getTraits(stack);
        if (traits.has(trait) != remove) return Optional.empty();
        MirrorTraits next = remove ? traits.without(trait) : traits.with(trait);
        return MirrorForms.resolve(new MirrorForm(Objects.requireNonNull(mirror.aspect().getId()), next.ids())).map(resolved -> next);
    }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        return isTemplateIngredient(container.getItem(0)) && isBaseIngredient(container.getItem(1))
                && isAdditionIngredient(container.getItem(2)) && next(container.getItem(1)).isPresent();
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess access) {
        return next(container.getItem(1)).map(traits -> {
            ItemStack result = container.getItem(1).copyWithCount(1);
            MirrorTraits.write(result, traits);
            return result;
        }).orElse(ItemStack.EMPTY);
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess access) {
        return new ItemStack(AvernusItems.UMBRAL_MIRROR.get());
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return remove ? stack.is(Items.FEATHER) : stack.isEmpty();
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.is(AvernusItems.MIRRORS);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return material.test(stack);
    }

    /** 添加配方的模板槽为空，不能按默认的“存在空原料”判为不完整。 */
    @Override
    public boolean isIncomplete() {
        return material.isEmpty();
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return AvernusRecipes.MIRROR_TRAIT.get();
    }

    public static class Serializer implements RecipeSerializer<MirrorTraitRecipe> {
        @Override
        public MirrorTraitRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new MirrorTraitRecipe(id, new ResourceLocation(GsonHelper.getAsString(json, "trait")),
                    Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "material")), GsonHelper.getAsBoolean(json, "remove", false));
        }

        @Override
        public MirrorTraitRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new MirrorTraitRecipe(id, buf.readResourceLocation(), Ingredient.fromNetwork(buf), buf.readBoolean());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, MirrorTraitRecipe recipe) {
            buf.writeResourceLocation(recipe.trait);
            recipe.material.toNetwork(buf);
            buf.writeBoolean(recipe.remove);
        }
    }
}
