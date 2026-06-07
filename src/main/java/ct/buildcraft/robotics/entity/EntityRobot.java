package ct.buildcraft.robotics.entity;

import java.util.Collections;

import javax.annotation.Nullable;

import ct.buildcraft.api.boards.RedstoneBoardRobot;
import ct.buildcraft.api.core.IZone;
import ct.buildcraft.api.mj.MjBattery;
import ct.buildcraft.api.robots.DockingStation;
import ct.buildcraft.api.robots.EntityRobotBase;
import ct.buildcraft.api.robots.IRobotRegistry;
import ct.buildcraft.api.robots.RobotManager;
import ct.buildcraft.robotics.BCRoboticsBoards;
import ct.buildcraft.robotics.BCRoboticsBoards.BoardEntry;
import ct.buildcraft.robotics.BCRoboticsEntities;
import ct.buildcraft.robotics.item.ItemRobot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class EntityRobot extends EntityRobotBase implements IEntityAdditionalSpawnData {
    private static final ResourceLocation BASE_TEXTURE = new ResourceLocation("buildcraftrobotics", "textures/entities/robot_base.png");

    private final net.minecraft.core.NonNullList<ItemStack> inventory = net.minecraft.core.NonNullList.withSize(4, ItemStack.EMPTY);
    private final MjBattery battery = new MjBattery(MAX_ENERGY);
    private BoardEntry boardEntry = BCRoboticsBoards.EMPTY;
    private RedstoneBoardRobot board;
    private long robotId = NULL_ROBOT_ID;
    private DockingStation linkedStation;
    private DockingStation dockingStation;
    private DockingStation mainStation;
    private ItemStack itemInUse = ItemStack.EMPTY;
    private boolean itemActive;
    private float aimYaw;
    private float aimPitch;

    public EntityRobot(EntityType<? extends EntityRobot> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public EntityRobot(Level level, BoardEntry boardEntry) {
        this(BCRoboticsEntities.ROBOT.get(), level);
        setBoard(boardEntry);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 4.0D);
    }

    public void setBoard(BoardEntry entry) {
        this.boardEntry = entry == null ? BCRoboticsBoards.EMPTY : entry;
        this.board = this.boardEntry.nbt().create(this);
    }

    public BoardEntry getBoardEntry() {
        return boardEntry;
    }

    public ResourceLocation getTexture() {
        return boardEntry == null ? BASE_TEXTURE : boardEntry.robotTextureLocation();
    }

    public void setEnergy(int energy) {
        battery.extractAll();
        battery.addPower(Math.max(0, Math.min(MAX_ENERGY, energy)), IFluidHandler.FluidAction.EXECUTE);
    }

    public void setUniqueRobotId(long id) {
        this.robotId = id;
        IRobotRegistry registry = getRegistry();
        if (registry != null) {
            registry.registerRobot(this);
        }
    }

    public ItemStack asItemStack() {
        return ItemRobot.createRobotStack(boardEntry, getEnergy());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide && robotId != NULL_ROBOT_ID && getRegistry() != null) {
            getRegistry().registerRobot(this);
        }
        if (!level.isClientSide && dockingStation != null) {
            BlockPos pos = new BlockPos(dockingStation.x(), dockingStation.y(), dockingStation.z());
            double x = pos.getX() + 0.5D + (dockingStation.side() == null ? 0.0D : dockingStation.side().getStepX() * 0.5D);
            double y = pos.getY() + 0.5D + (dockingStation.side() == null ? 0.0D : dockingStation.side().getStepY() * 0.5D);
            double z = pos.getZ() + 0.5D + (dockingStation.side() == null ? 0.0D : dockingStation.side().getStepZ() * 0.5D);
            setPos(x, y, z);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("boardId", boardEntry.id());
        tag.putLong("robotId", robotId);
        tag.put("battery", battery.serializeNBT());
        tag.putBoolean("itemActive", itemActive);
        if (!itemInUse.isEmpty()) {
            tag.put("itemInUse", itemInUse.save(new CompoundTag()));
        }
        ContainerHelper.saveAllItems(tag, inventory);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        setBoard(BCRoboticsBoards.getById(tag.getString("boardId")));
        robotId = tag.contains("robotId") ? tag.getLong("robotId") : NULL_ROBOT_ID;
        battery.deserializeNBT(tag.getCompound("battery"));
        itemActive = tag.getBoolean("itemActive");
        itemInUse = tag.contains("itemInUse") ? ItemStack.of(tag.getCompound("itemInUse")) : ItemStack.EMPTY;
        ContainerHelper.loadAllItems(tag, inventory);
    }

    @Override
    public InteractionResult interactAt(Player player, net.minecraft.world.phys.Vec3 hitVec, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public int getContainerSize() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < inventory.size() ? inventory.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(inventory, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(inventory, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < inventory.size()) {
            inventory.set(slot, stack);
            setChanged();
        }
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return isAlive() && player.distanceToSqr(this) <= 64.0D;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < inventory.size(); i++) {
            inventory.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    public void setItemInUse(ItemStack stack) {
        itemInUse = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    @Override
    public void setItemActive(boolean active) {
        itemActive = active;
    }

    @Override
    public boolean isMoving() {
        return false;
    }

    @Override
    public DockingStation getLinkedStation() {
        return linkedStation;
    }

    @Override
    public RedstoneBoardRobot getBoard() {
        return board;
    }

    @Override
    public void aimItemAt(float yaw, float pitch) {
        aimYaw = yaw;
        aimPitch = pitch;
    }

    @Override
    public void aimItemAt(int x, int y, int z) {
        double dx = x + 0.5D - getX();
        double dy = y + 0.5D - getY();
        double dz = z + 0.5D - getZ();
        aimYaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        aimPitch = (float) (-(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0D / Math.PI));
    }

    @Override
    public float getAimYaw() {
        return aimYaw;
    }

    @Override
    public float getAimPitch() {
        return aimPitch;
    }

    @Override
    public MjBattery getBattery() {
        return battery;
    }

    @Override
    public DockingStation getDockingStation() {
        return dockingStation;
    }

    @Override
    public void dock(DockingStation station) {
        linkedStation = station;
        dockingStation = station;
        if (station != null) {
            station.setLevel(level);
            setPos(station.x() + 0.5D + (station.side() == null ? 0.0D : station.side().getStepX() * 0.5D),
                    station.y() + 0.5D + (station.side() == null ? 0.0D : station.side().getStepY() * 0.5D),
                    station.z() + 0.5D + (station.side() == null ? 0.0D : station.side().getStepZ() * 0.5D));
        }
    }

    @Override
    public void undock() {
        dockingStation = null;
    }

    @Override
    public IZone getZoneToWork() {
        return null;
    }

    @Override
    public IZone getZoneToLoadUnload() {
        return null;
    }

    @Override
    public boolean containsItems() {
        return !isEmpty();
    }

    @Override
    public boolean hasFreeSlot() {
        return inventory.stream().anyMatch(ItemStack::isEmpty);
    }

    @Override
    public void unreachableEntityDetected(Entity entity) {
    }

    @Override
    public boolean isKnownUnreachable(Entity entity) {
        return false;
    }

    @Override
    public long getRobotId() {
        return robotId;
    }

    @Override
    public IRobotRegistry getRegistry() {
        return RobotManager.registryProvider == null ? null : RobotManager.registryProvider.getRegistry(level);
    }

    @Override
    public void releaseResources() {
        IRobotRegistry registry = getRegistry();
        if (registry != null) registry.releaseResources(this);
    }

    @Override
    public void onChunkUnload() {
        IRobotRegistry registry = getRegistry();
        if (registry != null) registry.unloadRobot(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        IRobotRegistry registry = getRegistry();
        if (!level.isClientSide && registry != null) {
            registry.killRobot(this);
        }
        super.remove(reason);
    }

    @Override
    public ItemStack receiveItem(BlockEntity blockEntity, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).isEmpty()) {
                inventory.set(i, stack.copy());
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    @Override
    public void setMainStation(DockingStation station) {
        mainStation = station;
        linkedStation = station;
    }

    public DockingStation getMainStation() {
        return mainStation;
    }


    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeUtf(boardEntry == null ? BCRoboticsBoards.EMPTY.id() : boardEntry.id());
        buffer.writeVarInt(getEnergy());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        setBoard(BCRoboticsBoards.getById(buffer.readUtf(32767)));
        setEnergy(buffer.readVarInt());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return (Packet<ClientGamePacketListener>) (Packet<?>) NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public int getTanks() {
        return 0;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
