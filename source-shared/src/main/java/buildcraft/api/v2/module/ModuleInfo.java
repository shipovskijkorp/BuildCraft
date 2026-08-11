package buildcraft.api.v2.module;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record ModuleInfo(ResourceLocation id, String version, boolean loaded) {
    public ModuleInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
    }
}
