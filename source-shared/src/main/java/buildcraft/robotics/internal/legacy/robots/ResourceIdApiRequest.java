package buildcraft.robotics.internal.legacy.robots;

import buildcraft.lib.internal.core.BlockIndex;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Internal persistent reservation key for API2 slot-independent item requests. */
public final class ResourceIdApiRequest extends ResourceId {
    private BlockIndex index = new BlockIndex();
    @Nullable private Direction side;
    private ResourceLocation requestId = new ResourceLocation("buildcraft", "invalid_request");

    public ResourceIdApiRequest() {}

    public ResourceIdApiRequest(DockingStation station, ResourceLocation requestId) {
        this.index = station.index();
        this.side = station.side();
        this.requestId = Objects.requireNonNull(requestId, "requestId");
    }

    public ResourceLocation requestId() { return requestId; }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ResourceIdApiRequest other
            && Objects.equals(index, other.index)
            && side == other.side
            && Objects.equals(requestId, other.requestId);
    }

    @Override
    public int hashCode() { return Objects.hash(index, side, requestId); }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        CompoundTag indexTag = new CompoundTag();
        index.writeTo(indexTag);
        nbt.put("index", indexTag);
        nbt.putByte("side", (byte) (side == null ? -1 : side.ordinal()));
        nbt.putString("requestId", requestId.toString());
    }

    @Override
    protected void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        index = new BlockIndex(nbt.getCompound("index"));
        byte sideId = nbt.getByte("side");
        side = sideId >= 0 && sideId < Direction.values().length ? Direction.values()[sideId] : null;
        ResourceLocation parsed = ResourceLocation.tryParse(nbt.getString("requestId"));
        if (parsed != null) requestId = parsed;
    }
}
