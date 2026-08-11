package buildcraft.api.v2.facade;

import java.util.Objects;
import net.minecraft.world.level.block.state.BlockState;

public record FacadeMaterial(BlockState state, boolean transparent) {
    public FacadeMaterial {
        Objects.requireNonNull(state, "state");
    }
}
