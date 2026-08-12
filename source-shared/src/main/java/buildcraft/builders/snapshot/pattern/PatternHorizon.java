/*
 * Copyright (c) 2011-2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.builders.snapshot.pattern;

import buildcraft.api.core.render.ISprite;
import buildcraft.builders.internal.filler.legacy.IFilledTemplate;
import buildcraft.builders.internal.filler.legacy.IFillerPatternShape;
import buildcraft.lib.internal.statement.IStatementParameter;
import buildcraft.builders.BCBuildersSprites;

public class PatternHorizon extends Pattern implements IFillerPatternShape {
    public PatternHorizon() {
        super("horizon");
    }

    @Override
    public ISprite getSprite() {
        return BCBuildersSprites.FILLER_HORIZON;
    }

    @Override
    public boolean fillTemplate(IFilledTemplate filledTemplate, IStatementParameter[] params) {
        if (filledTemplate.getSize().getY() <= 0) {
            return false;
        }
        // In BC7 Horizon used the selected X/Z area and produced a single horizontal ground layer. In this snapshot
        // based filler the layer is represented by the bottom plane of the selected volume.
        filledTemplate.setPlaneXZ(0, true);
        return true;
    }
}
