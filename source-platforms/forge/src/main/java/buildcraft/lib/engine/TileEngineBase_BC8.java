/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.engine;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftServices;
import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.content.BuildCraftContentIds;
import buildcraft.api.v2.energy.MjAmount;
import buildcraft.api.v2.energy.MjConnectionContext;
import buildcraft.api.v2.energy.MjPort;
import buildcraft.api.v2.energy.MjPortDescriptor;
import buildcraft.api.v2.energy.MjPortProvider;
import buildcraft.api.v2.energy.MjPortRole;
import buildcraft.api.v2.energy.MjTransferResult;
import buildcraft.api.v2.machine.EngineStage;
import buildcraft.api.v2.machine.EngineView;
import buildcraft.api.v2.machine.MachineComponent;
import buildcraft.api.v2.machine.MachineControl;
import buildcraft.api.v2.machine.WorkState;
import buildcraft.api.v2.machine.WorkStatus;
import buildcraft.lib.internal.mj.MjCapabilities;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;
import buildcraft.lib.internal.enums.EnumEngineType;
import buildcraft.lib.internal.enums.EnumPowerStage;
import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.lib.internal.mj.IMjConnector;
import buildcraft.lib.internal.mj.MjCapabilityHelper;
import buildcraft.lib.internal.tiles.IDebuggable;
import buildcraft.core.client.model.ModelEngine;
import buildcraft.lib.block.VanillaRotationHandlers;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.misc.collect.OrderedEnumMap;
import buildcraft.lib.tile.TileBC_Neptune;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

public abstract class TileEngineBase_BC8 extends TileBC_Neptune implements IDebuggable, EngineView, MjPortProvider {

    private static final ResourceLocation ADVANCEMENT_POWERING_UP =
        new ResourceLocation("buildcraftenergy:powering_up");
    private static final ResourceLocation MJ_NETWORK_ID = new ResourceLocation("buildcraft", "mj");

    private final MjPort api2OutputPort = new MjPort() {
        @Override
        public MjTransferResult insert(MjAmount offered, OperationMode mode) {
            return MjTransferResult.none(offered);
        }

        @Override
        public MjTransferResult extract(MjAmount requested, OperationMode mode) {
            long requestedMicro = Math.max(0L, requested.microMj());
            long extracted = extractPower(0L, requestedMicro, mode == OperationMode.EXECUTE);
            return MjTransferResult.of(requested, MjAmount.ofMicro(extracted));
        }

        @Override
        public MjAmount stored() {
            return MjAmount.ofMicro(Math.max(0L, power));
        }

        @Override
        public MjAmount capacity() {
            return MjAmount.ofMicro(Math.max(0L, getMaxPower()));
        }

        @Override public boolean canInsert() { return false; }
        @Override public boolean canExtract() { return true; }
    };

    /** Heat per {@link MjAmount#MICRO_MJ_PER_MJ}. */
    public static final double HEAT_PER_MJ = 0.0023;

    public static final double MIN_HEAT = 20;
    public static final double IDEAL_HEAT = 100;
    public static final double MAX_HEAT = 250;

    @Nonnull
    public final IMjConnector mjConnector = createConnector();
    private final MjCapabilityHelper mjCaps = new MjCapabilityHelper(mjConnector);

    protected double heat = MIN_HEAT;
    protected long power = 0;
    private long lastPower = 0;
    /** Increments from 0 to 1. Above 0.5 all of the held power is emitted. */
    public float progress;

	public float RenderProgress;
    private int progressPart = 0;

    protected EnumPowerStage powerStage = EnumPowerStage.BLUE;
    protected Direction currentDirection = Direction.UP;

    public long currentOutput;
    public boolean isRedstonePowered = false;
    protected boolean isPumping = false;

    boolean movingState;
    private boolean wasOperationalForAdvancement;
    private long lastPersistenceMarkTick = Long.MIN_VALUE;
    private double persistedHeat = Double.NaN;
    private long persistedPower = Long.MIN_VALUE;
    private float persistedProgress = Float.NaN;
    private int persistedProgressPart = Integer.MIN_VALUE;
    private Direction persistedDirection;
    private boolean persistedRedstonePowered;

    // Needed: Power stored

