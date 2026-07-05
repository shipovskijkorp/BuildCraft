/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package ct.buildcraft.builders.snapshot;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;

import ct.buildcraft.lib.misc.NBTUtilBC;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class NbtPath {
    private final List<String> elements;

    private NbtPath(List<String> elements) {
        this.elements = elements;
    }

    public static NbtPath of(String... elements) {
        return new NbtPath(Arrays.asList(elements));
    }

    public static NbtPath of(List<String> elements) {
        return new NbtPath(elements);
    }

    public List<String> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public void remove(CompoundTag tag) {
        if (tag == null || elements.isEmpty()) {
            return;
        }
        remove((Tag) tag, 0);
    }

    private void remove(Tag tag, int index) {
        if (tag == null || index >= elements.size()) {
            return;
        }
        if (tag instanceof CompoundTag compoundTag) {
            String key = elements.get(index);
            if (index == elements.size() - 1) {
                compoundTag.remove(key);
            } else if (compoundTag.contains(key)) {
                remove(compoundTag.get(key), index + 1);
            }
        } else if (tag instanceof ListTag listTag) {
            int key;
            try {
                key = Integer.parseInt(elements.get(index));
            } catch (NumberFormatException e) {
                return;
            }
            if (key >= 0 && key < listTag.size()) {
                remove(listTag.get(key), index + 1);
            }
        }
    }

    public Tag get(ByteTag tag) {
        if (elements.isEmpty()) {
            return tag;
        } else {
            return NBTUtilBC.NBT_NULL;
        }
    }

    public Tag get(ShortTag tag) {
        if (elements.isEmpty()) {
            return tag;
        } else {
            return NBTUtilBC.NBT_NULL;
        }
    }

    public Tag get(IntTag tag) {
        if (elements.isEmpty()) {
            return tag;
        } else {
            return NBTUtilBC.NBT_NULL;
        }
    }

    public Tag get(LongTag tag) {
        if (elements.isEmpty()) {
            return tag;
        } else {
            return NBTUtilBC.NBT_NULL;
        }
    }

    public Tag get(FloatTag tag) {
        if (elements.isEmpty()) {
            return tag;
        } else {
            return NBTUtilBC.NBT_NULL;
        }
    }

    public Tag get(DoubleTag tag) {
        if (elements.isEmpty()) {
            return tag;
        } else {
            return NBTUtilBC.NBT_NULL;
        }
    }

    public Tag get(ByteArrayTag tag) {
        if (elements.size() == 1) {
            int key;
            try {
                key = Integer.parseInt(elements.get(0));
            } catch (NumberFormatException e) {
                return NBTUtilBC.NBT_NULL;
            }
            if (key >= 0 && key < tag.getAsByteArray().length) {
                return ByteTag.valueOf(tag.getAsByteArray()[key]);
            } else {
                return NBTUtilBC.NBT_NULL;
            }
        } else if (elements.isEmpty()) {
            return tag;
        } else {
            return NBTUtilBC.NBT_NULL;
        }
    }

    public Tag get(StringTag tag) {
        if (elements.isEmpty()) {
            return tag;
        } else {
            return NBTUtilBC.NBT_NULL;
        }
    }

    public Tag get(ListTag tag) {
        if (elements.size() == 1) {
            int key;
            try {
                key = Integer.parseInt(elements.get(0));
            } catch (NumberFormatException e) {
                return NBTUtilBC.NBT_NULL;
            }
            if (key >= 0 && key < tag.size()) {
                return new NbtPath(elements.subList(1, elements.size())).get(tag.get(key));
            } else {
                return NBTUtilBC.NBT_NULL;
            }
        } else if (elements.isEmpty()) {
            return tag;
        } else {
            return NBTUtilBC.NBT_NULL;
        }
    }

    public Tag get(CompoundTag tag) {
        if (!elements.isEmpty()) {
            String key = elements.get(0);
            if (tag.contains(key)) {
                return new NbtPath(elements.subList(1, elements.size())).get(tag.get(key));
            } else {
                return NBTUtilBC.NBT_NULL;
            }
        } else {
            return tag;
        }
    }

    public Tag get(IntArrayTag tag) {
        if (elements.size() == 1) {
            int key;
            try {
                key = Integer.parseInt(elements.get(0));
            } catch (NumberFormatException e) {
                return NBTUtilBC.NBT_NULL;
            }
            if (key >= 0 && key < tag.getAsIntArray().length) {
                return IntTag.valueOf(tag.getAsIntArray()[key]);
            } else {
                return NBTUtilBC.NBT_NULL;
            }
        } else if (elements.isEmpty()) {
            return tag;
        } else {
            return NBTUtilBC.NBT_NULL;
        }
    }

    public Tag get(Tag tag) {
        switch (tag.getId()) {
            case Tag.TAG_BYTE:
                return get((ByteTag) tag);
            case Tag.TAG_SHORT:
                return get((ShortTag) tag);
            case Tag.TAG_INT:
                return get((IntTag) tag);
            case Tag.TAG_LONG:
                return get((LongTag) tag);
            case Tag.TAG_FLOAT:
                return get((FloatTag) tag);
            case Tag.TAG_DOUBLE:
                return get((DoubleTag) tag);
            case Tag.TAG_BYTE_ARRAY:
                return get((ByteArrayTag) tag);
            case Tag.TAG_STRING:
                return get((StringTag) tag);
            case Tag.TAG_LIST:
                return get((ListTag) tag);
            case Tag.TAG_COMPOUND:
                return get((CompoundTag) tag);
            case Tag.TAG_INT_ARRAY:
                return get((IntArrayTag) tag);
            default:
                return NBTUtilBC.NBT_NULL;
        }
    }

    @Override
    public String toString() {
        return "NbtPath{" + elements + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NbtPath nbtPath)) {
            return false;
        }
        return elements.equals(nbtPath.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements);
    }

    @SuppressWarnings("WeakerAccess")
    public static final JsonDeserializer<NbtPath> DESERIALIZER = (json, typeOfT, context) ->
        new NbtPath(
            context.deserialize(
                json,
                new TypeToken<List<String>>() {
                }.getType()
            )
        );
}
