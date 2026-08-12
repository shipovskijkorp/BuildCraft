package buildcraft.lib.internal.api.v2;

import buildcraft.api.v2.permission.ActorService;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/** Internal factory for loader-neutral automation identities. */
final class ActorServiceImpl implements ActorService {
    @Override public AutomationActor player(UUID playerId, String playerName) {
        return AutomationActor.player(playerId, playerName);
    }

    @Override public AutomationActor machineOwner(UUID ownerId, String ownerName, ResourceLocation sourceId) {
        return AutomationActor.machineOwner(ownerId, ownerName, sourceId);
    }

    @Override public AutomationActor system(ResourceLocation sourceId) {
        return AutomationActor.system(sourceId);
    }

    @Override public AutomationActor unknown() {
        return AutomationActor.unknown();
    }
}
