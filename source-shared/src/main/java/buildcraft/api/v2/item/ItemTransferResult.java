package buildcraft.api.v2.item;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/** Structured result for one item-port operation. */
public final class ItemTransferResult {
    private final int requestedCount;
    private final ItemStack transferred;

    private ItemTransferResult(int requestedCount, ItemStack transferred) {
        this.requestedCount = requestedCount;
        this.transferred = transferred;
    }

    public static ItemTransferResult ofInsertion(ItemStack offered, int acceptedCount) {
        Objects.requireNonNull(offered, "offered");
        validateRequested(offered.getCount());
        if (acceptedCount < 0 || acceptedCount > offered.getCount()) {
            throw new IllegalArgumentException("Accepted count must be between 0 and the offered stack size");
        }
        ItemStack moved = acceptedCount == 0 ? ItemStack.EMPTY : copyWithCount(offered, acceptedCount);
        return new ItemTransferResult(offered.getCount(), moved);
    }

    public static ItemTransferResult ofExtraction(int requestedCount, ItemStack extracted) {
        validateRequested(requestedCount);
        Objects.requireNonNull(extracted, "extracted");
        if (extracted.getCount() > requestedCount) {
            throw new IllegalArgumentException("Extracted count exceeds requested count");
        }
        return new ItemTransferResult(requestedCount, extracted.copy());
    }

    public static ItemTransferResult nothing(int requestedCount) {
        validateRequested(requestedCount);
        return new ItemTransferResult(requestedCount, ItemStack.EMPTY);
    }

    public int requestedCount() {
        return requestedCount;
    }

    public ItemStack transferred() {
        return transferred.copy();
    }

    public int transferredCount() {
        return transferred.getCount();
    }

    public int remainderCount() {
        return requestedCount - transferred.getCount();
    }

    public boolean movedAnything() {
        return !transferred.isEmpty();
    }

    public boolean completed() {
        return remainderCount() == 0;
    }

    private static ItemStack copyWithCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    private static void validateRequested(int requestedCount) {
        if (requestedCount < 0) {
            throw new IllegalArgumentException("Requested count must be non-negative");
        }
    }
}
