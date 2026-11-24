package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.recipe.MirrorAspectRecipe;
import io.zershyan.avernus.registry.recipe.MirrorTraitRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class AvernusRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> REGISTER =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Avernus.MODID);

    public static final RegistryObject<RecipeSerializer<?>> MIRROR_ASPECT =
            REGISTER.register("mirror_aspect", MirrorAspectRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<?>> MIRROR_TRAIT =
            REGISTER.register("mirror_trait", MirrorTraitRecipe.Serializer::new);

    private AvernusRecipes() {
    }

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
