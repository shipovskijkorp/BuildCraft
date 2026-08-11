package buildcraft.api.v2.automation;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class AutomationKinds {
    public static final ResourceLocation BREAK_BLOCK = id("break_block");
    public static final ResourceLocation PLACE_BLOCK = id("place_block");
    public static final ResourceLocation USE_ITEM = id("use_item");
    public static final ResourceLocation MOVE_ITEM = id("move_item");
    public static final ResourceLocation MOVE_FLUID = id("move_fluid");

    private AutomationKinds() {}
    private static ResourceLocation id(String path) { return Objects.requireNonNull(ResourceLocation.tryParse("buildcraft:" + path)); }
}
