package io.zershyan.avernus.registry.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.zershyan.avernus.api.AvernusApi;
import io.zershyan.avernus.registry.AvernusDataComponents;
import io.zershyan.avernus.registry.AvernusRecipes;
import io.zershyan.avernus.registry.data_component.MirrorForm;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/** 工作台切换境相：中心任意形态的镜，周围八格同种材料；保留类型与其余组件。 */
public class MirrorAspectRecipe extends CustomRecipe {
    private static final int CENTER = 4;

    private final ResourceLocation aspect;
    private final Ingredient material;

    public MirrorAspectRecipe(ResourceLocation aspect, Ingredient material) {
        super(CraftingBookCategory.MISC);
        this.aspect = aspect;
        this.material = material;
    }

    public ResourceLocation aspect() {
        return aspect;
    }

    public Ingredient material() {
        return material;
    }

    @Override
    public boolean matches(CraftingInput input, @NotNull Level level) {
        if (input.width() != 3 || input.height() != 3) return false;
        ItemStack mirror = input.getItem(CENTER);
        if (!AvernusApi.isMirror(mirror)) return false;
        MirrorForm next = AvernusApi.formOf(mirror).withAspect(aspect);
        if (next.equals(AvernusApi.formOf(mirror)) || next.resolve().isEmpty()) return false;
        for (int i = 0; i < input.size(); i++) {
            if (i != CENTER && !material.test(input.getItem(i))) return false;
        }
        return true;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.@NotNull Provider registries) {
        ItemStack result = input.getItem(CENTER).copyWithCount(1);
        result.set(AvernusDataComponents.MIRROR_FORM.get(), AvernusApi.formOf(result).withAspect(aspect));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return AvernusRecipes.MIRROR_ASPECT.get();
    }

    public static final MapCodec<MirrorAspectRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("aspect").forGetter(MirrorAspectRecipe::aspect),
            Ingredient.CODEC_NONEMPTY.fieldOf("material").forGetter(MirrorAspectRecipe::material)
    ).apply(instance, MirrorAspectRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MirrorAspectRecipe> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, MirrorAspectRecipe::aspect,
            Ingredient.CONTENTS_STREAM_CODEC, MirrorAspectRecipe::material,
            MirrorAspectRecipe::new);
}
