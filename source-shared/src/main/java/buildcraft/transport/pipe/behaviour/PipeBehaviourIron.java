/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;

import buildcraft.lib.internal.core.EnumPipePart;
import buildcraft.transport.internal.pipe.IPipe;
import buildcraft.transport.internal.pipe.PipeEventFluid;
import buildcraft.transport.internal.pipe.PipeEventHandler;
import buildcraft.transport.internal.pipe.PipeEventItem;

public class PipeBehaviourIron extends PipeBehaviourDirectional {
    public PipeBehaviourIron(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviourIron(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
    }

    @Override
    public int getTextureIndex(Direction face) {
        return face == currentDir.face ? 0 : 1;
    }

    @Override
    protected boolean canFaceDirection(Direction dir) {
        return pipe.isConnected(dir);
    }

    @PipeEventHandler
    public void sideCheck(PipeEventItem.SideCheck sideCheck) {
        if (currentDir == EnumPipePart.CENTER) {
            sideCheck.disallowAll();
        } else {
            sideCheck.disallowAllExcept(currentDir.face);
        }
    }

    @PipeEventHandler
    public void fluidSideCheck(PipeEventFluid.SideCheck sideCheck) {
        if (currentDir == EnumPipePart.CENTER) {
            sideCheck.disallowAll();
        } else {
            sideCheck.disallowAllExcept(currentDir.face);
        }
    }

    @PipeEventHandler
    public static void tryBounce(PipeEventItem.TryBounce tryBounce) {
        tryBounce.canBounce = true;
    }

    @PipeEventHandler
    public void fluidInsert(PipeEventFluid.TryInsert insert) {
        if (currentDir.face == insert.from) {
            insert.cancel();
        }
    }
}
