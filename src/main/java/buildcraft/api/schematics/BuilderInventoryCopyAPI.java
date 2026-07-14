/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.schematics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/**
 * API for mods that want BuildCraft builders to copy item contents from block entity inventories.
 * <p>
 * BuildCraft copies ordinary Forge item-handler inventories generically and restores their contents after placing the
 * empty block. Mods should register an exact NBT path here only for hidden/internal inventories that are not exposed
 * through {@code ForgeCapabilities.ITEM_HANDLER}.
 * <p>
 * Server configuration in {@code config/buildcraft/blacklist_blocks_simple_inventories.json} always overrides these
 * registrations.
 */
public final class BuilderInventoryCopyAPI {
    private static final List<BlockInventoryCopyRule> BLOCK_INVENTORY_COPY_RULES = new ArrayList<>();

    private BuilderInventoryCopyAPI() {
    }

    public static void registerBlockInventoryContent(ResourceLocation blockId, String... nbtPath) {
        registerBlockInventoryContent(blockId, Arrays.asList(nbtPath));
    }

    public static void registerBlockInventoryContent(ResourceLocation blockId, List<String> nbtPath) {
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(nbtPath, "nbtPath");
        if (nbtPath.isEmpty()) {
            throw new IllegalArgumentException("nbtPath must not be empty");
        }
        BLOCK_INVENTORY_COPY_RULES.add(new BlockInventoryCopyRule(blockId, nbtPath));
    }

    public static List<BlockInventoryCopyRule> getBlockInventoryCopyRules() {
        return Collections.unmodifiableList(BLOCK_INVENTORY_COPY_RULES);
    }

    public static final class BlockInventoryCopyRule {
        private final ResourceLocation blockId;
        private final List<String> nbtPath;

        private BlockInventoryCopyRule(ResourceLocation blockId, List<String> nbtPath) {
            this.blockId = blockId;
            this.nbtPath = Collections.unmodifiableList(new ArrayList<>(nbtPath));
        }

        public ResourceLocation getBlockId() {
            return blockId;
        }

        public List<String> getNbtPath() {
            return nbtPath;
        }
    }
}
