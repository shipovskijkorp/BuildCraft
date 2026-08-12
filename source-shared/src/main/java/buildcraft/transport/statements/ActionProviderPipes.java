/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

import buildcraft.transport.internal.gate.IGate;
import buildcraft.lib.internal.statement.IActionExternal;
import buildcraft.lib.internal.statement.IActionInternal;
import buildcraft.lib.internal.statement.IActionInternalSided;
import buildcraft.lib.internal.statement.IActionProvider;
import buildcraft.lib.internal.statement.IStatementContainer;
import buildcraft.transport.internal.IWireEmitter;
import buildcraft.transport.internal.pipe.IPipeHolder;
import buildcraft.transport.internal.pipe.PipeDefinition;
import buildcraft.transport.internal.pipe.PipeEventStatement;

import buildcraft.lib.misc.ColourUtil;

import buildcraft.transport.BCTransportPipes;
import buildcraft.transport.BCTransportStatements;

public enum ActionProviderPipes implements IActionProvider {
    INSTANCE;

    @Override
    public void addInternalActions(Collection<IActionInternal> actions, IStatementContainer container) {
        if (container instanceof IGate) {
            IGate gate = (IGate) container;
            IPipeHolder holder = gate.getPipeHolder();
            holder.fireEvent(new PipeEventStatement.AddActionInternal(holder, actions));

            if (container instanceof IWireEmitter) {
                for (DyeColor colour : ColourUtil.COLOURS) {
                    if (TriggerPipeSignal.doesGateHaveColour(gate, colour)) {
                        actions.add(BCTransportStatements.ACTION_PIPE_SIGNAL[colour.ordinal()]);
                    }
                }
            }

            PipeDefinition def = holder.getPipe().getDefinition();
            if (def == BCTransportPipes.ironPower) {
                Collections.addAll(actions, BCTransportStatements.ACTION_IRON_POWER_LIMIT);
            } else if (def == BCTransportPipes.diamondPower) {
                Collections.addAll(actions, BCTransportStatements.ACTION_DIAMOND_POWER_LIMIT);
            } else if (def == BCTransportPipes.ironFe) {
                Collections.addAll(actions, BCTransportStatements.ACTION_IRON_FE_LIMIT);
            } else if (def == BCTransportPipes.diamondFe) {
                Collections.addAll(actions, BCTransportStatements.ACTION_DIAMOND_FE_LIMIT);
            }
        }
    }

    @Override
    public void addInternalSidedActions(Collection<IActionInternalSided> actions, IStatementContainer container,
        @Nonnull Direction side) {
        if (container instanceof IGate) {
            IGate gate = (IGate) container;
            IPipeHolder holder = gate.getPipeHolder();
            holder.fireEvent(new PipeEventStatement.AddActionInternalSided(holder, actions, side));
        }
    }

    @Override
    public void addExternalActions(Collection<IActionExternal> actions, @Nonnull Direction side, BlockEntity tile) {

    }
}
