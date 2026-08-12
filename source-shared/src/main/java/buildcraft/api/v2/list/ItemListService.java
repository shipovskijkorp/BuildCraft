package buildcraft.api.v2.list;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public interface ItemListService {
    /** Optional named API2 list definitions. */
    Optional<ItemList> list(ResourceLocation id);
    Collection<ResourceLocation> ids();
    boolean matches(ResourceLocation listId, ItemStack stack);

    /** Resolves stack-backed list items without requiring implementation inheritance. */
    Optional<ItemListAdapter> adapter(ItemStack listStack);

    default boolean isList(ItemStack listStack) {
        return adapter(listStack).isPresent();
    }

    default boolean matches(ItemStack listStack, ItemStack target) {
        return adapter(listStack).map(a -> a.matches(listStack, target)).orElse(false);
    }
}
