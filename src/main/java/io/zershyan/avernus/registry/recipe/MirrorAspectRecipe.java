package io.zershyan.avernus.registry.recipe;

import com.google.gson.JsonObject;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.AvernusRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/** 工作台切换境相：中心任意镜子，周围八格同种材料，产出另一境相的镜子并保留境印、名字等全部 NBT。 */
public class MirrorAspectRecipe extends ShapedRecipe {
    private static final int CENTER = 4;

    private final Ingredient material;

    public MirrorAspectRecipe(ResourceLocation id, Ingredient material, ItemStack result) {
        super(id, "mirror_aspect", CraftingBookCategory.EQUIPMENT, 3, 3, NonNullList.of(Ingredient.EMPTY,
                material, material, material,
                material, Ingredient.of(AvernusItems.MIRRORS), material,
                material, material, material), result);
        this.material = material;
    }

    public Ingredient material() {
        return material;
    }

    @Override
    public boolean matches(@NotNull CraftingContainer container, @NotNull Level level) {
        return super.matches(container, level) && !container.getItem(CENTER).is(getResultItem(null).getItem());
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingContainer container, @NotNull RegistryAccess access) {
        ItemStack center = container.getItem(CENTER);
        ItemStack result = new ItemStack(getResultItem(access).getItem());
        if (center.hasTag()) result.setTag(center.getTag().copy());
        return result;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return AvernusRecipes.MIRROR_ASPECT.get();
    }

    public static class Serializer implements RecipeSerializer<MirrorAspectRecipe> {
        @Override
        public MirrorAspectRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient material = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "material"));
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new MirrorAspectRecipe(id, material, result);
        }

        @Override
        public MirrorAspectRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new MirrorAspectRecipe(id, Ingredient.fromNetwork(buf), buf.readItem());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, MirrorAspectRecipe recipe) {
            recipe.material.toNetwork(buf);
            buf.writeItem(recipe.getResultItem(null));
        }
    }
}
