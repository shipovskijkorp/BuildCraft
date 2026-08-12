/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */
package buildcraft.transport.statements;

import buildcraft.api.v2.energy.MjAmount;

import buildcraft.lib.internal.statement.IActionInternal;
import buildcraft.lib.internal.statement.IStatement;
import buildcraft.lib.internal.statement.IStatementContainer;
import buildcraft.lib.internal.statement.IStatementParameter;
import buildcraft.transport.internal.pipe.PipeApi;
import buildcraft.transport.internal.pipe.PipeApi.ForgeEnergyTransferInfo;
import buildcraft.transport.internal.pipe.PipeApi.PowerTransferInfo;
import buildcraft.transport.internal.pipe.PipeDefinition;
import buildcraft.core.statements.BCStatement;
import buildcraft.lib.client.sprite.SpriteHolderRegistry.SpriteHolder;
import buildcraft.transport.BCTransportPipes;
import buildcraft.transport.BCTransportSprites;
import buildcraft.transport.BCTransportStatements;
import buildcraft.transport.pipe.behaviour.PipeBehaviourLimiter;
import net.minecraft.network.chat.Component;

/** Gate action used by Iron and Diamond MJ/FE limiter pipes. */
public abstract class ActionPowerLimit extends BCStatement implements IActionInternal {
    public final PipeDefinition pipe;
    public final int limitShift;

    protected ActionPowerLimit(PipeDefinition pipe, int limitShift, String... uniqueTags) {
        super(uniqueTags);
        if (limitShift < 0 || limitShift > PipeBehaviourLimiter.MAX_SHIFT) {
            throw new IllegalArgumentException("limitShift out of range: " + limitShift);
        }
        this.pipe = pipe;
        this.limitShift = limitShift;
    }

    protected ActionPowerLimit(String suffix, PipeDefinition pipe, int limitShift) {
        this(pipe, limitShift, "buildcraft:pipe.power_limit." + suffix + "_s" + limitShift);
    }

    protected boolean isFe() {
        return false;
    }

    @Override
    public Component getDescription() {
        if (isFe()) {
            ForgeEnergyTransferInfo pipeInfo = PipeApi.getForgeEnergyTransferInfo(pipe);
            Object max;
            if (limitShift == PipeBehaviourLimiter.MAX_SHIFT) {
                max = 0;
            } else if (pipeInfo == null) {
                max = "??[INVALID_PIPE]??";
            } else {
                max = pipeInfo.transferPerTick >> limitShift;
            }
            return Component.translatable("gate.action.pipe.fe_limit", max);
        }

        PowerTransferInfo pipeInfo = PipeApi.getPowerTransferInfo(pipe);
        Object max;
        if (limitShift == PipeBehaviourLimiter.MAX_SHIFT) {
            max = 0;
        } else if (pipeInfo == null) {
            max = "??[INVALID_PIPE]??";
        } else {
            max = (pipeInfo.transferPerTick >> limitShift) / MjAmount.MICRO_MJ_PER_MJ;
        }
        return Component.translatable("gate.action.pipe.power_limit", max);
    }

    @Override
    public SpriteHolder getSprite() {
        return (isFe() ? BCTransportSprites.FE_LIMIT : BCTransportSprites.POWER_LIMIT)[limitShift];
    }

    @Override
    public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {
        // PipeBehaviourLimiter handles PipeEventActionActivate.
    }

    @Override
    public abstract IStatement[] getPossible();

    public static final class ActionIronPowerLimit extends ActionPowerLimit {
        public ActionIronPowerLimit(int limitShift) {
            super("iron", BCTransportPipes.ironPower, limitShift);
        }
        @Override public IStatement[] getPossible() { return BCTransportStatements.ACTION_IRON_POWER_LIMIT; }
    }

    public static final class ActionDiamondPowerLimit extends ActionPowerLimit {
        public ActionDiamondPowerLimit(int limitShift) {
            super("diamond", BCTransportPipes.diamondPower, limitShift);
        }
        @Override public IStatement[] getPossible() { return BCTransportStatements.ACTION_DIAMOND_POWER_LIMIT; }
    }

    public static final class ActionIronFeLimit extends ActionPowerLimit {
        public ActionIronFeLimit(int limitShift) {
            // Keep the legacy unique tag as a read alias for existing worlds.
            super(BCTransportPipes.ironFe, limitShift,
                "buildcraft:pipe.power_limit.iron_fe_s" + limitShift,
                "buildcraft:pipe.power_limit.iron_rf_s" + limitShift);
        }
        @Override protected boolean isFe() { return true; }
        @Override public IStatement[] getPossible() { return BCTransportStatements.ACTION_IRON_FE_LIMIT; }
    }

    public static final class ActionDiamondFeLimit extends ActionPowerLimit {
        public ActionDiamondFeLimit(int limitShift) {
            // Keep the legacy unique tag as a read alias for existing worlds.
            super(BCTransportPipes.diamondFe, limitShift,
                "buildcraft:pipe.power_limit.diamond_fe_s" + limitShift,
                "buildcraft:pipe.power_limit.diamond_rf_s" + limitShift);
        }
        @Override protected boolean isFe() { return true; }
        @Override public IStatement[] getPossible() { return BCTransportStatements.ACTION_DIAMOND_FE_LIMIT; }
    }
}
