/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import net.minecraft.world.item.DyeColor;
import net.minecraft.core.Direction;

import buildcraft.lib.internal.statement.StatementManager;
import buildcraft.lib.internal.statement.api2.StatementApi2Bridge;

import buildcraft.lib.misc.ColourUtil;

import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli.SlotIndex;
import buildcraft.transport.pipe.behaviour.PipeBehaviourLimiter;
import buildcraft.transport.statements.ActionExtractionPreset;
import buildcraft.transport.statements.ActionParameterSignal;
import buildcraft.transport.statements.ActionPipeColor;
import buildcraft.transport.statements.ActionPipeDirection;
import buildcraft.transport.statements.ActionPipeSignal;
import buildcraft.transport.statements.ActionPowerLimit.ActionDiamondFeLimit;
import buildcraft.transport.statements.ActionPowerLimit.ActionDiamondPowerLimit;
import buildcraft.transport.statements.ActionPowerLimit.ActionIronFeLimit;
import buildcraft.transport.statements.ActionPowerLimit.ActionIronPowerLimit;
import buildcraft.transport.statements.ActionProviderPipes;
import buildcraft.transport.statements.TriggerFluidsTraversing;
import buildcraft.transport.statements.TriggerItemsTraversing;
import buildcraft.transport.statements.TriggerParameterSignal;
import buildcraft.transport.statements.TriggerPipeSignal;
import buildcraft.transport.statements.TriggerPowerRequested;
import buildcraft.transport.statements.TriggerProviderPipes;

public class BCTransportStatements {

    public static final TriggerPipeSignal[] TRIGGER_PIPE_SIGNAL;
    public static final TriggerPowerRequested TRIGGER_POWER_REQUESTED;
    public static final TriggerItemsTraversing TRIGGER_ITEMS_TRAVERSING;
    public static final TriggerFluidsTraversing TRIGGER_FLUIDS_TRAVERSING;
    public static final ActionPipeSignal[] ACTION_PIPE_SIGNAL;
    public static final ActionPipeColor[] ACTION_PIPE_COLOUR;
    public static final ActionExtractionPreset[] ACTION_EXTRACTION_PRESET;
    public static final ActionPipeDirection[] ACTION_PIPE_DIRECTION;
    public static final ActionIronPowerLimit[] ACTION_IRON_POWER_LIMIT;
    public static final ActionDiamondPowerLimit[] ACTION_DIAMOND_POWER_LIMIT;
    public static final ActionIronFeLimit[] ACTION_IRON_FE_LIMIT;
    public static final ActionDiamondFeLimit[] ACTION_DIAMOND_FE_LIMIT;

    static {
        TRIGGER_PIPE_SIGNAL = new TriggerPipeSignal[2 * ColourUtil.COLOURS.length];
        for (DyeColor colour : ColourUtil.COLOURS) {
            TRIGGER_PIPE_SIGNAL[colour.ordinal() * 2 + 0] = new TriggerPipeSignal(true, colour);
            TRIGGER_PIPE_SIGNAL[colour.ordinal() * 2 + 1] = new TriggerPipeSignal(false, colour);
        }

        ACTION_PIPE_SIGNAL = new ActionPipeSignal[ColourUtil.COLOURS.length];
        for (DyeColor colour : ColourUtil.COLOURS) {
            ACTION_PIPE_SIGNAL[colour.ordinal()] = new ActionPipeSignal(colour);
        }

        ACTION_PIPE_COLOUR = new ActionPipeColor[ColourUtil.COLOURS.length];
        for (DyeColor colour : ColourUtil.COLOURS) {
            ACTION_PIPE_COLOUR[colour.ordinal()] = new ActionPipeColor(colour);
        }

        ACTION_EXTRACTION_PRESET = new ActionExtractionPreset[SlotIndex.VALUES.length];
        for (SlotIndex index : SlotIndex.VALUES) {
            ACTION_EXTRACTION_PRESET[index.ordinal()] = new ActionExtractionPreset(index);
        }

        ACTION_PIPE_DIRECTION = new ActionPipeDirection[Direction.values().length];
        for (Direction face : Direction.values()) {
            ACTION_PIPE_DIRECTION[face.ordinal()] = new ActionPipeDirection(face);
        }

        TRIGGER_POWER_REQUESTED = new TriggerPowerRequested();
        TRIGGER_ITEMS_TRAVERSING = new TriggerItemsTraversing();
        TRIGGER_FLUIDS_TRAVERSING = new TriggerFluidsTraversing();

        ACTION_IRON_POWER_LIMIT = new ActionIronPowerLimit[PipeBehaviourLimiter.MAX_SHIFT + 1];
        ACTION_DIAMOND_POWER_LIMIT = new ActionDiamondPowerLimit[PipeBehaviourLimiter.MAX_SHIFT + 1];
        ACTION_IRON_FE_LIMIT = new ActionIronFeLimit[PipeBehaviourLimiter.MAX_SHIFT + 1];
        ACTION_DIAMOND_FE_LIMIT = new ActionDiamondFeLimit[PipeBehaviourLimiter.MAX_SHIFT + 1];
        int limiterIndex = 0;
        for (int shift = PipeBehaviourLimiter.MAX_SHIFT; shift >= 0; shift--) {
            ACTION_IRON_POWER_LIMIT[limiterIndex] = new ActionIronPowerLimit(shift);
            ACTION_DIAMOND_POWER_LIMIT[limiterIndex] = new ActionDiamondPowerLimit(shift);
            ACTION_IRON_FE_LIMIT[limiterIndex] = new ActionIronFeLimit(shift);
            ACTION_DIAMOND_FE_LIMIT[limiterIndex] = new ActionDiamondFeLimit(shift);
            limiterIndex++;
        }

        StatementManager.registerParameter(TriggerParameterSignal::readFromNbt, TriggerParameterSignal::readFromBuf);
        StatementManager.registerParameter(ActionParameterSignal::readFromNbt);
        StatementApi2Bridge.mirrorLegacyStatements(TRIGGER_PIPE_SIGNAL);
        StatementApi2Bridge.mirrorLegacyStatement(TRIGGER_POWER_REQUESTED);
        StatementApi2Bridge.mirrorLegacyStatement(TRIGGER_ITEMS_TRAVERSING);
        StatementApi2Bridge.mirrorLegacyStatement(TRIGGER_FLUIDS_TRAVERSING);
        StatementApi2Bridge.mirrorLegacyStatements(ACTION_PIPE_SIGNAL);
        StatementApi2Bridge.mirrorLegacyStatements(ACTION_PIPE_COLOUR);
        StatementApi2Bridge.mirrorLegacyStatements(ACTION_EXTRACTION_PRESET);
        StatementApi2Bridge.mirrorLegacyStatements(ACTION_PIPE_DIRECTION);
        StatementApi2Bridge.mirrorLegacyStatements(ACTION_IRON_POWER_LIMIT);
        StatementApi2Bridge.mirrorLegacyStatements(ACTION_DIAMOND_POWER_LIMIT);
        StatementApi2Bridge.mirrorLegacyStatements(ACTION_IRON_FE_LIMIT);
        StatementApi2Bridge.mirrorLegacyStatements(ACTION_DIAMOND_FE_LIMIT);
    }

    public static void preInit() {
        StatementManager.registerTriggerProvider(TriggerProviderPipes.INSTANCE);
        StatementManager.registerActionProvider(ActionProviderPipes.INSTANCE);
    }
}
