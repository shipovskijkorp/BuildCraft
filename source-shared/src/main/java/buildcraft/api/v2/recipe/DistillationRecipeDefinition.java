package buildcraft.api.v2.recipe;

import buildcraft.api.v2.fluid.FluidAmount;
import buildcraft.api.v2.fluid.FluidVariant;
import buildcraft.api.v2.fluid.FluidVolume;
import buildcraft.api.v2.persistence.ApiCodec;
import buildcraft.api.v2.persistence.CodecResult;
import java.util.Objects;

/** Immutable Distiller recipe. */
public final class DistillationRecipeDefinition implements RecipeDefinition {
    public static final ApiCodec<DistillationRecipeDefinition, DistillationRecipeData> CODEC = new ApiCodec<>() {
        @Override
        public CodecResult<DistillationRecipeDefinition> decode(DistillationRecipeData data) {
            if (data == null) return CodecResult.failure("Distillation recipe payload is null");
            CodecResult<FluidIngredient> input = FluidIngredient.CODEC.decode(data.input());
            if (!input.successful()) return CodecResult.failure(String.join("; ", input.errors()));
            try {
                return CodecResult.success(new DistillationRecipeDefinition(
                    input.valueOrThrow(),
                    volume(data.gasVariant(), data.gasMilliBuckets()),
                    volume(data.liquidVariant(), data.liquidMilliBuckets()),
                    data.powerRequiredMicroMj()
                ));
            } catch (RuntimeException ex) {
                return CodecResult.failure(safeMessage(ex));
            }
        }

        @Override
        public CodecResult<DistillationRecipeData> encode(DistillationRecipeDefinition value) {
            if (value == null) return CodecResult.failure("Distillation recipe is null");
            CodecResult<FluidIngredientData> input = FluidIngredient.CODEC.encode(value.input);
            if (!input.successful()) return CodecResult.failure(String.join("; ", input.errors()));
            try {
                return CodecResult.success(new DistillationRecipeData(
                    input.valueOrThrow(),
                    variant(value.gasOutput), value.gasOutput.amount().milliBuckets(),
                    variant(value.liquidOutput), value.liquidOutput.amount().milliBuckets(),
                    value.powerRequiredMicroMj
                ));
            } catch (RuntimeException ex) {
                return CodecResult.failure(safeMessage(ex));
            }
        }

        private FluidVolume volume(buildcraft.api.v2.fluid.FluidVariantData variant, long amount) {
            if (variant == null && amount == 0) return FluidVolume.empty();
            if (variant == null) throw new IllegalArgumentException("Output amount exists without a fluid variant");
            if (amount <= 0) throw new IllegalArgumentException("Output amount must be > 0");
            FluidVariant decoded = FluidVariant.CODEC.decode(variant).valueOrThrow();
            return FluidVolume.of(decoded, FluidAmount.of(amount));
        }

        private buildcraft.api.v2.fluid.FluidVariantData variant(FluidVolume volume) {
            return volume.isEmpty() ? null : FluidVariant.CODEC.encode(volume.requireVariant()).valueOrThrow();
        }

        private String safeMessage(RuntimeException ex) {
            String msg = ex.getMessage();
            return msg == null || msg.isBlank() ? ex.getClass().getSimpleName() : msg;
        }
    };

    private final FluidIngredient input;
    private final FluidVolume gasOutput;
    private final FluidVolume liquidOutput;
    private final long powerRequiredMicroMj;

    public DistillationRecipeDefinition(
        FluidIngredient input, FluidVolume gasOutput, FluidVolume liquidOutput, long powerRequiredMicroMj
    ) {
        this.input = Objects.requireNonNull(input, "input");
        this.gasOutput = Objects.requireNonNull(gasOutput, "gasOutput");
        this.liquidOutput = Objects.requireNonNull(liquidOutput, "liquidOutput");
        if (gasOutput.isEmpty() && liquidOutput.isEmpty()) throw new IllegalArgumentException("Distillation recipe must produce at least one output");
        if (powerRequiredMicroMj < 0) throw new IllegalArgumentException("powerRequiredMicroMj must be >= 0");
        this.powerRequiredMicroMj = powerRequiredMicroMj;
    }

    @Override public Kind kind() { return Kind.DISTILLATION; }
    public FluidIngredient input() { return input; }
    public FluidVolume gasOutput() { return gasOutput; }
    public FluidVolume liquidOutput() { return liquidOutput; }
    public long powerRequiredMicroMj() { return powerRequiredMicroMj; }
}
