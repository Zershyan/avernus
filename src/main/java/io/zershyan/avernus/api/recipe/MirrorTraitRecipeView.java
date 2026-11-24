package io.zershyan.avernus.api.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipe;

/** 境印锻造配方的只读契约，供配方展示兼容使用，不包含序列化与注册实现。 */
public interface MirrorTraitRecipeView extends SmithingRecipe {
    ResourceLocation trait();

    Ingredient material();

    boolean remove();
}
