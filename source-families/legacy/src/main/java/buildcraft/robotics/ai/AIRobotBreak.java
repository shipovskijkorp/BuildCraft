package buildcraft.robotics.ai;

import buildcraft.api.core.BlockIndex;
import buildcraft.api.robots.AIRobot;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.FakePlayerProvider;
import buildcraft.robotics.entity.EntityRobot;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class AIRobotBreak extends AIRobot {
    private BlockIndex blockToBreak;
    private float blockDamage;
    private BlockState state;
    private float hardness;
    private float speed;

    public AIRobotBreak(EntityRobotBase robot) {
        super(robot);
    }

    public AIRobotBreak(EntityRobotBase robot, BlockIndex blockToBreak) {
        this(robot);
        this.blockToBreak = blockToBreak;
    }

    @Override
    public void start() {
        if (blockToBreak == null) {
            setSuccess(false);
            terminate();
            return;
        }
        robot.aimItemAt(blockToBreak.x, blockToBreak.y, blockToBreak.z);
        robot.setItemActive(true);
        refreshBlockData();
    }

    @Override
    public void update() {
        //? if <1.20 {
        if (!(robot.level instanceof ServerLevel serverLevel) || blockToBreak == null) {
        //?} else {
        /*?
        if (!(robot.level() instanceof ServerLevel serverLevel) || blockToBreak == null) {
        ?*/
        //?}
            setSuccess(false);
            terminate();
            return;
        }
        BlockPos pos = blockToBreak.toBlockPos();
        if (state == null || serverLevel.getBlockState(pos).isAir() || serverLevel.getBlockState(pos) != state) {
            refreshBlockData();
        }
        if (state == null || state.isAir() || hardness < 0.0F) {
            setSuccess(false);
            terminate();
            return;
        }

        if (hardness != 0.0F) {
            blockDamage += speed / hardness / 30.0F;
        } else {
            blockDamage = 1.1F;
        }

        if (blockDamage > 1.0F) {
            serverLevel.destroyBlockProgress(robot.getId(), pos, -1);
            blockDamage = 0.0F;
            ItemStack held = robot.getItemBySlot(EquipmentSlot.MAINHAND);
            GameProfile owner = robot instanceof EntityRobot entityRobot
                    ? entityRobot.getOwnerProfile()
                    : FakePlayerProvider.NULL_PROFILE;
            Player fakePlayer = BlockUtil.getFakePlayerWithTool(serverLevel, held, owner, pos);
            boolean harvested = BlockUtil.harvestBlock(serverLevel, pos, held, owner);
            if (harvested) {
                serverLevel.levelEvent(null, 2001, pos, Block.getId(state));
                if (!held.isEmpty()) {
                    held.mineBlock(serverLevel, state, pos, fakePlayer);
                    if (held.isEmpty()) {
                        robot.setItemInUse(ItemStack.EMPTY);
                    }
                }
            } else {
                setSuccess(false);
            }
            terminate();
        } else {
            serverLevel.destroyBlockProgress(robot.getId(), pos, (int) (blockDamage * 10.0F) - 1);
        }
    }

    private void refreshBlockData() {
        BlockPos pos = blockToBreak.toBlockPos();
        //? if <1.20 {
        state = robot.level.getBlockState(pos);
        //?} else {
        /*?
        state = robot.level().getBlockState(pos);
        ?*/
        //?}
        if (state == null || state.isAir()) {
            hardness = -1.0F;
            speed = 0.0F;
            return;
        }
        //? if <1.20 {
        hardness = state.getDestroySpeed(robot.level, pos);
        //?} else {
        /*?
        hardness = state.getDestroySpeed(robot.level(), pos);
        ?*/
        //?}
        speed = getBreakSpeed(robot.getItemBySlot(EquipmentSlot.MAINHAND), state);
    }

    private float getBreakSpeed(ItemStack stack, BlockState state) {
        float result = stack.isEmpty() ? 1.0F : stack.getDestroySpeed(state);
        if (result > 1.0F && !stack.isEmpty()) {
            int efficiency = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, stack);
            if (efficiency > 0) {
                result += efficiency * efficiency + 1;
            }
        }
        return Math.max(0.0F, result);
    }

    @Override
    public void end() {
        robot.setItemActive(false);
        //? if <1.20 {
        if (blockToBreak != null && robot.level instanceof ServerLevel serverLevel) {
        //?} else {
        /*?
        if (blockToBreak != null && robot.level() instanceof ServerLevel serverLevel) {
        ?*/
        //?}
            serverLevel.destroyBlockProgress(robot.getId(), blockToBreak.toBlockPos(), -1);
        }
    }

    @Override
    public int getEnergyCost() {
        return 8;
    }

    @Override
    public boolean canLoadFromNBT() {
        return true;
    }

    @Override
    public void writeSelfToNBT(CompoundTag nbt) {
        if (blockToBreak != null) {
            CompoundTag tag = new CompoundTag();
            blockToBreak.writeTo(tag);
            nbt.put("blockToBreak", tag);
        }
    }

    @Override
    public void loadSelfFromNBT(CompoundTag nbt) {
        if (nbt.contains("blockToBreak")) {
            blockToBreak = new BlockIndex(nbt.getCompound("blockToBreak"));
        }
    }
}
