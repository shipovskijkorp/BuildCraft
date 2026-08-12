/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import java.util.Locale;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftServices;
import buildcraft.api.v2.signal.BuildCraftSignalChannels;
import buildcraft.transport.internal.gate.IGate;
import buildcraft.lib.internal.statement.IStatement;
import buildcraft.lib.internal.statement.IStatementContainer;
import buildcraft.lib.internal.statement.IStatementParameter;
import buildcraft.lib.internal.statement.ITriggerInternal;
import buildcraft.core.statements.BCStatement;
import buildcraft.lib.client.sprite.SpriteHolderRegistry.SpriteHolder;
import buildcraft.transport.BCTransportSprites;
import buildcraft.transport.BCTransportStatements;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.DyeColor;

public class TriggerPipeSignal extends BCStatement implements ITriggerInternal {

    private final boolean active;
    private final DyeColor colour;

    public TriggerPipeSignal(boolean active, DyeColor colour) {
        super(
            "buildcraft:pipe.wire.input." + colour.getName().toLowerCase(Locale.ROOT)
                + (active ? ".active" : ".inactive"), //
            "buildcraft.pipe.wire.input." + colour.getName().toLowerCase(Locale.ROOT)
                + (active ? ".active" : ".inactive"));

        this.active = active;
        this.colour = colour;
    }

    public static boolean doesGateHaveColour(IGate gate, DyeColor c) {
        return BuildCraftApi.service(BuildCraftServices.SIGNALS)
            .port(gate.getPipeHolder().getPipeWorld(), gate.getPipeHolder().getPipePos(), gate.getSide(), BuildCraftSignalChannels.id(c))
            .map(buildcraft.api.v2.signal.SignalPort::connected)
            .orElse(false);
    }

    @Override
    public int maxParameters() {
        return 3;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("gate.trigger.pipe.wire." + (active ? "active" : "inactive"),
        		Component.translatable("color.minecraft." + colour.getName()).withStyle(Style.EMPTY.withColor(colour.getTextColor())));
    }

    @Override
    public boolean isTriggerActive(IStatementContainer container, IStatementParameter[] parameters) {
        if (!(container instanceof IGate)) {
            return false;
        }

        IGate gate = (IGate) container;

        if (this.active != readSignal(gate, this.colour)) {
            return false;
        }

        for (IStatementParameter param : parameters) {
            if (param instanceof TriggerParameterSignal signal && signal.colour != null) {
                if (signal.active != readSignal(gate, signal.colour)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean readSignal(IGate gate, DyeColor colour) {
        return BuildCraftApi.service(BuildCraftServices.SIGNALS)
            .port(gate.getPipeHolder().getPipeWorld(), gate.getPipeHolder().getPipePos(), gate.getSide(), BuildCraftSignalChannels.id(colour))
            .map(port -> Boolean.TRUE.equals(port.value()))
            .orElse(false);
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return TriggerParameterSignal.EMPTY;
    }

    @Override
    public SpriteHolder getSprite() {
        return BCTransportSprites.getPipeSignal(active, colour);
    }

    @Override
    public IStatement[] getPossible() {
        return BCTransportStatements.TRIGGER_PIPE_SIGNAL;
    }
}
