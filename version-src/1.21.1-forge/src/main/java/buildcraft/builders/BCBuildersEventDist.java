/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.builders;


import buildcraft.builders.client.ClientArchitectTables;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class BCBuildersEventDist {

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onTickClientTick(TickEvent.ClientTickEvent.Post event) {
        if (!Minecraft.getInstance().isPaused()) {
            ClientArchitectTables.tick();
        }
    }
}
