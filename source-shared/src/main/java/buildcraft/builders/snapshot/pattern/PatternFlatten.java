/*
 * Copyright (c) 2011-2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.builders.snapshot.pattern;

import buildcraft.api.core.render.ISprite;
import buildcraft.api.filler.IFilledTemplate;
import buildcraft.api.filler.IFillerPatternShape;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.builders.BCBuildersSprites;

public class PatternFlatten extends Pattern implements IFillerPatternShape {
    public PatternFlatten() {
        super("flatten");
    }

    @Override
    public ISprite getSprite() {
        return BCBuildersSprites.FILLER_FLATTEN;
    }

    @Override
    public boolean fillTemplate(IFilledTemplate filledTemplate, IStatementParameter[] params) {
        if (filledTemplate.getSize().getY() <= 0) {
            return false;
        }
        // Classic BuildCraft flatten: fill the bottom layer of the selected area. The filler/builder will excavate
        // everything above this layer when excavation is enabled.
        filledTemplate.setPlaneXZ(0, true);
        return true;
    }
}
