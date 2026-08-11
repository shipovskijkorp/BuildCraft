package buildcraft.api.v2.signal;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Stable IDs for the four classic BuildCraft wire channels. */
public final class BuildCraftSignalChannels {
    public static final ResourceLocation RED = id("red");
    public static final ResourceLocation BLUE = id("blue");
    public static final ResourceLocation GREEN = id("green");
    public static final ResourceLocation YELLOW = id("yellow");

    private BuildCraftSignalChannels() {}
    private static ResourceLocation id(String path) { return Objects.requireNonNull(ResourceLocation.tryParse("buildcraft:" + path)); }
}
