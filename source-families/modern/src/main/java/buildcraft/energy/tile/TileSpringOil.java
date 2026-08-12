package buildcraft.energy.tile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import buildcraft.lib.internal.tiles.IDebuggable;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.lib.misc.AdvancementUtil;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// We don't extend TileBC here because we have no need of any of its functions.
public class TileSpringOil extends BlockEntity implements IDebuggable, ITileOilSpring {

	private static final ResourceLocation ADVANCEMENT_PUMP_LARGE_OIL_WELL = ResourceLocation.parse("buildcraftfactory:black_gold");

    private final Map<GameProfile, PlayerPumpInfo> pumpProgress = new ConcurrentHashMap<>();

    /** An approximation of the total number of oil source blocks in the oil spring. The actual number will be less than
     * this, so this is taken as an approximation.
     * <p>
     * Note that this SHOULD NEVER be set! (Except by the generator, and readFromNbt) */
    public int totalSources;

    public TileSpringOil(BlockPos pos, BlockState state) {
		super(BCEnergyBlocks.TILE_SPRING.get(), pos, state);
	}
    
    @Override
    public void onPumpOil(GameProfile profile, BlockPos oilPos) {
        if (profile == null) {
            // BCLog.logger.warn("Unknown owner for pump at " + pump.getPos());
            return;
        }
        PlayerPumpInfo info = pumpProgress.computeIfAbsent(profile, PlayerPumpInfo::new);
        info.lastPumpTick = level.getGameTime();
        info.sourcesPumped++;

        // BCLog.logger.info("Pumped " + info.sourcesPumped + " / " + totalSources + " at " + oilPos + " (for " +
        // System.identityHashCode(this) + ", "+getPos()+")");
        if (info.sourcesPumped >= totalSources * 7 / 8) {
            // BCLog.logger.info("Pumped nearly all oil blocks!");
            if (oilPos.equals(getBlockPos().above())) {
                AdvancementUtil.unlockAdvancement(profile.getId(), ADVANCEMENT_PUMP_LARGE_OIL_WELL);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        totalSources = nbt.getInt("totalSources");
        ListTag list = nbt.getList("pumpProgress", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            PlayerPumpInfo info = new PlayerPumpInfo(list.getCompound(i));
            if (info.profile != null) {
                pumpProgress.put(info.profile, info);
            }
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.putInt("totalSources", totalSources);
        ListTag list = new ListTag();
        int i = 0;
        for (PlayerPumpInfo info : pumpProgress.values()) {
            list.add(i++, info.writeToNbt());
        }
        nbt.put("pumpProgress", list);
    }

    @Override
    public void getDebugInfo(List<String> left, List<String> right, Direction side) {
        left.add("totalSources = " + totalSources);
        boolean added = false;
        for (PlayerPumpInfo info : pumpProgress.values()) {
            if (!added) {
                left.add("Player Progress:");
                added = true;
            }
            left.add("  " + info.profile.getName() + " = " + info.sourcesPumped + " ( "
                + (level.getGameTime() - info.lastPumpTick) / 20 + "s )");
        }
    }

    static class PlayerPumpInfo {
        final GameProfile profile;
        long lastPumpTick = -1;
        int sourcesPumped = 0;

        public PlayerPumpInfo(GameProfile profile) {
            this.profile = profile;
        }

        public PlayerPumpInfo(CompoundTag nbt) {
            profile = readGameProfile(nbt.getCompound("profile"));
            lastPumpTick = nbt.getLong("lastPumpTick");
            sourcesPumped = nbt.getInt("sourcesPumped");
        }

        public CompoundTag writeToNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.put("profile", writeGameProfile(profile));
            nbt.putLong("lastPumpTick", lastPumpTick);
            nbt.putInt("sourcesPumped", sourcesPumped);
            return nbt;
        }

        @Nullable
        private static GameProfile readGameProfile(CompoundTag nbt) {
            UUID id = nbt.hasUUID("Id") ? nbt.getUUID("Id") : null;
            String name = nbt.contains("Name", Tag.TAG_STRING) ? nbt.getString("Name") : null;
            if (id == null && (name == null || name.isBlank())) {
                return null;
            }
            return new GameProfile(id, name);
        }

        private static CompoundTag writeGameProfile(GameProfile profile) {
            CompoundTag nbt = new CompoundTag();
            if (profile.getId() != null) {
                nbt.putUUID("Id", profile.getId());
            }
            if (profile.getName() != null) {
                nbt.putString("Name", profile.getName());
            }
            return nbt;
        }
    }
}
