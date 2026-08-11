package buildcraft.api.v2.schematic;

import buildcraft.api.v2.area.BlockBox;
import java.util.Objects;

public record BlueprintMetadata(String name, String author, BlockBox bounds) {
    public BlueprintMetadata {
        name = name == null ? "" : name;
        author = author == null ? "" : author;
        Objects.requireNonNull(bounds, "bounds");
    }
}
