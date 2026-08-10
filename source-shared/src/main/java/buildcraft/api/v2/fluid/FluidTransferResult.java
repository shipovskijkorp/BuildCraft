package buildcraft.api.v2.fluid;

import java.util.Objects;

/**
 * Structured result for one fluid-port operation.
 *
 * requestedAmount records the caller's upper bound; transferred contains the
 * amount that actually moved. A result never implies that simulation and a
 * later execution will be identical if the world changed in between.
 */
public final class FluidTransferResult {
    private final FluidAmount requestedAmount;
    private final FluidVolume transferred;

    private FluidTransferResult(FluidAmount requestedAmount, FluidVolume transferred) {
        this.requestedAmount = requestedAmount;
        this.transferred = transferred;
    }

    public static FluidTransferResult nothing(FluidAmount requestedAmount) {
        return ofExtraction(requestedAmount, FluidVolume.empty());
    }

    public static FluidTransferResult ofInsertion(FluidVolume offered, FluidAmount acceptedAmount) {
        Objects.requireNonNull(offered, "offered");
        Objects.requireNonNull(acceptedAmount, "acceptedAmount");
        if (offered.isEmpty() && !acceptedAmount.isZero()) {
            throw new IllegalArgumentException("Cannot accept fluid from an empty offer");
        }
        if (acceptedAmount.compareTo(offered.amount()) > 0) {
            throw new IllegalArgumentException("Accepted amount exceeds offered amount");
        }
        FluidVolume transferred = acceptedAmount.isZero()
            ? FluidVolume.empty()
            : FluidVolume.of(offered.requireVariant(), acceptedAmount);
        return new FluidTransferResult(offered.amount(), transferred);
    }

    public static FluidTransferResult ofExtraction(FluidAmount requestedAmount, FluidVolume extracted) {
        Objects.requireNonNull(requestedAmount, "requestedAmount");
        Objects.requireNonNull(extracted, "extracted");
        if (extracted.amount().compareTo(requestedAmount) > 0) {
            throw new IllegalArgumentException("Extracted amount exceeds requested amount");
        }
        return new FluidTransferResult(requestedAmount, extracted);
    }

    public FluidAmount requestedAmount() {
        return requestedAmount;
    }

    public FluidVolume transferred() {
        return transferred;
    }

    public FluidAmount transferredAmount() {
        return transferred.amount();
    }

    public FluidAmount remainderAmount() {
        return requestedAmount.minus(transferred.amount());
    }

    public boolean movedAnything() {
        return !transferred.isEmpty();
    }

    public boolean completed() {
        return remainderAmount().isZero();
    }

    @Override
    public String toString() {
        return "FluidTransferResult[requested=" + requestedAmount + ", transferred=" + transferred + "]";
    }
}
