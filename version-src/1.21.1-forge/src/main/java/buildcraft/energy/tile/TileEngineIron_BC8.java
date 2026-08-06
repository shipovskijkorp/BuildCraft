/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.tile;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.core.IFluidFilter;
import buildcraft.api.core.IFluidHandlerAdv;
import buildcraft.api.fuels.BuildcraftFuelRegistry;
import buildcraft.api.fuels.IFuel;
import buildcraft.api.fuels.IFuelManager.IDirtyFuel;
import buildcraft.api.fuels.ISolidCoolant;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.api.transport.pipe.IItemPipe;
import buildcraft.core.client.render.RenderEngine_BC8;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.menu.ContainerEngineIron_BC8;
import buildcraft.lib.engine.EngineConnector;
import buildcraft.lib.engine.TileEngineBase_BC8;
import buildcraft.lib.fluid.FluidCompatRegistry;
import buildcraft.lib.fluid.Tank;
import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.misc.CapUtil;
import buildcraft.lib.misc.EntityUtil;
import buildcraft.lib.misc.FluidUtilBC;
import buildcraft.lib.misc.StackUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.LogicalSide;

public class TileEngineIron_BC8 extends TileEngineBase_BC8 implements MenuProvider{
    private static final ResourceLocation ADVANCEMENT_ICE_COOL =
        ResourceLocation.parse("buildcraftenergy:ice_cool");

    public static final int MAX_FLUID = 10_000;

    public static final double COOLDOWN_RATE = 0.05;
    public static final int MAX_COOLANT_PER_TICK = 40;

    private boolean solidCoolantLoaded;

    public final Tank tankFuel = new Tank("fuel", MAX_FLUID, this, this::isValidFuel);
    public final Tank tankCoolant = new Tank("coolant", MAX_FLUID, this, this::isValidCoolant) {
        @Override
        protected FluidGetResult map(ItemStack stack, int space) {
            ISolidCoolant coolant = BuildcraftFuelRegistry.coolant.getSolidCoolant(stack);
            if (coolant == null) {
                return super.map(stack, space);
            }
            FluidStack fluidCoolant = coolant.getFluidFromSolidCoolant(stack);
            if (fluidCoolant == null || fluidCoolant.getAmount() <= 0 || fluidCoolant.getAmount() > space) {
                return super.map(stack, space);
            }
            return new FluidGetResult(StackUtil.EMPTY, fluidCoolant);
        }

        @Override
        public ItemStack transferStackToTank(Player player, ItemStack stack) {
            boolean isSolidCoolant = BuildcraftFuelRegistry.coolant.getSolidCoolant(stack) != null;
            int amountBefore = getFluidAmount();
            ItemStack result = super.transferStackToTank(player, stack);
            if (!player.level().isClientSide && isSolidCoolant && getFluidAmount() > amountBefore) {
                solidCoolantLoaded = true;
            }
            return result;
        }

        @Override
        protected void onContentsChanged() {
            super.onContentsChanged();
            if (getFluidAmount() <= 0) {
                solidCoolantLoaded = false;
            }
        }
    };
    public final Tank tankResidue = new Tank("residue", MAX_FLUID, this, this::isResidue);
    private final IFluidHandlerAdv fluidHandler = new InternalFluidHandler();

    private int penaltyCooling = 0;
    private boolean lastPowered = false;
    private double burnTime;
    private double residueAmount = 0;
    private IFuel currentFuel;

