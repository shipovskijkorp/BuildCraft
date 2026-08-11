package buildcraft.api.v2.robot;

import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record RobotBoardType(ResourceLocation id, int tier, Set<ResourceLocation> taskTypes) {
    public RobotBoardType {
        Objects.requireNonNull(id, "id");
        if (tier < 0) throw new IllegalArgumentException("tier must be non-negative");
        taskTypes = Set.copyOf(Objects.requireNonNull(taskTypes, "taskTypes"));
    }
}
