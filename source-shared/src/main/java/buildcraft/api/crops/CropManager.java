package buildcraft.api.crops;

import buildcraft.api.v2.crops.CropAdapter;
import buildcraft.lib.internal.api.v2.BuildCraftApiRuntime;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Legacy compatibility facade over API 2's ordered crop service. */
public final class CropManager {
    private static final ResourceLocation DEFAULT_ID = id("legacy/default");
    private static final AtomicLong NEXT_ID = new AtomicLong();
    private static volatile ICropHandler defaultHandler;

    private CropManager() {}

    public static void registerHandler(ICropHandler cropHandler) {
        Objects.requireNonNull(cropHandler, "cropHandler");
        long sequence = NEXT_ID.getAndIncrement();
        ResourceLocation id = id(String.format(Locale.ROOT, "legacy/handler/%010d", sequence));
        BuildCraftApiRuntime.INSTANCE.crops().register(id, 0, adapt(cropHandler));
    }

    public static void setDefaultHandler(ICropHandler cropHandler) {
        defaultHandler = cropHandler;
        if (cropHandler == null) {
            BuildCraftApiRuntime.INSTANCE.crops().removeLegacy(DEFAULT_ID);
        } else {
            BuildCraftApiRuntime.INSTANCE.crops().replaceLegacy(DEFAULT_ID, Integer.MIN_VALUE, adapt(cropHandler));
        }
    }

    public static ICropHandler getDefaultHandler() {
        return defaultHandler;
    }

    public static boolean isSeed(ItemStack stack) {
        return BuildCraftApiRuntime.INSTANCE.crops().isSeed(stack);
    }

    public static boolean canSustainPlant(Level world, ItemStack seed, BlockPos pos) {
        return BuildCraftApiRuntime.INSTANCE.crops().canSustainPlant(world, seed, pos);
    }

    public static boolean plantCrop(Level world, Player player, ItemStack seed, BlockPos pos) {
        return BuildCraftApiRuntime.INSTANCE.crops().plant(world, player, seed, pos);
    }

    public static boolean isMature(BlockGetter blockAccess, BlockState state, BlockPos pos) {
        return BuildCraftApiRuntime.INSTANCE.crops().isMature(blockAccess, state, pos);
    }

    public static boolean harvestCrop(Level world, BlockPos pos, NonNullList<ItemStack> drops) {
        return harvestCrop(world, pos, drops, null);
    }

    public static boolean harvestCrop(Level world, BlockPos pos, NonNullList<ItemStack> drops, Player actor) {
        return BuildCraftApiRuntime.INSTANCE.crops().harvest(world, pos, drops, actor);
    }

    private static CropAdapter adapt(ICropHandler handler) {
        return new CropAdapter() {
            @Override public boolean isSeed(ItemStack stack) { return handler.isSeed(stack); }
            @Override public boolean canSustainPlant(Level level, ItemStack seed, BlockPos pos) {
                return handler.canSustainPlant(level, seed, pos);
            }
            @Override public boolean plant(Level level, Player actor, ItemStack seed, BlockPos pos) {
                return handler.plantCrop(level, actor, seed, pos);
            }
            @Override public boolean isMature(BlockGetter level, BlockState state, BlockPos pos) {
                return handler.isMature(level, state, pos);
            }
            @Override public boolean harvest(Level level, BlockPos pos, NonNullList<ItemStack> drops, Player actor) {
                return handler.harvestCrop(level, pos, drops, actor);
            }
        };
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("buildcraft:" + path));
    }
}