    public TileEngineIron_BC8(BlockPos pos, BlockState state) {
        super(BCEnergyBlocks.ENGINE_IRON_TILE_BC8.get(), pos, state);
        tankManager.addAll(tankFuel, tankCoolant, tankResidue);

        // TODO: Auto list of example fuels!
/*        tankFuel.helpInfo = new ElementHelpInfo(tankFuel.helpInfo.title, 0xFF_FF_33_33, Tank.DEFAULT_HELP_KEY, null,
            "buildcraft.help.tank.fuel");

        // TODO: Auto list of example coolants!
        tankCoolant.helpInfo = new ElementHelpInfo(tankCoolant.helpInfo.title, 0xFF_55_55_FF, Tank.DEFAULT_HELP_KEY,
            null, "buildcraft.help.tank.coolant");

        tankResidue.helpInfo = new ElementHelpInfo(tankResidue.helpInfo.title, 0xFF_AA_33_AA, Tank.DEFAULT_HELP_KEY,
            null, "buildcraft.help.tank.residue");
*/
        caps.addCapabilityInstance(CapUtil.CAP_FLUIDS, fluidHandler, EnumPipePart.VALUES);
    }

    // BlockEntity overrides

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.put("tank", tankManager.serializeNBT(registries));
        nbt.putInt("penaltyCooling", penaltyCooling);
        nbt.putDouble("burnTime", burnTime);
        nbt.putDouble("residueAmount", residueAmount);
        nbt.putBoolean("solidCoolantLoaded", solidCoolantLoaded);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        tankManager.deserializeNBT(registries, nbt.getCompound("tank"));
        penaltyCooling = nbt.getInt("penaltyCooling");
        burnTime = nbt.getDouble("burnTime");
        residueAmount = nbt.getDouble("residueAmount");
        if (!Double.isFinite(residueAmount) || residueAmount < 0) {
            residueAmount = 0;
        }
        solidCoolantLoaded = nbt.getBoolean("solidCoolantLoaded") && tankCoolant.getFluidAmount() > 0;
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, CustomPayloadEvent.Context ctx) throws IOException {
        super.readPayload(id, buffer, side, ctx);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_GUI_DATA || id == NET_GUI_TICK) {
                tankManager.readData(buffer);
            }
        }
    }

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER) {
            if (id == NET_GUI_DATA || id == NET_GUI_TICK) {
                tankManager.writeData(buffer);
            }
        }
    }

    // TileEngineBase overrides

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack current = player.getItemInHand(hand).copy();
        if (FluidUtilBC.onTankActivated(player, worldPosition, hand, fluidHandler)) {
            return InteractionResult.SUCCESS;
        }
        if (!current.isEmpty()) {
            if (EntityUtil.getWrenchHand(player) != null) {
                return InteractionResult.PASS;
            }
            if (current.getItem() instanceof IItemPipe) {
                return InteractionResult.PASS;
            }
        }
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this, buffer -> buffer.writeBlockPos(worldPosition));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public double getPistonSpeed() {
        switch (getPowerStage()) {
            case BLUE:
                return 0.04;
            case GREEN:
                return 0.05;
            case YELLOW:
                return 0.06;
            case RED:
                return 0.07;
            default:
                return 0;
        }
    }

    @Nonnull
    @Override
    protected IMjConnector createConnector() {
        return new EngineConnector(false);
    }

    @Override
    protected boolean canUnlockPoweringUpAdvancement() {
        return true;
    }

    /**
     * Restores the BC7 combustion-engine climate modifier. Hot biomes make fuel heat the engine faster and reduce
     * coolant efficiency; cold biomes do the opposite. A biome temperature of 1.0 keeps the original 1.0 scalar.
     */
    private float getBiomeTempScalar() {
        float temperature = getBiome().getBaseTemperature();
        // Modded biomes may use temperatures below -1 or extreme positive values. Keep cooling finite and positive.
        return Mth.clamp(((temperature - 1.0F) * 0.5F) + 1.0F, 0.1F, 4.0F);
    }

    @Override
    public boolean isBurning() {
        FluidStack fuel = tankFuel.getFluid();
        return fuel != null && fuel.getAmount() > 0 && penaltyCooling == 0 && isRedstonePowered;
    }

    @Override
    protected void burn() {
        final FluidStack fuel = this.tankFuel.getFluid();
        if (currentFuel == null || !FluidCompatRegistry.areEquivalent(currentFuel.getFluid(), fuel)) {
            currentFuel = BuildcraftFuelRegistry.fuel.getFuel(fuel);
        }

        if (fuel.isEmpty() || currentFuel == null) {
            return;
        }

        if (penaltyCooling <= 0) {
            if (isRedstonePowered) {
                lastPowered = true;

                if (burnTime > 0 || fuel.getAmount() > 0) {
                    if (burnTime > 0) {
                        burnTime--;
                    }
                    if (burnTime <= 0) {
                        if (fuel.getAmount() > 0) {
                            fuel.setAmount(fuel.getAmount() - 1);
                            burnTime += currentFuel.getTotalBurningTime() / 1000.0;

                            // If we also produce residue then put it out too
                            if (currentFuel instanceof IDirtyFuel) {
                                IDirtyFuel dirtyFuel = (IDirtyFuel) currentFuel;
                                FluidStack residueFluid = dirtyFuel.getResidue().copy();
                                residueAmount += residueFluid.getAmount() / 1000.0;
                                if (residueAmount >= 1) {
                                    residueFluid.setAmount(Mth.floor(residueAmount));
                                    residueAmount -= tankResidue.fill(residueFluid, FluidAction.EXECUTE);
                                } else if (tankResidue.getFluid().isEmpty()) {
                                    residueFluid.setAmount(0);
                                    tankResidue.setFluid(residueFluid);
                                }
                            }
                        } else {
                            tankFuel.setFluid(FluidStack.EMPTY);
                            currentFuel = null;
                            currentOutput = 0;
                            return;
                        }
                    }
                    currentOutput = currentFuel.getPowerPerCycle(); // Comment out for constant power
                    addPower(currentFuel.getPowerPerCycle());
                    heat += currentFuel.getPowerPerCycle() * HEAT_PER_MJ / MjAPI.MJ * getBiomeTempScalar();
                }
            } else if (lastPowered) {
                lastPowered = false;
                penaltyCooling = 10;
                // 10 tick of penalty on top of the cooling
            }
        }

        if (burnTime <= 0 && fuel.getAmount() <= 0) {
            tankFuel.setFluid(FluidStack.EMPTY);
        }
    }

    @Override
    public void updateHeatLevel() {
        double target;
        if (heat > MIN_HEAT && (penaltyCooling > 0 || !isRedstonePowered)) {
            heat -= COOLDOWN_RATE;
            target = MIN_HEAT;
        } else if (heat > IDEAL_HEAT) {
            target = IDEAL_HEAT;
        } else {
            target = heat;
        }

        if (target != heat) {
            // coolEngine(target)
            {
                double coolingBuffer = 0;
                double extraHeat = heat - target;

                if (extraHeat > 0) {
                    // fillCoolingBuffer();
                    {
                        if (tankCoolant.getFluidAmount() > 0) {
                            FluidStack coolant = tankCoolant.getFluid();
                            float coolPerMb = BuildcraftFuelRegistry.coolant.getDegreesPerMb(coolant, (float) heat);
                            if (coolPerMb > 0) {
                                boolean alternativeCoolant = solidCoolantLoaded
                                    || !FluidCompatRegistry.areEquivalent(coolant.getFluid(), Fluids.WATER);
                                double coolingPerMb = coolPerMb / getBiomeTempScalar();
                                int requiredCoolant = Math.max(1, Mth.ceil(extraHeat / coolingPerMb));
                                int coolantAmount = Math.min(
                                    Math.min(MAX_COOLANT_PER_TICK, tankCoolant.getFluidAmount()), requiredCoolant
                                );
                                FluidStack drained = tankCoolant.drain(coolantAmount, FluidAction.EXECUTE);
                                if (!drained.isEmpty()) {
                                    coolingBuffer += Math.min(extraHeat, drained.getAmount() * coolingPerMb);
                                    if (alternativeCoolant && getOwner() != null) {
                                        AdvancementUtil.unlockAdvancement(getOwner().getId(), ADVANCEMENT_ICE_COOL);
                                    }
                                }
                            }
                        }
                    }
                    // end
                }

                // if (coolingBuffer >= extraHeat) {
                // coolingBuffer -= extraHeat;
                // heat -= extraHeat;
                // return;
                // }

                heat -= coolingBuffer;
                coolingBuffer = 0.0f;
            }
            // end
            getPowerStage();
        }

        if (heat <= MIN_HEAT && penaltyCooling > 0) {
            penaltyCooling--;
        }

        if (heat <= MIN_HEAT) {
            heat = MIN_HEAT;
        }
    }

    @Override
    public boolean isActive() {
        return penaltyCooling <= 0;
    }

    @Override
    public long getMaxPower() {
        return 10_000 * MjAPI.MJ;
    }

    @Override
    public long maxPowerReceived() {
        return 2_000 * MjAPI.MJ;
    }

    @Override
    public long maxPowerExtracted() {
        return 500 * MjAPI.MJ;
    }

    @Override
    protected boolean shouldExplodeOnOverheat() {
        return true;
    }

    @Override
    public float explosionRange() {
        return 4;
    }

    @Override
    protected int getMaxChainLength() {
        return 4;
    }

    @Override
    public long getCurrentOutput() {
        if (currentFuel == null) {
            return 0;
        } else {
            return currentFuel.getPowerPerCycle();
        }
    }

    // Fluid related

    private boolean isValidFuel(FluidStack fluid) {
        return BuildcraftFuelRegistry.fuel.getFuel(fluid) != null;
    }

    private boolean isValidCoolant(FluidStack fluid) {
        return BuildcraftFuelRegistry.coolant.getCoolant(fluid) != null;
    }

    private boolean isResidue(FluidStack fluid) {
        // If this is the client then we don't have a current fuel- just trust the server that its correct
        if (level != null && level.isClientSide) {
            return true;
        }
        if (currentFuel instanceof IDirtyFuel) {
            return FluidCompatRegistry.areEquivalent(fluid, ((IDirtyFuel) currentFuel).getResidue());
        }
        return false;
    }

    private class InternalFluidHandler implements IFluidHandlerAdv {

        @Override
        public int fill(FluidStack resource, FluidAction doFill) {
            int filled = tankFuel.fill(resource, doFill);
            if (filled == 0) {
                filled = tankCoolant.fill(resource, doFill);
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction doDrain) {
            return tankResidue.drain(resource, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction doDrain) {
            return tankResidue.drain(maxDrain, doDrain);
        }

        @Override
        public int getTanks() {
            return 3;
        }
        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> tankFuel.getFluid();
                case 1 -> tankCoolant.getFluid();
                case 2 -> tankResidue.getFluid();
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank >= 0 && tank < getTanks() ? MAX_FLUID : 0;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return switch (tank) {
                case 0 -> isValidFuel(stack);
                case 1 -> isValidCoolant(stack);
                case 2 -> isResidue(stack);
                default -> false;
            };
        }

        @Override
        public FluidStack drain(IFluidFilter filter, int maxDrain, FluidAction doDrain) {
            if(filter.matches(tankResidue.getFluid()))
                return drain(maxDrain, doDrain);
            else return FluidStack.EMPTY;
        }
    }

    @Override
    public TextureAtlasSprite getTextureBack() {
        return RenderEngine_BC8.IRON_BACK;
    }

    @Override
    public TextureAtlasSprite getTextureSide() {
        return RenderEngine_BC8.IRON_SIDE;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerIncentory, Player player) {
        return new ContainerEngineIron_BC8(id, playerIncentory, ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.buildcraftcore.engine_"+this.getBlockState().getValue(BuildCraftProperties.ENGINE_TYPE).getSerializedName());
    }



}
