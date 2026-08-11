package buildcraft.api.v2.statement;

import buildcraft.api.v2.persistence.ApiCodec;
import buildcraft.api.v2.persistence.OpaqueData;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class ParameterType<T> {
    private final ResourceLocation id;
    private final ApiCodec<T, OpaqueData> codec;
    private final StatementSuggestionProvider<T> suggestions;

    public ParameterType(ResourceLocation id, ApiCodec<T, OpaqueData> codec, StatementSuggestionProvider<T> suggestions) {
        this.id = Objects.requireNonNull(id, "id");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.suggestions = Objects.requireNonNull(suggestions, "suggestions");
    }

    public ParameterType(ResourceLocation id, ApiCodec<T, OpaqueData> codec) {
        this(id, codec, context -> List.of());
    }

    public ResourceLocation id() { return id; }
    public ApiCodec<T, OpaqueData> codec() { return codec; }
    public List<T> suggestions(StatementContext context) { return List.copyOf(suggestions.suggestions(context)); }
}
