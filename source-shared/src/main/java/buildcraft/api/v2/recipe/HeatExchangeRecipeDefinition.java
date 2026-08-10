package buildcraft.api.v2.recipe;

import buildcraft.api.v2.fluid.FluidAmount;
import buildcraft.api.v2.fluid.FluidVariant;
import buildcraft.api.v2.fluid.FluidVolume;
import buildcraft.api.v2.persistence.ApiCodec;
import buildcraft.api.v2.persistence.CodecResult;
import java.util.Objects;

/** Immutable heating or cooling recipe for the Heat Exchanger. */
public final class HeatExchangeRecipeDefinition implements RecipeDefinition {
    public static final ApiCodec<HeatExchangeRecipeDefinition, HeatExchangeRecipeData> CODEC = new ApiCodec<>() {
        @Override
        public CodecResult<HeatExchangeRecipeDefinition> decode(HeatExchangeRecipeData data) {
            if (data == null) return CodecResult.failure("Heat exchange payload is null");
            if (data.kind() != Kind.HEATING && data.kind() != Kind.COOLING) {
                return CodecResult.failure("Heat exchange kind must be HEATING or COOLING");
            }
            CodecResult<FluidIngredient> input = FluidIngredient.CODEC.decode(data.input());
            if (!input.successful()) return CodecResult.failure(String.join("; ", input.errors()));
            try {
                FluidVolume output = FluidVolume.empty();
                if (data.outputVariant() != null || data.outputMilliBuckets() != 0) {
                    if (data.outputVariant() == null || data.outputMilliBuckets() <= 0) {
                        return CodecResult.failure("Heat exchange output variant and positive amount must be specified together");
                    }
                    FluidVariant variant = FluidVariant.CODEC.decode(data.outputVariant()).valueOrThrow();
                    output = FluidVolume.of(variant, FluidAmount.of(data.outputMilliBuckets()));
                }
                return CodecResult.success(new HeatExchangeRecipeDefinition(
                    data.kind(), input.valueOrThrow(), output, data.heatFrom(), data.heatTo()
                ));
            } catch (RuntimeException ex) {
                return CodecResult.failure(safeMessage(ex));
            }
        }

        @Override
        public CodecResult<HeatExchangeRecipeData> encode(HeatExchangeRecipeDefinition value) {
            if (value == null) return CodecResult.failure("Heat exchange recipe is null");
            CodecResult<FluidIngredientData> input = FluidIngredient.CODEC.encode(value.input);
            if (!input.successful()) return CodecResult.failure(String.join("; ", input.errors()));
            buildcraft.api.v2.fluid.FluidVariantData outputVariant = value.output.isEmpty()
                ? null : FluidVariant.CODEC.encode(value.output.requireVariant()).valueOrThrow();
            return CodecResult.success(new HeatExchangeRecipeData(
                value.kind, input.valueOrThrow(), outputVariant, value.output.amount().milliBuckets(), value.heatFrom, value.heatTo
            ));
        }

        private String safeMessage(RuntimeException ex) {
            String msg = ex.getMessage();
            return msg == null || msg.isBlank() ? ex.getClass().getSimpleName() : msg;
        }
    };

    private final Kind kind;
    private final FluidIngredient input;
    private final FluidVolume output;
    private final int heatFrom;
    private final int heatTo;

    public HeatExchangeRecipeDefinition(Kind kind, FluidIngredient input, FluidVolume output, int heatFrom, int heatTo) {
        if (kind != Kind.HEATING && kind != Kind.COOLING) throw new IllegalArgumentException("kind must be HEATING or COOLING");
        if (kind == Kind.HEATING && heatTo <= heatFrom) throw new IllegalArgumentException("Heating recipe must increase heat");
        if (kind == Kind.COOLING && heatTo >= heatFrom) throw new IllegalArgumentException("Cooling recipe must decrease heat");
        this.kind = kind;
        this.input = Objects.requireNonNull(input, "input");
        this.output = Objects.requireNonNull(output, "output");
        this.heatFrom = heatFrom;
        this.heatTo = heatTo;
    }

    @Override public Kind kind() { return kind; }
    public FluidIngredient input() { return input; }
    public FluidVolume output() { return output; }
    public int heatFrom() { return heatFrom; }
    public int heatTo() { return heatTo; }
}
