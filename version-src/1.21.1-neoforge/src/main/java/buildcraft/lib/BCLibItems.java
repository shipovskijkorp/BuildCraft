/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.lib;

import buildcraft.lib.item.ItemGuide;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Items owned by the BuildCraft library module. */
public final class BCLibItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, BCLib.MODID);

    public static final DeferredHolder<Item, ItemGuide> GUIDE = ITEMS.register(
        "guide",
        () -> new ItemGuide(new Item.Properties().stacksTo(1))
    );

    private BCLibItems() {
    }

    static void registry(IEventBus bus) {
        ITEMS.register(bus);
    }
}
