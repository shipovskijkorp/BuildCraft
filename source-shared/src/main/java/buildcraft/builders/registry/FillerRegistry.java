/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.registry;

import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;

import javax.annotation.Nullable;

import buildcraft.api.filler.IFilledTemplate;
import buildcraft.api.filler.IFillerPattern;
import buildcraft.api.filler.IFillerRegistry;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.Template;
import net.minecraft.core.BlockPos;

public enum FillerRegistry implements IFillerRegistry {
    INSTANCE;

    private final Map<String, IFillerPattern> patterns = new LinkedHashMap<>();

    @Override
    public synchronized void addPattern(IFillerPattern pattern) {
        Objects.requireNonNull(pattern, "pattern");
        String id = Objects.requireNonNull(pattern.getUniqueTag(), "pattern unique tag");
        // Legacy IFillerRegistry historically used last-write-wins for duplicate
        // statement tags. Preserve that compatibility behavior until filler
        // patterns move to the typed API 2 statements domain.
        patterns.put(id, pattern);
    }

    @Override
    @Nullable
    public IFillerPattern getPattern(String name) {
        return patterns.get(name);
    }

    @Override
    public synchronized Collection<IFillerPattern> getPatterns() {
        return List.copyOf(patterns.values());
    }

    @Override
    public IFilledTemplate createFilledTemplate(BlockPos pos, BlockPos size) {
        Template template = new Template();
        template.size = size;
        template.offset = pos;
        template.data = new BitSet(Snapshot.getDataSize(size));
        return template.getFilledTemplate();
    }
}
