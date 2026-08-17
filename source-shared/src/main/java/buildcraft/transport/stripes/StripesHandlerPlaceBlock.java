/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.transport.stripes;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.automation.StripesOutput;
import buildcraft.api.v2.permission.WorldOperationKind;
import buildcraft.lib.misc.AutomationPermissionUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public enum StripesHandlerPlaceBlock {
    INSTANCE;

    public boolean handle(Level world,
                          BlockPos pos,
                          Direction direction,
                          ItemStack stack,
                          Player player,
                          StripesOutput activator) {
        if (!(stack.getItem() instanceof BlockItem)) {
            return false;
        }
        BlockPos target = pos.relative(direction);
        if (!world.isEmptyBlock(target)) {
            return false;
        }
        if (!AutomationPermissionUtil.mayBlock(
            world, pos, target, player.getGameProfile(), AutomationPermissionUtil.SOURCE_STRIPES_PIPE,
            WorldOperationKind.BLOCK_PLACE, OperationMode.EXECUTE
        )) {
            return false;
        }
        return stack.getItem().useOn(new UseOnContext(
            player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(target), direction, target, false)
        )).consumesAction();
    }
}
