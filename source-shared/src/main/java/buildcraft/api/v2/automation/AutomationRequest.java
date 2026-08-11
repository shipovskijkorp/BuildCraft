package buildcraft.api.v2.automation;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.permission.AutomationActor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public interface AutomationRequest {
    ResourceLocation kind();
    AutomationActor actor();
    OperationMode mode();
    BlockPos origin();
}
