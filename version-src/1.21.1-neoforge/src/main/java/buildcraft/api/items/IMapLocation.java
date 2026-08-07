package buildcraft.api.items;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import buildcraft.api.core.IBox;
import buildcraft.api.core.IZone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Created by asie on 2/28/15. */
public interface IMapLocation extends INamedItem {
    enum MapLocationType {
        CLEAN,
        SPOT,
        AREA,
        PATH,
        ZONE,
        /** Like PATH but repeats around in a loop. */
        PATH_REPEATING;

        public final int meta = ordinal();

        public static MapLocationType getFromStack(@Nonnull ItemStack stack) {
            CompoundTag tag = getCustomData(stack);
            if (tag != null && tag.contains("kind")) {
                int kind = tag.getByte("kind");
                return switch (kind) {
                    case 0 -> SPOT;
                    case 1 -> AREA;
                    case 2 -> PATH;
                    case 3 -> ZONE;
                    case 4 -> PATH_REPEATING;
                    default -> CLEAN;
                };
            }

            int dam = stack.getDamageValue();
            if (dam < 0 || dam >= values().length) {
                return MapLocationType.CLEAN;
            }
            return values()[dam];
        }

        public void setToStack(@Nonnull ItemStack stack) {
            stack.setDamageValue(meta);
            stack.set(DataComponents.MAX_STACK_SIZE, this == CLEAN ? 16 : 1);
            CompoundTag tag = getCustomData(stack);

            if (this == CLEAN) {
                if (tag != null) {
                    tag.remove("kind");
                    tag.remove("Damage");
                    setCustomData(stack, tag.isEmpty() ? null : tag);
                }
                return;
            }

            if (tag == null) {
                tag = new CompoundTag();
            }
            tag.putByte("kind", switch (this) {
                case SPOT -> (byte) 0;
                case AREA -> (byte) 1;
                case PATH -> (byte) 2;
                case ZONE -> (byte) 3;
                case PATH_REPEATING -> (byte) 4;
                case CLEAN -> (byte) -1;
            });
            setCustomData(stack, tag);
        }
    }

    /** This function can be used for SPOT types.
     * 
     * @param stack
     * @return The point representing the map location. */
    BlockPos getPoint(@Nonnull ItemStack stack);

    /** This function can be used for SPOT and AREA types.
     * 
     * @param stack
     * @return The box representing the map location. */
    IBox getBox(@Nonnull ItemStack stack);

    /** This function can be used for SPOT, AREA and ZONE types. The PATH type needs to be handled separately.
     * 
     * @param stack
     * @return An IZone representing the map location - also an instance of IBox for SPOT and AREA types. */
    IZone getZone(@Nonnull ItemStack stack);

    /** This function can be used for SPOT and PATH types.
     * 
     * @param stack
     * @return A list of BlockPoses representing the path the Map Location stores. */
    List<BlockPos> getPath(@Nonnull ItemStack stack);

    /** This function can be used for SPOT types only.
     * 
     * @param stack
     * @return The side of the spot. */
    Direction getPointSide(@Nonnull ItemStack stack);
    @Nullable
    private static CompoundTag getCustomData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    private static void setCustomData(ItemStack stack, @Nullable CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
        }
    }
}
