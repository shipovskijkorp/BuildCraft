package buildcraft.api.v2.signal;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/** Stable IDs for the classic BuildCraft pipe-wire channels. */
public final class BuildCraftSignalChannels {
    public static final ResourceLocation WHITE = id("white");
    public static final ResourceLocation ORANGE = id("orange");
    public static final ResourceLocation MAGENTA = id("magenta");
    public static final ResourceLocation LIGHT_BLUE = id("light_blue");
    public static final ResourceLocation YELLOW = id("yellow");
    public static final ResourceLocation LIME = id("lime");
    public static final ResourceLocation PINK = id("pink");
    public static final ResourceLocation GRAY = id("gray");
    public static final ResourceLocation LIGHT_GRAY = id("light_gray");
    public static final ResourceLocation CYAN = id("cyan");
    public static final ResourceLocation PURPLE = id("purple");
    public static final ResourceLocation BLUE = id("blue");
    public static final ResourceLocation BROWN = id("brown");
    public static final ResourceLocation GREEN = id("green");
    public static final ResourceLocation RED = id("red");
    public static final ResourceLocation BLACK = id("black");

    private static final Map<DyeColor, ResourceLocation> IDS = new EnumMap<>(DyeColor.class);

    static {
        IDS.put(DyeColor.WHITE, WHITE);
        IDS.put(DyeColor.ORANGE, ORANGE);
        IDS.put(DyeColor.MAGENTA, MAGENTA);
        IDS.put(DyeColor.LIGHT_BLUE, LIGHT_BLUE);
        IDS.put(DyeColor.YELLOW, YELLOW);
        IDS.put(DyeColor.LIME, LIME);
        IDS.put(DyeColor.PINK, PINK);
        IDS.put(DyeColor.GRAY, GRAY);
        IDS.put(DyeColor.LIGHT_GRAY, LIGHT_GRAY);
        IDS.put(DyeColor.CYAN, CYAN);
        IDS.put(DyeColor.PURPLE, PURPLE);
        IDS.put(DyeColor.BLUE, BLUE);
        IDS.put(DyeColor.BROWN, BROWN);
        IDS.put(DyeColor.GREEN, GREEN);
        IDS.put(DyeColor.RED, RED);
        IDS.put(DyeColor.BLACK, BLACK);
    }

    private BuildCraftSignalChannels() {}

    public static ResourceLocation id(DyeColor color) {
        return Objects.requireNonNull(IDS.get(Objects.requireNonNull(color, "color")), "Missing signal channel id");
    }

    public static Optional<DyeColor> color(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        for (Map.Entry<DyeColor, ResourceLocation> entry : IDS.entrySet()) {
            if (entry.getValue().equals(id)) return Optional.of(entry.getKey());
        }
        return Optional.empty();
    }

    public static Map<DyeColor, ResourceLocation> classicChannels() {
        return Map.copyOf(IDS);
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("buildcraft:" + path));
    }
}
