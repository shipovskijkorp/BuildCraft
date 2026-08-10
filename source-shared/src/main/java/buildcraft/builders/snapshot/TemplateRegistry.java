/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.builders.snapshot;

import buildcraft.api.core.EnumHandlerPriority;
import buildcraft.api.template.ITemplateHandler;
import buildcraft.api.template.ITemplateRegistry;
import buildcraft.lib.api.v2.BuildCraftApiRuntime;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Legacy template registry facade backed by API 2's ordered TemplateService. */
public enum TemplateRegistry implements ITemplateRegistry {
    INSTANCE;

    private final AtomicLong nextId = new AtomicLong();

    @Override
    public void addHandler(ITemplateHandler handler, EnumHandlerPriority priority) {
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(priority, "priority");
        ResourceLocation id = id(String.format(Locale.ROOT, "legacy/handler/%010d", nextId.getAndIncrement()));
        BuildCraftApiRuntime.INSTANCE.templates().register(id, priorityValue(priority), handler::handle);
    }

    @Override
    public boolean handle(Level world, BlockPos pos, Player player, ItemStack stack) {
        return BuildCraftApiRuntime.INSTANCE.templates().handle(world, pos, player, stack);
    }

    private static int priorityValue(EnumHandlerPriority priority) {
        return switch (priority) {
            case HIGHEST -> 400;
            case HIGH -> 300;
            case NORMAL -> 200;
            case LOW -> 100;
            case LOWEST -> 0;
        };
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("buildcraft:template/" + path));
    }
}
