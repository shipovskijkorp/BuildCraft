package buildcraft.lib.client.guide;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OriginalGuideResourcesTester {
    private static final String MANIFEST = "/assets/buildcraft/guide/original_manifest.json";

    @Test
    void originalGuideManifestContainsEveryOfficialMarkdownPage() throws Exception {
        JsonObject root;
        try (InputStream stream = resource(MANIFEST);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        JsonArray entries = root.getAsJsonArray("entries");
        Assertions.assertTrue(entries.size() >= 111, "The current guide must retain every original BuildCraftGuide page");

        int listed = 0;
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            if (entry.get("listed").getAsBoolean()) listed++;
            String[] source = entry.get("source").getAsString().split(":", 2);
            Assertions.assertEquals(2, source.length, "Invalid guide resource location");
            try (InputStream ignored = resource("/assets/" + source[0] + "/" + source[1])) {
                // Opening every path is enough: the original registry intentionally contains one empty markdown page.
            }
        }
        Assertions.assertTrue(listed >= 106, "The current guide must retain every original contents entry");
    }


    @Test
    void currentProjectGuideCoversEveryAddedModuleAndMajorWorkflow() throws Exception {
        JsonObject root;
        try (InputStream stream = resource(MANIFEST);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        java.util.Set<String> ids = new java.util.HashSet<>();
        for (JsonElement element : root.getAsJsonArray("entries")) {
            ids.add(element.getAsJsonObject().get("id").getAsString());
        }
        String[] required = {
            "buildcraftbuilders:block/architect", "buildcraftbuilders:block/builder",
            "buildcraftbuilders:block/filler", "buildcraftbuilders:block/quarry",
            "buildcraftrobotics:item/robot", "buildcraftrobotics:block/zone_planner",
            "buildcraftrobotics:block/requester", "buildcraftrobotics:robot/builder",
            "buildcraftsilicon:block/assembly_table", "buildcraftsilicon:block/programming_table",
            "buildcrafttransport:pipe/wood_power", "buildcrafttransport:pipe/diamond_power",
            "buildcraftcore:item/map_location", "buildcraftfactory:item/water_gel",
            "buildcraftenergy:item/oil", "buildcraftenergy:item/fuel_gaseous",
            "buildcraftenergy:item/oil_residue", "buildcraftcompat:pipe/propolis_item"
        };
        for (String id : required) {
            Assertions.assertTrue(ids.contains(id), "Missing current-project guide entry " + id);
        }
    }

    @Test
    void robotCareerEntriesCarryVariantNbt() throws Exception {
        JsonObject root;
        try (InputStream stream = resource(MANIFEST);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        int careers = 0;
        for (JsonElement element : root.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            if (!entry.get("id").getAsString().startsWith("buildcraftrobotics:robot/")) continue;
            Assertions.assertEquals("buildcraftrobotics:robot", entry.get("stack").getAsString());
            Assertions.assertTrue(entry.has("stack_nbt"), "Robot career entry must preserve its board variant");
            net.minecraft.nbt.TagParser.parseTag(entry.get("stack_nbt").getAsString());
            careers++;
        }
        Assertions.assertEquals(17, careers, "Every registered robot career must be documented");
    }

    @Test
    void everyCurrentEnergyFluidFamilyHasAReadableGuidePage() throws Exception {
        JsonObject root;
        try (InputStream stream = resource(MANIFEST);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        java.util.Set<String> ids = new java.util.HashSet<>();
        JsonObject forestry = null;
        for (JsonElement element : root.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            ids.add(entry.get("id").getAsString());
            if ("buildcraftcompat:pipe/propolis_item".equals(entry.get("id").getAsString())) forestry = entry;
        }
        String[] fluids = {
            "oil", "oil_residue", "oil_heavy", "oil_dense", "oil_distilled",
            "fuel_dense", "fuel_mixed_heavy", "fuel_light", "fuel_mixed_light", "fuel_gaseous"
        };
        for (String fluid : fluids) {
            Assertions.assertTrue(ids.contains("buildcraftenergy:item/" + fluid),
                "Missing current Energy fluid guide for " + fluid);
        }
        Assertions.assertNotNull(forestry, "Missing Apiarist's Pipe guide entry");
        Assertions.assertEquals("forestry", forestry.get("requires_mod").getAsString());
    }

    @Test
    void everyListedGuidePageContainsAnOptionalPracticalHint() throws Exception {
        JsonObject root;
        try (InputStream stream = resource(MANIFEST);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        java.util.Set<String> checkedSources = new java.util.HashSet<>();
        for (JsonElement element : root.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            if (!entry.get("listed").getAsBoolean()) continue;
            String source = entry.get("source").getAsString();
            if (!checkedSources.add(source)) continue;
            String[] split = source.split(":", 2);
            String markdown;
            try (InputStream stream = resource("/assets/" + split[0] + "/" + split[1]);
                 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                StringBuilder text = new StringBuilder();
                char[] buffer = new char[1024];
                int read;
                while ((read = reader.read(buffer)) >= 0) text.append(buffer, 0, read);
                markdown = text.toString();
            }
            Assertions.assertTrue(markdown.contains("<hint>"), "Missing hint section in " + source);
            Assertions.assertTrue(markdown.contains("<bold>Hint:</bold>"), "Hint is not visibly labelled in " + source);
            GuideDocument hidden = GuideDocument.parse(markdown, true, false, false);
            GuideDocument visible = GuideDocument.parse(markdown, true, true, false);
            Assertions.assertTrue(visible.blocks.size() > hidden.blocks.size(),
                "Show Hints must reveal additional content in " + source);
        }
        Assertions.assertTrue(checkedSources.size() >= 204,
            "The current guide should retain practical hints for every listed page");
    }

    @Test
    void deprecatedOriginalGuideShortcutsRemainFunctional() throws Exception {
        String path = "/assets/buildcraft/guide/en_us/buildcraftlib/item/guide.md";
        String markdown;
        try (InputStream stream = resource(path);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            StringBuilder text = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) >= 0) text.append(buffer, 0, read);
            markdown = text.toString();
        }
        GuideDocument document = GuideDocument.parse(markdown, true, false, false);
        Assertions.assertTrue(document.blocks.stream().anyMatch(block ->
            block.kind == GuideDocument.Kind.RECIPES
                && "recipes_usages".equals(block.secondary)
                && "buildcraftlib:guide".equals(block.target)
        ));
        Assertions.assertTrue(document.blocks.stream().anyMatch(block -> block.kind == GuideDocument.Kind.NEW_PAGE));
    }

    @Test
    void originalGuideRegistrySourcesAreBundled() throws Exception {
        String[] namespaces = {
            "buildcraftlib", "buildcraftcore", "buildcraftbuilders", "buildcraftenergy",
            "buildcraftfactory", "buildcraftrobotics", "buildcraftsilicon", "buildcrafttransport",
            "buildcraftcompat"
        };
        for (String namespace : namespaces) {
            try (InputStream ignored = resource("/assets/buildcraft/guide/registry/" + namespace + ".txt")) {
                // All authored registry scripts are kept in the central guide resource tree.
            }
        }
        try (InputStream ignored = resource("/assets/buildcraft/guide/registry/util.txt")) {
            // Shared aliases used by the original guide registry scripts.
        }
    }

    @Test
    void guideInterfaceTexturesUseTheCentralBuildCraftResourceTree() throws Exception {
        String[] textures = {
            "cover.png", "icons.png", "left_page.png", "left_page_back.png", "left_page_first.png",
            "note.png", "right_page.png", "right_page_back.png", "right_page_last.png"
        };
        for (String texture : textures) {
            try (InputStream ignored = resource("/assets/buildcraft/guide/gui/" + texture)) {
                // The custom guide UI is stored beside the authored guide content.
            }
        }
    }

    @Test
    void everyGuidePageUsesTheCentralBuildCraftResourceTree() throws Exception {
        JsonObject root;
        try (InputStream stream = resource(MANIFEST);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        for (JsonElement element : root.getAsJsonArray("entries")) {
            String source = element.getAsJsonObject().get("source").getAsString();
            Assertions.assertTrue(source.startsWith("buildcraft:guide/en_us/"),
                "Guide page escaped the centralized resource tree: " + source);
        }
    }

    @Test
    void legacyRegistryReferencesMapAcrossEveryAffectedGuideCategory() {
        Assertions.assertEquals("buildcraftcore:gears/gear_wood",
            GuideContent.remapLegacyStackId("buildcraftcore:gear_wood", 0));
        Assertions.assertEquals("buildcraftcore:engine_redstone",
            GuideContent.remapLegacyStackId("buildcraftcore:engine", 0));
        Assertions.assertEquals("buildcraftenergy:engine_stone",
            GuideContent.remapLegacyStackId("buildcraftcore:engine", 1));
        Assertions.assertEquals("buildcraftenergy:engine_iron",
            GuideContent.remapLegacyStackId("buildcraftcore:engine", 2));
        Assertions.assertEquals("buildcraftsilicon:plug/lens",
            GuideContent.remapLegacyStackId("buildcraftsilicon:plug_filter", 0));
        Assertions.assertEquals("buildcrafttransport:wire/white",
            GuideContent.remapLegacyStackId("buildcrafttransport:wire", 0));
        Assertions.assertEquals("buildcrafttransport:quartz_fluid",
            GuideContent.remapLegacyStackId("buildcrafttransport:pipe_quartz_fluid", 0));
        Assertions.assertEquals("buildcrafttransport:cobblestone_item",
            GuideContent.remapLegacyStackId("buildcrafttransport:pipe_cobble_item", 0));
    }

    @Test
    void legacyStatementReferencesMapToCurrentRegistryKeys() {
        Assertions.assertEquals("buildcraft:pipe.wire.input.black.active",
            GuideContent.remapLegacyStatementId("buildcraft.pipe.wire.input.black.active"));
        Assertions.assertEquals("buildcraft:pipe_contains_items",
            GuideContent.remapLegacyStatementId("buildcraft:pipe.items.traversing"));
        Assertions.assertEquals("buildcraft:pipe_contains_fluids",
            GuideContent.remapLegacyStatementId("buildcraft:pipe.fluids.traversing"));
    }

    @Test
    void everyLegacyTransportStackInTheOfficialManifestUsesTheModernRegistryShape() throws Exception {
        JsonObject root;
        try (InputStream stream = resource(MANIFEST);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        int remappedPipes = 0;
        for (JsonElement element : root.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            if (!entry.has("stack")) continue;
            String legacy = entry.get("stack").getAsString();
            if (!legacy.startsWith("buildcrafttransport:pipe_")) continue;
            String modern = GuideContent.remapLegacyStackId(legacy, 0);
            Assertions.assertTrue(modern.startsWith("buildcrafttransport:"));
            Assertions.assertTrue(!modern.startsWith("buildcrafttransport:pipe_"));
            remappedPipes++;
        }
        Assertions.assertEquals(28, remappedPipes, "Every official item/fluid pipe entry must be migrated");
    }

    @Test
    void representativeArticleKeepsRecipesChaptersAndUsagesInSourceOrder() throws Exception {
        String path = "/assets/buildcraft/guide/en_us/buildcrafttransport/pipe/quartz_fluid.md";
        String markdown;
        try (InputStream stream = resource(path);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            StringBuilder text = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) >= 0) text.append(buffer, 0, read);
            markdown = text.toString();
        }

        GuideDocument document = GuideDocument.parse(markdown, true, false, false);
        List<GuideDocument.Block> blocks = document.blocks;
        int recipe = indexOf(blocks, GuideDocument.Kind.RECIPES, "recipes");
        int chapter = indexOf(blocks, GuideDocument.Kind.CHAPTER, null);
        int usages = indexOf(blocks, GuideDocument.Kind.RECIPES, "usages");
        Assertions.assertTrue(recipe >= 0 && chapter > recipe && usages > chapter);
        Assertions.assertEquals("buildcrafttransport:pipe_quartz_fluid", blocks.get(recipe).target);
        Assertions.assertEquals("Pipe Mechanics", blocks.get(chapter).text.getString());
    }

    private static int indexOf(List<GuideDocument.Block> blocks, GuideDocument.Kind kind, String secondary) {
        for (int index = 0; index < blocks.size(); index++) {
            GuideDocument.Block block = blocks.get(index);
            if (block.kind == kind && (secondary == null || secondary.equals(block.secondary))) return index;
        }
        return -1;
    }

    private static InputStream resource(String path) {
        InputStream stream = OriginalGuideResourcesTester.class.getResourceAsStream(path);
        Assertions.assertNotNull(stream, "Missing guide resource " + path);
        return stream;
    }
}
