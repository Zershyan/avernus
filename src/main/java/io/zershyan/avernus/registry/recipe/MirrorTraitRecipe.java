package io.zershyan.avernus.registry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.zershyan.avernus.api.realm.MirrorForm;
import io.zershyan.avernus.api.recipe.MirrorTraitRecipeView;
import io.zershyan.avernus.realm.MirrorForms;
import io.zershyan.avernus.registry.AvernusDataComponents;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.AvernusRecipes;
import io.zershyan.avernus.registry.data_component.MirrorTraits;
import io.zershyan.avernus.registry.item.MirrorItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/** 锻造台增删境印：添加时模板槽留空，删除时模板槽放羽毛；附加槽为该境印的印记。 */
public class MirrorTraitRecipe implements MirrorTraitRecipeView {
    private final ResourceLocation trait;
    private final Ingredient material;
    private final boolean remove;

    public MirrorTraitRecipe(ResourceLocation trait, Ingredient material, boolean remove) {
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
    public boolean matches(SmithingRecipeInput input, @NotNull Level level) {
        return isTemplateIngredient(input.template()) && material.test(input.addition()) && next(input.base()).isPresent();
    }

    @Override
    public @NotNull ItemStack assemble(SmithingRecipeInput input, HolderLookup.@NotNull Provider registries) {
        return next(input.base()).map(traits -> {
            ItemStack result = input.base().copyWithCount(1);
            result.set(AvernusDataComponents.MIRROR_TRAITS.get(), traits);
            return result;
        }).orElse(ItemStack.EMPTY);
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
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
        return material.hasNoItems();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return AvernusRecipes.MIRROR_TRAIT.get();
    }

    public static final MapCodec<MirrorTraitRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("trait").forGetter(MirrorTraitRecipe::trait),
            Ingredient.CODEC_NONEMPTY.fieldOf("material").forGetter(MirrorTraitRecipe::material),
            Codec.BOOL.optionalFieldOf("remove", false).forGetter(MirrorTraitRecipe::remove)
    ).apply(instance, MirrorTraitRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MirrorTraitRecipe> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, MirrorTraitRecipe::trait,
            Ingredient.CONTENTS_STREAM_CODEC, MirrorTraitRecipe::material,
            ByteBufCodecs.BOOL, MirrorTraitRecipe::remove,
            MirrorTraitRecipe::new);
}
