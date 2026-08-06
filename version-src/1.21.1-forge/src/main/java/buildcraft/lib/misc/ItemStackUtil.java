/**
 * Copyright (c) the BuildCraft team, 2026.
 * This file is part of BuildCraft, licensed under the LGPLv3.
 */
package buildcraft.lib.misc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Compatibility helpers for the Minecraft 1.20.5+ ItemStack data-component API. */
public final class ItemStackUtil {
    private ItemStackUtil() {
    }

    @Nonnull
    public static CompoundTag getCustomData(@Nonnull ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    @Nullable
    public static CompoundTag getCustomDataOrNull(@Nonnull ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public static boolean hasCustomData(@Nonnull ItemStack stack) {
        CompoundTag tag = getCustomDataOrNull(stack);
        return tag != null && !tag.isEmpty();
    }

    public static void setCustomData(@Nonnull ItemStack stack, @Nullable CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
        }
    }

    /** Static registry context used only before a server/client world is available. */
    private static final RegistryAccess BUILTIN_REGISTRIES =
        RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    /** Client-side registry lookup cached while a world is connected. */
    @Nullable
    private static volatile RegistryAccess clientRegistries;

    public static void setClientRegistryProvider(@Nullable HolderLookup.Provider registries) {
        clientRegistries = registries instanceof RegistryAccess access ? access : null;
    }

    /**
     * Returns the active server registry lookup, the connected client world's lookup,
     * or a built-in registry lookup during early mod lifecycle events.
     */
    @Nonnull
    public static HolderLookup.Provider getActiveRegistryProvider() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.registryAccess();
        }
        RegistryAccess client = clientRegistries;
        return client != null ? client : BUILTIN_REGISTRIES;
    }

    @Nonnull
    public static HolderLookup.Provider requireActiveRegistryProvider() {
        return getActiveRegistryProvider();
    }

    /** Registry access required by RegistryFriendlyByteBuf in Minecraft 1.21.1. */
    @Nonnull
    public static RegistryAccess getActiveRegistryAccess() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.registryAccess();
        }
        RegistryAccess client = clientRegistries;
        return client != null ? client : BUILTIN_REGISTRIES;
    }

    /**
     * Compatibility overload for APIs that cannot pass a lookup explicitly. It is
     * always registry-aware; callers with a world/provider should prefer the explicit overload.
     */
    @Nonnull
    public static CompoundTag saveOptional(@Nonnull ItemStack stack) {
        return saveOptional(stack, requireActiveRegistryProvider());
    }

    /** Counterpart to {@link #saveOptional(ItemStack)}. */
    @Nonnull
    public static ItemStack parseOptional(@Nullable CompoundTag tag) {
        return parseOptional(requireActiveRegistryProvider(), tag);
    }

    @Nonnull
    public static ItemStack parseOptional(@Nonnull HolderLookup.Provider registries, @Nullable CompoundTag tag) {
        return tag == null || tag.isEmpty() ? ItemStack.EMPTY : ItemStack.parseOptional(registries, tag);
    }

    @Nonnull
    public static CompoundTag saveOptional(@Nonnull ItemStack stack, @Nonnull HolderLookup.Provider registries) {
        Tag tag = stack.saveOptional(registries);
        return tag instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    @Nonnull
    private static RegistryFriendlyByteBuf registryBuffer(@Nonnull FriendlyByteBuf buffer) {
        if (buffer instanceof RegistryFriendlyByteBuf registryBuffer) {
            return registryBuffer;
        }
        // BuildCraft has several length-prefixed nested payloads represented as a plain
        // FriendlyByteBuf. Wrap the same backing buffer with the active registry lookup
        // instead of falling back to NBT or rejecting an otherwise valid packet.
        return new RegistryFriendlyByteBuf(buffer, getActiveRegistryAccess());
    }

    public static void write(@Nonnull FriendlyByteBuf buffer, @Nonnull ItemStack stack) {
        ItemStack.STREAM_CODEC.encode(registryBuffer(buffer), stack);
    }

    @Nonnull
    public static ItemStack read(@Nonnull FriendlyByteBuf buffer) {
        return ItemStack.STREAM_CODEC.decode(registryBuffer(buffer));
    }

    public static void writeOptional(@Nonnull FriendlyByteBuf buffer, @Nonnull ItemStack stack) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(registryBuffer(buffer), stack);
    }

    @Nonnull
    public static ItemStack readOptional(@Nonnull FriendlyByteBuf buffer) {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode(registryBuffer(buffer));
    }
}
