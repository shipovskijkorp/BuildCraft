package buildcraft.factory;

import java.util.List;
import java.util.Set;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;

/** Empty compatibility provider retained for callers that create module providers directly. */
public class BCFactoryLootTableProvider extends LootTableProvider {
    public BCFactoryLootTableProvider(PackOutput output) {
        super(output, Set.of(), List.of());
    }
}
