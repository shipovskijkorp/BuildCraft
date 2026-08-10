package buildcraft.api.v2.crops;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Loader-neutral crop behavior extension point. */
public interface CropAdapter {
    boolean isSeed(ItemStack stack);
    boolean canSustainPlant(Level level, ItemStack seed, BlockPos pos);
    boolean plant(Level level, Player actor, ItemStack seed, BlockPos pos);
    boolean isMature(BlockGetter level, BlockState state, BlockPos pos);
    boolean harvest(Level level, BlockPos pos, NonNullList<ItemStack> drops, Player actor);
}
