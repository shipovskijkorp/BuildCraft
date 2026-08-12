/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;

import buildcraft.lib.internal.statement.IActionExternal;
import buildcraft.lib.internal.statement.IActionInternal;
import buildcraft.lib.internal.statement.IActionInternalSided;
import buildcraft.lib.internal.statement.IActionProvider;
import buildcraft.lib.internal.statement.IStatementContainer;
import buildcraft.lib.internal.statement.containers.IFillerStatementContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public enum BCBuildersActionProvider implements IActionProvider {
    INSTANCE;

    @Override
    public void addInternalActions(Collection<IActionInternal> res, IStatementContainer container) {
    }

    @Override
    public void addInternalSidedActions(Collection<IActionInternalSided> actions, IStatementContainer container, @Nonnull Direction side) {
    }

    @Override
    public void addExternalActions(Collection<IActionExternal> res, @Nonnull Direction side, BlockEntity tile) {
        if (tile instanceof IFillerStatementContainer) {
            Collections.addAll(res, BCBuildersStatements.PATTERNS);
        }
    }
}
