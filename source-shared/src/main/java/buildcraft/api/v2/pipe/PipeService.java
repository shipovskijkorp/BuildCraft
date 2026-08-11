package buildcraft.api.v2.pipe;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.item.ItemTransferResult;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Runtime lookup and safe mutation facade for BuildCraft pipes. */
public interface PipeService {
    Optional<PipeView> pipe(Level level, BlockPos pos);

    PipeAttachmentResult placeAttachment(
        Level level,
        BlockPos pos,
        Direction side,
        ResourceLocation attachmentTypeId,
        ItemStack stack,
        AutomationActor actor,
        OperationMode mode
    );

    boolean removeAttachment(Level level, BlockPos pos, Direction side, AutomationActor actor, OperationMode mode);

    ItemTransferResult injectItem(Level level, BlockPos pos, ItemInjectionRequest request, OperationMode mode);
}
