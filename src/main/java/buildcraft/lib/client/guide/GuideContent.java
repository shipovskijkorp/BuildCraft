/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.lib.client.guide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import buildcraft.api.core.BCLog;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.StatementManager;

/**
 * Loads the original BuildCraft 8 guide registry and markdown files.
 * <p>
 * The manifest is generated directly from the official BuildCraftGuide 8.0.x-1.12 submodule. The markdown itself is
 * kept under the original resource paths so resource packs can replace individual pages exactly as they could in BC8.
 */
public final class GuideContent {
    private static final ResourceLocation MANIFEST =
        new ResourceLocation("buildcraftlib", "guide/original_manifest.json");
    private static final Pattern FIRST_CHAPTER = Pattern.compile("<chapter\\s+name=\"([^\"]+)\"[^>]*/>");
    private static final Pattern FIRST_HEADING = Pattern.compile("(?m)^#+\\s+(.+)$");
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");

    private final List<Entry> allEntries;
    private final List<Entry> listedEntries;
    private final Map<ResourceLocation, Entry> byId;

    private GuideContent(List<Entry> entries) {
        allEntries = Collections.unmodifiableList(entries);
        List<Entry> listed = new ArrayList<>();
        Map<ResourceLocation, Entry> ids = new LinkedHashMap<>();
        for (Entry entry : entries) {
            ids.put(entry.id, entry);
            if (entry.listed) {
                listed.add(entry);
            }
        }
        listedEntries = Collections.unmodifiableList(listed);
        byId = Collections.unmodifiableMap(ids);
    }

    public static GuideContent load() {
        try {
            String manifestText = readText(MANIFEST);
            JsonObject root = JsonParser.parseString(manifestText).getAsJsonObject();
            JsonArray array = root.getAsJsonArray("entries");
            List<Entry> entries = new ArrayList<>(array.size());
            for (JsonElement element : array) {
                JsonObject json = element.getAsJsonObject();
                ResourceLocation id = new ResourceLocation(json.get("id").getAsString());
                ResourceLocation source = new ResourceLocation(json.get("source").getAsString());
                String text;
                try {
                    text = readText(source);
                } catch (IOException io) {
                    BCLog.logger.warn("[lib.guide] Unable to load original page {} from {}", id, source, io);
                    continue;
                }
                String stackId = getString(json, "stack");
                ItemStack stack = resolveStack(stackId);
                String statement = getString(json, "statement");
                entries.add(new Entry(
                    id,
                    source,
                    getString(json, "module"),
                    getString(json, "type"),
                    getString(json, "subtype"),
                    getString(json, "name"),
                    getString(json, "book"),
                    json.has("listed") && json.get("listed").getAsBoolean(),
                    stackId,
                    statement,
                    stack,
                    text
                ));
            }
            return new GuideContent(entries);
        } catch (Exception ex) {
            BCLog.logger.error("[lib.guide] Failed to load the original BuildCraft guide", ex);
            return new GuideContent(new ArrayList<>());
        }
    }