    public TileEngineBase_BC8(BlockEntityType<?> bet, BlockPos pos, BlockState state) {
    	super(bet, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        syncRenderProgressFromProgress();
        if (level != null && level.isClientSide) {
            refreshEngineModelData();
        }
    }

    private void refreshEngineModelData() {
        if (level == null) {
            return;
        }
        requestModelDataUpdate();
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    private void syncRenderProgressFromProgress() {
        RenderProgress = computeRenderProgress(progress);
    }

    private static float computeRenderProgress(float progress) {
        return progress > 0.5F ? ((1 - progress) * (8 * 2 - 0.01F)) * 0.125F : (progress * (8 * 2 - 0.01F) * 0.125F);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        currentDirection = NBTUtilBC.readEnum(nbt.get("currentDirection"), Direction.class);
        if (currentDirection == null) {
            currentDirection = Direction.UP;
        }
        isRedstonePowered = nbt.getBoolean("isRedstonePowered");
        heat = nbt.getDouble("heat");
        power = nbt.getLong("power");
        progress = nbt.getFloat("progress");
        progressPart = nbt.getInt("progressPart");
        syncRenderProgressFromProgress();
        capturePersistedState();
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("currentDirection", NBTUtilBC.writeEnum(currentDirection));
        nbt.putBoolean("isRedstonePowered", isRedstonePowered);
        nbt.putDouble("heat", heat);
        nbt.putLong("power", power);
        nbt.putFloat("progress", progress);
        nbt.putInt("progressPart", progressPart);
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, NetworkEvent.Context ctx) throws IOException {
        super.readPayload(id, buffer, side, ctx);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_RENDER_DATA) {
                isPumping = buffer.readBoolean();
                Direction newDir = buffer.readEnum(Direction.class);
                if (newDir == null) {
                    newDir = Direction.UP;
                }
                boolean directionChanged = newDir != currentDirection;
                currentDirection = newDir;
                powerStage = buffer.readEnum(EnumPowerStage.class);
                progress = buffer.readFloat();
                syncRenderProgressFromProgress();
                if (directionChanged) {
                    refreshEngineModelData();
                }
            } else if (id == NET_GUI_DATA) {
                heat = buffer.readFloat();
                currentOutput = buffer.readLong();
                power = buffer.readLong();
            } else if (id == NET_GUI_TICK) {
                heat = buffer.readFloat();
                currentOutput = buffer.readLong();
                power = buffer.readLong();

            }
        }
    }

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER) {
            if (id == NET_RENDER_DATA) {
                buffer.writeBoolean(isPumping);
                buffer.writeEnum(currentDirection);
                buffer.writeEnum(powerStage);
                buffer.writeFloat(progress);
            } else if (id == NET_GUI_DATA) {
                buffer.writeFloat((float) heat);
                buffer.writeLong(currentOutput);
                buffer.writeLong(power);
            } else if (id == NET_GUI_TICK) {
                buffer.writeFloat((float) heat);
                buffer.writeLong(currentOutput);
                buffer.writeLong(power);

            }
        }
    }

    public InteractionResult attemptRotation() {
        OrderedEnumMap<Direction> possible = VanillaRotationHandlers.ROTATE_FACING;
        Direction current = currentDirection;
        for (int i = 0; i < 6; i++) {
            current = possible.next(current);
            if (isFacingReceiver(current)) {
                if (currentDirection != current) {
                    currentDirection = current;
                    // makeTileCache();
                    sendNetworkUpdate(NET_RENDER_DATA);
                    redrawBlock();
                    markChunkDirty();
                    capturePersistedState();
//                    world.notifyNeighborsRespectDebug(getPos(), getBlockType(), true);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.FAIL;
    }

    protected boolean isFacingReceiver(Direction dir) {
        return getPortToPower(dir) != null;
    }

    protected final boolean canChain() {
        return getMaxChainLength() > 0;
    }

    /** @return The number of additional engines that this engine can send power through. */
    protected int getMaxChainLength() {
        return 2;
    }

    public void rotateIfInvalid() {
        if (currentDirection != null && isFacingReceiver(currentDirection)) {
            return;
        }
        attemptRotation();
        if (currentDirection == null) {
            currentDirection = Direction.UP;
        }
    }

    @Override
    public void onPlacedBy(LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        currentDirection = null;// Force rotateIfInvalid to always attempt to rotate
        rotateIfInvalid();
    }

    protected Biome getBiome() {
        // TODO: Cache this!
        return level.getBiome(worldPosition).value();
    }

    /** @return The heat of the current biome, in celsius. */
    protected float getBiomeHeat() {
        float temperature = getBiome().getBaseTemperature();
        return Math.max(0, Math.min(30, temperature * 15f));
    }

    public double getPowerLevel() {
        return power / (double) getMaxPower();
    }

    protected EnumPowerStage computePowerStage() {
        double heatLevel = getHeatLevel();
        if (heatLevel < 0.25f) return EnumPowerStage.BLUE;
        else if (heatLevel < 0.5f) return EnumPowerStage.GREEN;
        else if (heatLevel < 0.75f) return EnumPowerStage.YELLOW;
        else if (heatLevel < 0.85f) return EnumPowerStage.RED;
        else return EnumPowerStage.OVERHEAT;
    }

    public final EnumPowerStage getPowerStage() {
        if (!level.isClientSide) {
            EnumPowerStage newStage = computePowerStage();

            if (powerStage != newStage) {
                powerStage = newStage;
                sendNetworkUpdate(NET_RENDER_DATA);
            }
        }

        return powerStage;
    }

    public void updateHeatLevel() {
        heat = ((MAX_HEAT - MIN_HEAT) * getPowerLevel()) + MIN_HEAT;
    }

    public double getHeatLevel() {
        return (heat - MIN_HEAT) / (MAX_HEAT - MIN_HEAT);
    }

    public double getIdealHeatLevel() {
        return heat / IDEAL_HEAT;
    }

    public double getHeat() {
        return heat;
    }

    public double getPistonSpeed() {
        switch (getPowerStage()) {
            case BLUE:
                return 0.02;
            case GREEN:
                return 0.04;
            case YELLOW:
                return 0.08;
            case RED:
                return 0.12;
            default:
                return 0;
        }
    }

    @Nonnull
    protected abstract IMjConnector createConnector();

    @Override
    public void neighbourBlockChanged(BlockState state, BlockPos nehighbour, boolean a) {
    	super.onNeighbourBlockChanged(state, nehighbour);
        isRedstonePowered = level.hasNeighborSignal(worldPosition);
        
    }

    public void update() {
        deltaManager.tick();
        if (cannotUpdate()) return;

        boolean overheat = getPowerStage() == EnumPowerStage.OVERHEAT;

        if (level.isClientSide) {

            if (isPumping) {
                progress += getPistonSpeed();

                if (progress >= 1) {
                    progress = 0;
                }
            } else if (progress > 0) {
                progress -= 0.01f;
            }
            syncRenderProgressFromProgress();
//            clientModelData.tick();
            return;
        }

        lastPower = 0;

        if (!isRedstonePowered) {
            if (power > MjAmount.MICRO_MJ_PER_MJ) {
                power -= MjAmount.MICRO_MJ_PER_MJ;
            } else if (power > 0) {
                power = 0;
            }
        }

        updateHeatLevel();
        overheat = getPowerStage() == EnumPowerStage.OVERHEAT;
        engineUpdate();
        if (overheat && explodeIfOverheated()) {
            return;
        }

        if (progressPart != 0) {
            progress += getPistonSpeed();

            if (progress > 0.5 && progressPart == 1) {
                progressPart = 2;
            } else if (progress >= 1) {
                progress = 0;
                progressPart = 0;
            }
        } else if (isRedstonePowered && isActive()) {
            if (getPowerToExtract(false) > 0) {
                progressPart = 1;
                setPumping(true);
            } else {
                setPumping(false);
            }
        } else {
            setPumping(false);
        }

        // Power transfer is independent from the piston animation. Keeping it tied to the midpoint of a
        // stroke leaves usable energy frozen in the internal buffer between strokes and fuel items.
        if (isRedstonePowered && isActive()) {
            sendPower();
        }

        if (!overheat) {
            burn();
            if (explodeIfOverheated()) {
                return;
            }
        }

        boolean operationalForAdvancement = canUnlockPoweringUpAdvancement()
            && isRedstonePowered && isBurning();
        if (operationalForAdvancement && !wasOperationalForAdvancement && getOwner() != null) {
            AdvancementUtil.unlockAdvancement(getOwner().getId(), ADVANCEMENT_POWERING_UP);
        }
        wasOperationalForAdvancement = operationalForAdvancement;

        markPersistentStateIfNeeded();
    }

    private void markPersistentStateIfNeeded() {
        if (level == null || level.isClientSide || !hasPersistentStateChanged()) {
            return;
        }
        long now = level.getGameTime();
        if (lastPersistenceMarkTick == Long.MIN_VALUE || now - lastPersistenceMarkTick >= 20) {
            markChunkDirty();
            lastPersistenceMarkTick = now;
            capturePersistedState();
        }
    }

    private boolean hasPersistentStateChanged() {
        return Double.doubleToLongBits(heat) != Double.doubleToLongBits(persistedHeat)
            || power != persistedPower
            || Float.floatToIntBits(progress) != Float.floatToIntBits(persistedProgress)
            || progressPart != persistedProgressPart
            || currentDirection != persistedDirection
            || isRedstonePowered != persistedRedstonePowered;
    }

    private void capturePersistedState() {
        persistedHeat = heat;
        persistedPower = power;
        persistedProgress = progress;
        persistedProgressPart = progressPart;
        persistedDirection = currentDirection;
        persistedRedstonePowered = isRedstonePowered;
    }

    protected long getPowerToExtract(boolean doExtract) {
        MjPort receiver = getPortToPower(currentDirection);
        if (receiver == null) {
            return 0;
        }

        long available = extractPower(0, maxPowerExtracted(), false);
        if (available <= 0) return 0;
        MjAmount offered = MjAmount.ofMicro(available);
        long accepted = receiver.insert(offered, OperationMode.SIMULATE).transferred().microMj();
        accepted = Math.max(0L, Math.min(available, accepted));
        if (doExtract && accepted > 0) {
            extractPower(accepted, accepted, true);
        }
        return accepted;
    }

    protected void sendPower() {
        MjPort receiver = getPortToPower(currentDirection);
        if (receiver == null) {
            return;
        }

        long offered = getPowerToExtract(false);
        if (offered <= 0) {
            return;
        }

        long accepted = receiver.insert(MjAmount.ofMicro(offered), OperationMode.EXECUTE).transferred().microMj();
        accepted = Math.max(0L, Math.min(offered, accepted));
        if (accepted > 0) {
            extractPower(accepted, accepted, true);
        }
    }

    // Uncomment out for constant power
    // public float getActualOutput() {
    // float heatLevel = getIdealHeatLevel();
    // return getCurrentOutput() * heatLevel;
    // }
    protected void burn() {}

    /** Only combustion-style engines opt into destructive overheating. */
    protected boolean shouldExplodeOnOverheat() {
        return false;
    }

    private boolean explodeIfOverheated() {
        if (level == null || level.isClientSide || !shouldExplodeOnOverheat()
            || getPowerStage() != EnumPowerStage.OVERHEAT) {
            return false;
        }

        BlockPos pos = worldPosition.immutable();
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            //? if <1.20 {
            explosionRange(), net.minecraft.world.level.Explosion.BlockInteraction.BREAK);
            //?} else {
            /*?
            explosionRange(), net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
            ?*/
            //?}
        if (level.getBlockEntity(pos) == this) {
            level.removeBlock(pos, false);
        }
        return true;
    }

    /** Only fuel-burning engines participate in the Powering Up advancement. */
    protected boolean canUnlockPoweringUpAdvancement() {
        return false;
    }

    protected void engineUpdate() {
        if (!isRedstonePowered) {
            if (power >= 1) {
                power -= 1;
            } else if (power < 1) {
                power = 0;
            }
        }
    }

    public boolean isActive() {
        return true;
    }

    protected final void setPumping(boolean isActive) {
        if (this.isPumping == isActive) {
            return;
        }

        this.isPumping = isActive;
        sendNetworkUpdate(NET_RENDER_DATA);
    }

    @FunctionalInterface
    public interface ITileBuffer {
        BlockEntity getTile();
    }

    /** Temp! This should be replaced with a tile buffer! */
    public ITileBuffer getTileBuffer(Direction side) {
        BlockEntity tile = level.getBlockEntity(worldPosition.offset(side.getNormal()));
        return () -> tile;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // tileCache = null;
        // checkOrientation = true;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        // tileCache = null;
        // checkOrientation = true;
    }

    /* STATE INFORMATION */
    public abstract boolean isBurning();

    // IPowerReceptor stuffs -- move!
    // @Override
    // public PowerReceiver getPowerReceiver(ForgeDirection side) {
    // return powerHandler.getPowerReceiver();
    // }
    //
    // @Override
    // public void doWork(PowerHandler workProvider) {
    // if (worldObj.isRemote) {
    // return;
    // }
    //
    // addEnergy(powerHandler.useEnergy(1, maxEnergyReceived(), true) * 0.95F);
    // }

    public void addPower(long microJoules) {
        power += microJoules;
        lastPower += microJoules;

        if (power > getMaxPower()) {
            power = getMaxPower();
        }
    }

    public long extractPower(long min, long max, boolean doExtract) {
        if (power < min) {
            return 0;
        }

        long actualMax;

        if (max > maxPowerExtracted()) {
            actualMax = maxPowerExtracted();
        } else {
            actualMax = max;
        }

        if (actualMax < min) {
            return 0;
        }

        long extracted;

        if (power >= actualMax) {
            extracted = actualMax;

            if (doExtract) {
                power -= actualMax;
            }
        } else {
            extracted = power;

            if (doExtract) {
                power = 0;
            }
        }

        return extracted;
    }

    public final boolean isPoweredTile(BlockEntity tile, Direction side) {
        if (tile == null) return false;
        if (tile.getClass() == getClass()) {
            TileEngineBase_BC8 other = (TileEngineBase_BC8) tile;
            return other.currentDirection == currentDirection;
        }
        return BuildCraftApi.service(BuildCraftServices.ENERGY)
            .port(level, tile.getBlockPos(), side.getOpposite()).isPresent();
    }

    /** Returns the API2 MJ endpoint reached through a valid same-engine chain. */
    public MjPort getPortToPower(Direction side) {
        TileEngineBase_BC8 engine = this;
        BlockEntity next = null;

        for (int len = 0; len <= getMaxChainLength(); len++) {
            next = engine.getTileBuffer(side).getTile();
            if (next == null) return null;

            if (next.getClass() == getClass()) {
                if (side != ((TileEngineBase_BC8) next).currentDirection) return null;
            }

            if (next instanceof TileEngineBase_BC8) {
                if (next.getClass() != getClass()) return null;
                engine = (TileEngineBase_BC8) next;
            } else {
                break;
            }
        }

        if (next == null || next instanceof TileEngineBase_BC8) return null;
        var energy = BuildCraftApi.service(BuildCraftServices.ENERGY);
        MjPort remotePort = energy.port(level, next.getBlockPos(), side.getOpposite()).orElse(null);
        if (remotePort == null) return null;
        Optional<MjPortDescriptor> localDescriptor = engine.mjPortDescriptor(side);
        Optional<MjPortDescriptor> remoteDescriptor = energy.descriptor(level, next.getBlockPos(), side.getOpposite());
        if (localDescriptor.isPresent() && remoteDescriptor.isPresent()
            && !energy.canConnect(new MjConnectionContext(level, engine.worldPosition, side,
                localDescriptor.get(), remoteDescriptor.get()))) {
            return null;
        }
        return remotePort;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        if (facing == currentDirection) {
            return mjCaps.getCapability(capability, facing);
        } else {
            return super.getCapability(capability, facing);
        }
    }

    public abstract long getMaxPower();

    public long minPowerReceived() {
        return 2 * MjAmount.MICRO_MJ_PER_MJ;
    }

    public abstract long maxPowerReceived();

    public abstract long maxPowerExtracted();

    public abstract float explosionRange();

    public long getEnergyStored() {
        return power;
    }

    public abstract long getCurrentOutput();

    public boolean isEngineOn() {
        return isPumping;
    }

    @OnlyIn(Dist.CLIENT)
    public float getProgressClient(float partialTicks) {
        float last = RenderProgress;
        float now = progress;
        if (last > 0.5 && now < 0.5) {
            // we just returned
            now += 1;
        }
        float interp = last * (1 - partialTicks) + now * partialTicks;
        return interp % 1;
    }

    public Direction getCurrentFacing() {
        return currentDirection;
    }

    @Override
    public ResourceLocation typeId() {
        BlockState state = getBlockState();
        if (state.hasProperty(BuildCraftProperties.ENGINE_TYPE)) {
            EnumEngineType type = state.getValue(BuildCraftProperties.ENGINE_TYPE);
            return switch (type) {
                case WOOD -> BuildCraftContentIds.Engines.REDSTONE;
                case STONE -> BuildCraftContentIds.Engines.STONE;
                case IRON -> BuildCraftContentIds.Engines.IRON;
                case CREATIVE -> BuildCraftContentIds.Engines.CREATIVE;
                case FE -> BuildCraftContentIds.Engines.FE;
            };
        }
        return BuildCraftContentIds.Engines.REDSTONE;
    }

    @Override
    public BlockPos position() {
        return worldPosition;
    }

    @Override
    public WorkStatus workStatus() {
        WorkState state = !isRedstonePowered ? WorkState.PAUSED
            : (isPumping || isBurning() ? WorkState.RUNNING : WorkState.IDLE);
        double p = Math.max(0.0, Math.min(1.0, progress));
        return new WorkStatus(state, p, getPowerStage().name().toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public Collection<MachineComponent> components() {
        return List.of((MachineComponent) () -> BuildCraftContentIds.MachineComponents.ENERGY);
    }

    @Override
    public Optional<MachineControl> control() {
        return Optional.empty();
    }

    @Override
    public Optional<MjPort> mjPort(Direction side) {
        return side == currentDirection ? Optional.of(api2OutputPort) : Optional.empty();
    }

    @Override
    public Optional<MjPortDescriptor> mjPortDescriptor(Direction side) {
        if (side != currentDirection) return Optional.empty();
        return Optional.of(new MjPortDescriptor(
            MJ_NETWORK_ID,
            Set.of(MjPortRole.PROVIDER, MjPortRole.CONNECTOR, MjPortRole.READABLE),
            MjAmount.ZERO,
            MjAmount.ofMicro(Math.max(0L, maxPowerExtracted()))
        ));
    }

    @Override
    public EngineStage stage() {
        return switch (getPowerStage()) {
            case BLUE -> new EngineStage(BuildCraftContentIds.EngineStages.BLUE, 0);
            case GREEN -> new EngineStage(BuildCraftContentIds.EngineStages.GREEN, 1);
            case YELLOW -> new EngineStage(BuildCraftContentIds.EngineStages.YELLOW, 2);
            case RED -> new EngineStage(BuildCraftContentIds.EngineStages.RED, 3);
            case OVERHEAT -> new EngineStage(BuildCraftContentIds.EngineStages.OVERHEAT, 4);
            case BLACK -> new EngineStage(BuildCraftContentIds.EngineStages.BLACK, 5);
        };
    }

    @Override
    public MjAmount outputPerTick() {
        return MjAmount.ofMicro(Math.max(0L, getCurrentOutput()));
    }

    @Override
    public void getDebugInfo(List<String> left, List<String> right, Direction side) {
        left.add("facing = " + currentDirection);
        left.add("heat = " + (heat) + " -- " + String.format("%.2f %%", getHeatLevel()));
        left.add("power = " + (power));
        left.add("stage = " + powerStage);
        left.add("progress = " + progress);
        left.add("last = " + (lastPower));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void getClientDebugInfo(List<String> left, List<String> right, Direction side) {
        left.add("Current Model Variables:");
//        clientModelData.addDebugInfo(left);
    }
    
    @OnlyIn(Dist.CLIENT)
	public abstract TextureAtlasSprite getTextureBack();

    /**
     * Texture used by the moving piston face. Normal engines use their back texture; converters such as the
     * BuildCraft 8 MJ Dynamo can override this without duplicating the engine renderer.
     */
    @OnlyIn(Dist.CLIENT)
    public TextureAtlasSprite getTextureFront() {
        return getTextureBack();
    }

    @OnlyIn(Dist.CLIENT)
	public abstract TextureAtlasSprite getTextureSide();

	@Override
	public @NotNull ModelData getModelData() {
		return ModelData.builder().with(ModelEngine.EngineModelFacingKey, currentDirection).build();
	}
    
    

}
