/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package ct.buildcraft.silicon.plug;

import ct.buildcraft.api.transport.pipe.IPipeHolder;
import ct.buildcraft.api.transport.pipe.PipeEventHandler;
import ct.buildcraft.api.transport.pipe.PipeEventStatement;
import ct.buildcraft.api.transport.pluggable.PipePluggable;
import ct.buildcraft.api.transport.pluggable.PluggableDefinition;
import ct.buildcraft.api.transport.pluggable.PluggableModelKey;
import ct.buildcraft.silicon.BCSiliconItems;
import ct.buildcraft.silicon.BCSiliconStatements;
import ct.buildcraft.silicon.client.model.key.KeyPlugTimer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class PluggableTimer extends PipePluggable {
    private static final VoxelShape[] BOXES = new VoxelShape[6];

    static {
        double ll = 2 / 16.0;
        double lu = 4 / 16.0;
        double ul = 12 / 16.0;
        double uu = 14 / 16.0;

        double min = 5 / 16.0;
        double max = 11 / 16.0;

        BOXES[Direction.DOWN.get3DDataValue()] = Shapes.box(min, ll, min, max, lu, max);
        BOXES[Direction.UP.get3DDataValue()] = Shapes.box(min, ul, min, max, uu, max);
        BOXES[Direction.NORTH.get3DDataValue()] = Shapes.box(min, min, ll, max, max, lu);
        BOXES[Direction.SOUTH.get3DDataValue()] = Shapes.box(min, min, ul, max, max, uu);
        BOXES[Direction.WEST.get3DDataValue()] = Shapes.box(ll, min, min, lu, max, max);
        BOXES[Direction.EAST.get3DDataValue()] = Shapes.box(ul, min, min, uu, max, max);
    }

    public PluggableTimer(PluggableDefinition definition, IPipeHolder holder, Direction side) {
        super(definition, holder, side);
    }

    @Override
    public VoxelShape getBoundingBox() {
        return BOXES[side.get3DDataValue()];
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public ItemStack getPickStack() {
        return new ItemStack(BCSiliconItems.PLUG_TIMER_ITEM.get());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public PluggableModelKey getModelRenderKey(RenderType layer) {
        if (layer == RenderType.cutout()) return new KeyPlugTimer(side);
        return null;
    }

    @PipeEventHandler
    public void addInternalTriggers(PipeEventStatement.AddTriggerInternal event) {
        event.triggers.add(BCSiliconStatements.TRIGGER_TIMER_SHORT);
        event.triggers.add(BCSiliconStatements.TRIGGER_TIMER_MEDIUM);
        event.triggers.add(BCSiliconStatements.TRIGGER_TIMER_LONG);
    }
}