    private static String readText(ResourceLocation location) throws IOException {
        Optional<Resource> optional = Minecraft.getInstance().getResourceManager().getResource(location);
        if (optional.isEmpty()) {
            throw new IOException("Missing resource " + location);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(optional.get().open(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        }
    }

    @Nullable
    private static String getString(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    private static ItemStack resolveStack(@Nullable String rawId) {
        return resolveStackForTag(rawId, Map.of());
    }

    public static ItemStack resolveStackForTag(@Nullable String rawId) {
        return resolveStackForTag(rawId, Map.of());
    }

    /**
     * Resolves a stack reference from the original 1.12 guide against the current registries.
     * <p>
     * BC8 pages use a mixture of plain registry names, {@code {id,count,data}} shortcuts and XML attributes. Modern
     * BuildCraft split several metadata items into separate registry entries, so resolving only the literal item id
     * breaks whole classes of pages (pipes, engines, coloured wires, lenses and filters).
     */
    public static ItemStack resolveStackForTag(@Nullable String rawId, Map<String, String> attributes) {
        LegacyStackSpec spec = LegacyStackSpec.parse(rawId, attributes);
        if (spec == null) {
            return ItemStack.EMPTY;
        }

        String remappedId = remapLegacyStackId(spec.id, spec.data);
        ResourceLocation id;
        try {
            id = new ResourceLocation(remappedId);
        } catch (RuntimeException ex) {
            return ItemStack.EMPTY;
        }
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = item.getDefaultInstance();
        stack.setCount(Math.max(1, spec.count));

        if (id.equals(new ResourceLocation("buildcraftsilicon", "plug/lens"))) {
            int legacyData = spec.data;
            boolean oldFilterId = "buildcraftsilicon:plug_filter".equals(spec.id);
            boolean filter = oldFilterId || legacyData >= 16;
            int oldDyeDamage = Math.floorMod(legacyData, 16);
            // EnumDyeColor.byDyeDamage(0) was black in 1.12, while DyeColor.byId(0) is white in modern versions.
            int modernColourId = 15 - oldDyeDamage;
            stack.setDamageValue(modernColourId + (filter ? 16 : 0));
        } else if (spec.hasExplicitData && spec.data >= 0) {
            stack.setDamageValue(spec.data);
        }
        return stack;
    }

    /** Visible for the resource regression tests. */
    static String remapLegacyStackId(String id, int data) {
        id = id.trim();
        if (id.startsWith("buildcraftcore:items/")) {
            id = "buildcraftcore:" + id.substring("buildcraftcore:items/".length());
        }
        if (id.startsWith("buildcraftcore:gear_")) {
            return "buildcraftcore:gears/" + id.substring("buildcraftcore:".length());
        }
        if (id.equals("buildcraftcore:paintbrush")) return "buildcraftcore:paintbrush/clean";
        if (id.equals("buildcraftcore:engine")) {
            if (data == 1) return "buildcraftenergy:engine_stone";
            if (data == 2) return "buildcraftenergy:engine_iron";
            return "buildcraftcore:engine_redstone";
        }
        if (id.equals("buildcraftsilicon:plug_facade")) return "buildcraftsilicon:plug/facade";
        if (id.equals("buildcraftsilicon:plug_gate")) return "buildcraftsilicon:plug/gate";
        if (id.equals("buildcraftsilicon:plug_filter")) return "buildcraftsilicon:plug/lens";
        if (id.equals("buildcraftsilicon:plug_lens")) return "buildcraftsilicon:plug/lens";
        if (id.equals("buildcraftsilicon:plug_light_sensor")) return "buildcraftsilicon:plug/light_sensor";
        if (id.equals("buildcraftsilicon:plug_pulsar")) return "buildcraftsilicon:plug/pulsar";
        if (id.equals("buildcrafttransport:wire")) return "buildcrafttransport:wire/white";

        if (id.startsWith("buildcrafttransport:pipe_")) {
            String path = id.substring("buildcrafttransport:pipe_".length());
            if (path.equals("cobble_item")) path = "cobblestone_item";
            if (path.equals("cobble_fluid")) path = "cobblestone_fluid";
            if (path.equals("cobble_power")) path = "cobblestone_power";
            return "buildcrafttransport:" + path;
        }
        return id;
    }

    /** Resolves original statement aliases for both index titles and sprites. */
    @Nullable
    public static IStatement resolveStatement(@Nullable String rawId) {
        if (rawId == null || rawId.isEmpty()) return null;
        return StatementManager.statements.get(remapLegacyStatementId(rawId));
    }

    /** Visible for the resource regression tests. */
    static String remapLegacyStatementId(String rawId) {
        String id = rawId.trim();
        if (id.startsWith("buildcraft.") && id.indexOf(':') < 0) {
            id = "buildcraft:" + id.substring("buildcraft.".length());
        }
        switch (id) {
            case "buildcraft:pipe.items.traversing": return "buildcraft:pipe_contains_items";
            case "buildcraft:pipe.fluids.traversing": return "buildcraft:pipe_contains_fluids";
            default: return id;
        }
    }

    private static final class LegacyStackSpec {
        final String id;
        final int count;
        final int data;
        final boolean hasExplicitData;

        private LegacyStackSpec(String id, int count, int data, boolean hasExplicitData) {
            this.id = id;
            this.count = count;
            this.data = data;
            this.hasExplicitData = hasExplicitData;
        }

        @Nullable
        static LegacyStackSpec parse(@Nullable String rawId, Map<String, String> attributes) {
            if (rawId == null || rawId.trim().isEmpty()) return null;
            String value = rawId.trim();
            if ((value.startsWith("{") && value.endsWith("}"))
                || (value.startsWith("(") && value.endsWith(")"))) {
                value = value.substring(1, value.length() - 1).trim();
            }
            String[] split = value.split(",", -1);
            String id = split.length == 0 ? "" : split[0].trim();
            if (id.isEmpty()) return null;

            int count = parseInt(attributes.get("count"), split.length > 1 ? parseInt(split[1], 1) : 1);
            boolean hasDataInValue = split.length > 2 && !split[2].trim().isEmpty();
            boolean hasDataAttribute = attributes.containsKey("data") && attributes.get("data") != null
                && !attributes.get("data").trim().isEmpty();
            int data = hasDataAttribute ? parseInt(attributes.get("data"), 0)
                : hasDataInValue ? parseInt(split[2], 0) : 0;
            return new LegacyStackSpec(id, count, data, hasDataInValue || hasDataAttribute);
        }
    }

    private static int parseInt(@Nullable String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public List<Entry> getAllEntries() {
        return allEntries;
    }

    public List<Entry> getListedEntries() {
        return listedEntries;
    }

    @Nullable
    public Entry get(ResourceLocation id) {
        return byId.get(id);
    }

    @Nullable
    public Entry get(String id) {
        try {
            return get(new ResourceLocation(id));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static Entry createGeneratedItemEntry(ItemStack stack) {
        ResourceLocation registryId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (registryId == null) {
            registryId = new ResourceLocation("minecraft", "air");
        }
        ResourceLocation pageId = new ResourceLocation(
            "buildcraftlib", "generated/item/" + registryId.getNamespace() + "/" + registryId.getPath()
        );
        return new Entry(
            pageId, pageId, registryId.getNamespace(), "item", "generated", registryId.getPath(),
            "buildcraftcore:main", false, registryId.toString(), null, stack.copy(), ""
        );
    }

    public static final class Entry {
        public final ResourceLocation id;
        public final ResourceLocation source;
        public final String module;
        public final String type;
        public final String subtype;
        public final String name;
        public final String book;
        public final boolean listed;
        public final @Nullable String stackId;
        public final @Nullable String statement;
        public final ItemStack stack;
        public final String markdown;
        public final String searchText;

        private Entry(ResourceLocation id, ResourceLocation source, @Nullable String module, @Nullable String type,
            @Nullable String subtype, @Nullable String name, @Nullable String book, boolean listed,
            @Nullable String stackId, @Nullable String statement, ItemStack stack, String markdown) {
            this.id = id;
            this.source = source;
            this.module = module == null ? id.getNamespace() : module;
            this.type = type == null ? "other" : type;
            this.subtype = subtype == null ? "other" : subtype;
            this.name = name == null ? id.getPath() : name;
            this.book = book == null ? "buildcraftcore:main" : book;
            this.listed = listed;
            this.stackId = stackId;
            this.statement = statement;
            this.stack = stack;
            this.markdown = markdown;
            this.searchText = (title() + " " + id + " " + moduleName() + " " + plainText(markdown))
                .toLowerCase(Locale.ROOT);
        }

        public String title() {
            if (!stack.isEmpty()) {
                return stack.getHoverName().getString();
            }
            IStatement resolved = GuideContent.resolveStatement(statement);
            if (resolved != null && resolved.getDescription() != null) {
                return resolved.getDescription().getString();
            }
            String pageKey = "buildcraft.guide.page." + name;
            String translatedPage = translateOrLiteral(pageKey);
            if (!translatedPage.equals(pageKey)) {
                return translatedPage;
            }
            // Listed entries are named by their registry value in BC8. Their first authored chapter is a section of
            // the page, not the page title (using it caused dozens of unrelated entries to be called Pipe Mechanics).
            if (listed) {
                return titleCase(name);
            }
            Matcher heading = FIRST_HEADING.matcher(markdown);
            if (heading.find()) {
                return translateOrLiteral(heading.group(1).trim());
            }
            Matcher chapter = FIRST_CHAPTER.matcher(markdown);
            if (chapter.find()) {
                return translateOrLiteral(chapter.group(1));
            }
            return titleCase(name);
        }

        public String moduleName() {
            if (module.startsWith("buildcraft")) {
                String suffix = module.substring("buildcraft".length());
                if (suffix.isEmpty()) return translateOrLiteral("buildcraft.guide.chapter.mod.buildcraft");
                String translated = translateOrLiteral("buildcraft.guide.chapter.submod." + suffix);
                if (!translated.startsWith("buildcraft.guide.")) return translated;
                return "BuildCraft " + Character.toUpperCase(suffix.charAt(0)) + suffix.substring(1);
            }
            return module;
        }

        public String typeName() {
            String key = "buildcraft.guide.chapter.type." + type;
            String translated = translateOrLiteral(key);
            return translated.equals(key) ? titleCase(type) : translated;
        }

        public String subtypeName() {
            String key = "buildcraft.guide.chapter.subtype." + subtype;
            String translated = translateOrLiteral(key);
            return translated.equals(key) ? titleCase(subtype) : translated;
        }
    }

    static String translateOrLiteral(String value) {
        if (I18n.exists(value)) {
            return I18n.get(value);
        }
        return value;
    }

    static String titleCase(String value) {
        String[] words = value.replace('/', ' ').replace('_', ' ').split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) builder.append(word.substring(1));
        }
        return builder.toString();
    }

    private static String plainText(String markdown) {
        String text = TAGS.matcher(markdown).replaceAll(" ");
        text = text.replaceAll("(?m)^#+\\s*", " ");
        text = text.replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1");
        return text.replace("&lt;", "<").replace("&gt;", ">").replaceAll("\\s+", " ");
    }
}
