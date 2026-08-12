package buildcraft.api.v2.module;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class BuildCraftModules {
    public static final ResourceLocation LIB = id("lib");
    public static final ResourceLocation CORE = id("core");
    public static final ResourceLocation TRANSPORT = id("transport");
    public static final ResourceLocation ENERGY = id("energy");
    public static final ResourceLocation FACTORY = id("factory");
    public static final ResourceLocation BUILDERS = id("builders");
    public static final ResourceLocation SILICON = id("silicon");
    public static final ResourceLocation ROBOTICS = id("robotics");
    public static final ResourceLocation COMPAT = id("compat");

    private BuildCraftModules() {}
    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("buildcraft:" + path));
    }
}
