/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import java.util.Arrays;

import net.minecraft.nbt.CompoundTag;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeEventFluid;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;

public class PipeBehaviourVoid extends PipeBehaviour {
    public PipeBehaviourVoid(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviourVoid(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
    }

    @PipeEventHandler
    public static void reachCentre(PipeEventItem.ReachCenter reachCenter) {
        reachCenter.getStack().setCount(0);
    }

    @PipeEventHandler
    public static void moveFluidToCentre(PipeEventFluid.OnMoveToCentre move) {
        Arrays.fill(move.fluidEnteringCentre, 0);
    }
}
