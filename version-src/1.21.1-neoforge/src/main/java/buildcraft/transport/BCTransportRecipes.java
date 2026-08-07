package buildcraft.transport;

import buildcraft.transport.recipe.PipeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Recipe serializers owned by the transport module. */
public final class BCTransportRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, BCTransport.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PipeRecipe>> PIPE =
        SERIALIZERS.register("pipe", PipeRecipe.Serializer::new);

    private BCTransportRecipes() {
    }

    public static void preInit(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}
