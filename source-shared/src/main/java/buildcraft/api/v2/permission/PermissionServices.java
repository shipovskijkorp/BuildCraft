package buildcraft.api.v2.permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Composition helpers with deterministic DENY-terminal semantics. */
public final class PermissionServices {
    private PermissionServices() {}

    public static PermissionService compose(Collection<? extends PermissionService> services) {
        Objects.requireNonNull(services, "services");
        List<PermissionService> copy = new ArrayList<>(services.size());
        for (PermissionService service : services) copy.add(Objects.requireNonNull(service, "service"));
        return context -> {
            Objects.requireNonNull(context, "context");
            PermissionDecision allowed = null;
            for (PermissionService service : copy) {
                PermissionDecision decision = Objects.requireNonNull(service.decide(context), "permission decision");
                if (decision.verdict() == PermissionVerdict.DENY) return decision;
                if (decision.verdict() == PermissionVerdict.ALLOW && allowed == null) allowed = decision;
            }
            return allowed == null ? PermissionDecision.pass() : allowed;
        };
    }
}
