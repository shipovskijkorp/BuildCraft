package buildcraft.api.v2.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Opaque persisted data that BuildCraft cannot currently interpret.
 *
 * The original type id, schema version and raw payload are retained so a
 * temporarily missing addon does not cause save-data loss.
 */
public final class UnknownPayload<T, P> implements PayloadResolution<T, P> {
    private final ResourceLocation storedTypeId;
    private final int storedSchemaVersion;
    private final P rawPayload;
    private final UnknownPayloadReason reason;
    private final List<String> diagnostics;

    public UnknownPayload(
        ResourceLocation storedTypeId,
        int storedSchemaVersion,
        P rawPayload,
        UnknownPayloadReason reason,
        List<String> diagnostics
    ) {
        this.storedTypeId = Objects.requireNonNull(storedTypeId, "storedTypeId");
        if (storedSchemaVersion < 0) {
            throw new IllegalArgumentException("storedSchemaVersion must be non-negative");
        }
        this.storedSchemaVersion = storedSchemaVersion;
        this.rawPayload = Objects.requireNonNull(rawPayload, "rawPayload");
        this.reason = Objects.requireNonNull(reason, "reason");
        this.diagnostics = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(diagnostics, "diagnostics")));
    }

    @Override
    public ResourceLocation storedTypeId() {
        return storedTypeId;
    }

    @Override
    public int storedSchemaVersion() {
        return storedSchemaVersion;
    }

    public P rawPayload() {
        return rawPayload;
    }

    public UnknownPayloadReason reason() {
        return reason;
    }

    public List<String> diagnostics() {
        return diagnostics;
    }

    @Override
    public boolean known() {
        return false;
    }

    public EncodedPayload<P> toEncodedPayload(PayloadCopier<P> copier) {
        Objects.requireNonNull(copier, "copier");
        return new EncodedPayload<>(storedTypeId, storedSchemaVersion, copier.copy(rawPayload));
    }
}
