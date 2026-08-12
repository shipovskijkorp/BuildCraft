package buildcraft.api.v2.list;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/**
 * Addon hook for special list-entry semantics such as tool classes, fluid containers or tagged materials.
 * Returning an empty optional from {@link #examples(ListMatchContext)} asks the caller to discover examples by
 * enumerating candidates and calling {@link #matches(ListMatchContext)}.
 */
public interface ListMatchAdapter {
    boolean supports(ListMatchContext context);
    boolean matches(ListMatchContext context);

    default Optional<List<ItemStack>> examples(ListMatchContext context) {
        return Optional.empty();
    }
}
