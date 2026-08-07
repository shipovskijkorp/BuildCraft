/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.builders;

import java.lang.ref.WeakReference;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import buildcraft.builders.client.ClientArchitectTables;
import buildcraft.builders.tile.TileQuarry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class BCBuildersEventDist {

    private static final UUID UUID_SINGLE_SCHEMATIC = new UUID(0xfd3b8c59b0a8b191L, 0x772ec006c1b0ffaaL);
    private static final Map<Level, Deque<WeakReference<TileQuarry>>> allQuarries = new WeakHashMap<>();

    @Deprecated
    public static synchronized void validateQuarry(TileQuarry quarry) {
        Deque<WeakReference<TileQuarry>> quarries =
            allQuarries.computeIfAbsent(quarry.getLevel(), k -> new LinkedList<>());
        quarries.add(new WeakReference<>(quarry));
    }

    @Deprecated
    public static synchronized void invalidateQuarry(TileQuarry quarry) {
        Deque<WeakReference<TileQuarry>> quarries = allQuarries.get(quarry.getLevel());
        if (quarries == null) {
            // Odd.
            return;
        }
        Iterator<WeakReference<TileQuarry>> iter = quarries.iterator();
        while (iter.hasNext()) {
            WeakReference<TileQuarry> ref = iter.next();
            TileQuarry pos = ref.get();
            if (pos == null || pos == quarry) {
                iter.remove();
            }
        }
    }

/*    @SubscribeEvent
    public synchronized void onGetCollisionBoxesForQuarry(GetCollisionBoxesEvent event) {
        Deque<WeakReference<TileQuarry>> quarries = allQuarries.get(event.getWorld());
        Level
        if (quarries == null) {
            // No quarries in the target world
            return;
        }
        Iterator<WeakReference<TileQuarry>> iter = quarries.iterator();
        while (iter.hasNext()) {
            WeakReference<TileQuarry> ref = iter.next();
            TileQuarry quarry = ref.get();
            if (quarry == null) {
                iter.remove();
                continue;
            }
            for (AABB aabb : quarry.getCollisionBoxes()) {
                if (event.getAabb().intersects(aabb)) {
                    event.getCollisionBoxesList().add(aabb);
                }
            }
        }
    }*/

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onTickClientTick(ClientTickEvent.Post event) {
        if (!Minecraft.getInstance().isPaused()) {
            ClientArchitectTables.tick();
        }
    }
}
