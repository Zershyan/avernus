package io.zershyan.avernus.registry.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.AvernusRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/** 工作台切换境相：中心任意镜子，周围八格同种材料，产出另一境相的镜子并保留类型、名字等全部组件。 */
public class MirrorAspectRecipe extends ShapedRecipe {
    private static final int CENTER = 4;

    private final Ingredient material;

    public MirrorAspectRecipe(Ingredient material, ItemStack result) {
        super("mirror_aspect", CraftingBookCategory.EQUIPMENT, ShapedRecipePattern.of(
                Map.of('M', material, 'X', Ingredient.of(AvernusItems.MIRRORS)), "MMM", "MXM", "MMM"), result);
        this.material = material;
    }

    public Ingredient material() {
        return material;
    }

    private ItemStack result() {
        return getResultItem(null);
    }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        return super.matches(input, level) && !input.getItem(CENTER).is(result().getItem());
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.@NotNull Provider registries) {
        return input.getItem(CENTER).transmuteCopy(result().getItem(), 1);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return AvernusRecipes.MIRROR_ASPECT.get();
    }

    public static final MapCodec<MirrorAspectRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("material").forGetter(MirrorAspectRecipe::material),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(MirrorAspectRecipe::result)
    ).apply(instance, MirrorAspectRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MirrorAspectRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, MirrorAspectRecipe::material,
            ItemStack.STREAM_CODEC, MirrorAspectRecipe::result,
            MirrorAspectRecipe::new);
}
