package io.zershyan.avernus.registry;

import com.mojang.serialization.MapCodec;
import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.recipe.MirrorAspectRecipe;
import io.zershyan.avernus.registry.recipe.MirrorTraitRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class AvernusRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> REGISTER =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Avernus.MODID);

    public static final RegistryObject<RecipeSerializer<?>, RecipeSerializer<MirrorAspectRecipe>> MIRROR_ASPECT =
            REGISTER.register("mirror_aspect", () -> serializer(MirrorAspectRecipe.CODEC, MirrorAspectRecipe.STREAM_CODEC));
    public static final RegistryObject<RecipeSerializer<?>, RecipeSerializer<MirrorTraitRecipe>> MIRROR_TRAIT =
            REGISTER.register("mirror_trait", () -> serializer(MirrorTraitRecipe.CODEC, MirrorTraitRecipe.STREAM_CODEC));

    private AvernusRecipes() {
    }

    private static <T extends Recipe<?>> RecipeSerializer<T> serializer(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
        return new RecipeSerializer<>() {
            @Override
            public MapCodec<T> codec() {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
                return streamCodec;
            }
        };
    }

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
