package buildcraft.lib.internal.api.v2;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftRegistries;
import buildcraft.api.v2.filler.FillerPatternService;
import buildcraft.api.v2.filler.FillerPatternType;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Registry-backed filler pattern service. */
public final class FillerPatternServiceImpl implements FillerPatternService {
    public static final FillerPatternServiceImpl INSTANCE = new FillerPatternServiceImpl();

    private FillerPatternServiceImpl() {}

    @Override
    public Optional<FillerPatternType> type(ResourceLocation id) {
        return Optional.ofNullable(BuildCraftApi.registry(BuildCraftRegistries.FILLER_PATTERN_TYPES).get(id));
    }

    @Override
    public Collection<FillerPatternType> types() {
        return java.util.List.copyOf(BuildCraftApi.registry(BuildCraftRegistries.FILLER_PATTERN_TYPES).values());
    }
}
