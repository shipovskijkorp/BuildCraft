package buildcraft.api.v2.filler;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public interface FillerPatternService {
    Optional<FillerPatternType> type(ResourceLocation id);
    Collection<FillerPatternType> types();
}
