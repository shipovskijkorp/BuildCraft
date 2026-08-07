/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */
package buildcraft.core.statements;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.core.render.ISprite;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementMouseClick;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import buildcraft.lib.misc.StackUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Directions might eventually be replaced by individual statements per side. */
@Deprecated
public class StatementParameterDirection implements IStatementParameter {
    @Nullable
    private Direction direction;

    public StatementParameterDirection() {
    }

    public StatementParameterDirection(@Nullable Direction face) {
        this.direction = face;
    }

    @Nullable
    public Direction getDirection() {
        return direction;
    }

    @Nonnull
    @Override
    public ItemStack getItemStack() {
        return StackUtil.EMPTY;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ISprite getSprite() {
        return null;
    }

    @Override
    public IStatementParameter onClick(IStatementContainer source, IStatement stmt, ItemStack stack,
        StatementMouseClick mouse) {
        return null;
    }

    @Override
    public void writeToNbt(CompoundTag nbt) {
        if (direction != null) {
            nbt.putByte("direction", (byte) direction.ordinal());
        }
    }

    public void readFromNBT(CompoundTag nbt) {
        if (nbt.contains("direction")) {
            direction = Direction.values()[nbt.getByte("direction")];
        } else {
            direction = null;
        }
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof StatementParameterDirection parameter && parameter.direction == direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(direction);
    }

    @Override
    public Component getDescription() {
        return direction == null ? Component.empty()
            : Component.translatable("direction." + direction.getName());
    }

    @Override
    public String getUniqueTag() {
        return "buildcraft:pipeActionDirection";
    }

    @Override
    public IStatementParameter rotateLeft() {
        StatementParameterDirection rotated = new StatementParameterDirection();
        Direction dir = rotated.getDirection();
        if (dir != null && dir.getAxis() != Axis.Y) {
            rotated.direction = dir.getClockWise();
        }
        return rotated;
    }

    @Override
    public IStatementParameter[] getPossible(IStatementContainer source) {
        IStatementParameter[] possible = new IStatementParameter[7];
        for (EnumPipePart part : EnumPipePart.VALUES) {
            possible[part.getIndex()] = part.face == direction ? this : new StatementParameterDirection(part.face);
        }
        return possible;
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientSprites {
        private static final ISprite[] SPRITES = {
            SpriteHolderRegistry.getHolder("buildcraftcore:triggers/trigger_dir_down"),
            SpriteHolderRegistry.getHolder("buildcraftcore:triggers/trigger_dir_up"),
            SpriteHolderRegistry.getHolder("buildcraftcore:triggers/trigger_dir_north"),
            SpriteHolderRegistry.getHolder("buildcraftcore:triggers/trigger_dir_south"),
            SpriteHolderRegistry.getHolder("buildcraftcore:triggers/trigger_dir_west"),
            SpriteHolderRegistry.getHolder("buildcraftcore:triggers/trigger_dir_east")
        };
    }
}
