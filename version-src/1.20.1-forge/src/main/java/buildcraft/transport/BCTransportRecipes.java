package buildcraft.transport;

import buildcraft.transport.recipe.PipeRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Recipe serializers owned by the transport module. */
public final class BCTransportRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, BCTransport.MODID);

    public static final RegistryObject<RecipeSerializer<PipeRecipe>> PIPE =
            SERIALIZERS.register("pipe", PipeRecipe.Serializer::new);

    private BCTransportRecipes() {
    }

    public static void preInit(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}
