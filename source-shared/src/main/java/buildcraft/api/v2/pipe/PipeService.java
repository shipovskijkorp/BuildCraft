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

/** Runtime lookup and safe mutation facade for BuildCraft pipes.
 *
 * <p>{@link #createItem(ResourceLocation)} is the supported bridge for registering an item for an
 * API2 pipe variant. It reuses the variant's declared archetype without exposing BCCE PipeDefinition internals.</p>
 */
public interface PipeService {
    /**
     * Creates the vanilla Item used to place a registered API2 pipe type. The type must be a runtime archetype
     * (a built-in pipe) or a {@link PipeType#variant(ResourceLocation, PipeType) variant} of one.
     * Addons remain responsible for registering the returned Item with their loader's item registry.
     */
    net.minecraft.world.item.Item createItem(ResourceLocation pipeTypeId);

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
