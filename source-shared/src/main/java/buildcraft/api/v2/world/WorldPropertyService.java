package buildcraft.api.v2.world;

import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public interface WorldPropertyService {
    void register(ResourceLocation id, WorldProperty property);
    Optional<WorldProperty> get(ResourceLocation id);
    Map<ResourceLocation, WorldProperty> properties();

    default boolean test(ResourceLocation id, Level level, BlockPos pos) {
        return get(id).map(property -> property.test(level, pos)).orElse(false);
    }
}
