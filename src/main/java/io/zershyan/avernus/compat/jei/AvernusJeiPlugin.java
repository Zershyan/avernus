package io.zershyan.avernus.compat.jei;

import io.zershyan.avernus.api.AvernusApi;
import io.zershyan.avernus.api.realm.MirrorForm;
import io.zershyan.avernus.api.recipe.MirrorTraitRecipeView;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.recipe.category.extensions.vanilla.smithing.ISmithingCategoryExtension;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** 可选 JEI 兼容：类型锻造配方显示在原版锻造页；未安装 JEI 时不会加载。 */
@JeiPlugin
public final class AvernusJeiPlugin implements IModPlugin {
    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(AvernusApi.MODID, "jei");
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getSmithingCategory().addExtension(MirrorTraitRecipeView.class, new TraitSmithing());
    }

    private static final class TraitSmithing implements ISmithingCategoryExtension<MirrorTraitRecipeView> {
        @Override
        public <T extends IIngredientAcceptor<T>> void setTemplate(MirrorTraitRecipeView recipe, T ingredients) {
            if (recipe.remove()) ingredients.addItemStack(new ItemStack(Items.FEATHER));
        }

        @Override
        public <T extends IIngredientAcceptor<T>> void setBase(MirrorTraitRecipeView recipe, T ingredients) {
            ingredients.addItemStacks(mirrors(recipe, recipe.remove()));
        }

        @Override
        public <T extends IIngredientAcceptor<T>> void setAddition(MirrorTraitRecipeView recipe, T ingredients) {
            ingredients.addIngredients(recipe.material());
        }

        @Override
        public <T extends IIngredientAcceptor<T>> void setOutput(MirrorTraitRecipeView recipe, T ingredients) {
            ingredients.addItemStacks(mirrors(recipe, !recipe.remove()));
        }

        /** 每个境相一面镜子，按需带上该类型；底座与产物顺序一致，轮播时一一对应。 */
        private static List<ItemStack> mirrors(MirrorTraitRecipeView recipe, boolean withTrait) {
            return AvernusApi.mirrorItems().stream().map(mirror -> {
                ItemStack stack = new ItemStack(mirror);
                if (!withTrait) return stack;
                MirrorForm form = AvernusApi.formOf(stack).orElseThrow();
                return AvernusApi.createMirror(new MirrorForm(form.aspect(), List.of(recipe.trait())));
            }).toList();
        }
    }
}
