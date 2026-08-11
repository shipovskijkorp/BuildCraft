package buildcraft.api.v2.list;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public interface ItemListService {
    Optional<ItemList> list(ResourceLocation id);
    Collection<ResourceLocation> ids();
    boolean matches(ResourceLocation listId, ItemStack stack);
}
