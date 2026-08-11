package buildcraft.api.v2.client;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public interface ClientPresentationService {
    Optional<ContentPresentation> presentation(ResourceLocation contentId);
    Optional<PipePresentation> pipe(ResourceLocation pipeTypeId);
    Optional<StatementPresentation> statement(ResourceLocation statementId);
    Optional<ParameterPresentation> parameter(ResourceLocation parameterTypeId);
}
