/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package ct.buildcraft.builders.snapshot;

import ct.buildcraft.api.mj.MjAPI;
import ct.buildcraft.builders.tile.TileFiller;

/**
 * Snapshot builder tuned specifically for the Filler.
 *
 * <p>The regular Builder also uses {@link TemplateBuilder}, so changing its constants would speed up both machines.
 * Keeping the Filler tuning in a dedicated subclass guarantees that Builder timing and power use remain unchanged.</p>
 */
public class FillerTemplateBuilder extends TemplateBuilder {
    private static final int FILLER_CHECKS_PER_TICK = 128;
    private static final int FILLER_FAST_CHECKS_PER_TICK = 256;
    private static final long FILLER_MAX_POWER_PER_TICK = 512 * MjAPI.MJ;

    public FillerTemplateBuilder(TileFiller tile) {
        super(tile);
    }

    @Override
    protected int getChecksPerTick() {
        return FILLER_CHECKS_PER_TICK;
    }

    @Override
    protected int getFastChecksPerTick() {
        return FILLER_FAST_CHECKS_PER_TICK;
    }

    @Override
    protected long getMaxPowerPerTick() {
        return FILLER_MAX_POWER_PER_TICK;
    }
}
