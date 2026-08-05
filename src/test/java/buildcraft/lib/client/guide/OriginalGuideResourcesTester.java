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
    private static final String MANIFEST = "/assets/buildcraftlib/guide/original_manifest.json";

    @Test
    void originalGuideManifestContainsEveryOfficialMarkdownPage() throws Exception {
        JsonObject root;
        try (InputStream stream = resource(MANIFEST);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        JsonArray entries = root.getAsJsonArray("entries");
        Assertions.assertEquals(111, entries.size(), "Unexpected number of original BuildCraftGuide pages");

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
        Assertions.assertEquals(106, listed, "Unexpected number of registered contents entries");
    }


    @Test
    void deprecatedOriginalGuideShortcutsRemainFunctional() throws Exception {
        String path = "/assets/buildcraftlib/compat/buildcraft/guide/en_us/item/guide.md";
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
            "buildcraftlib", "buildcraftcore", "buildcraftenergy",
            "buildcraftfactory", "buildcraftsilicon", "buildcrafttransport"
        };
        for (String namespace : namespaces) {
            try (InputStream ignored = resource("/assets/" + namespace + "/compat/buildcraft/guide.txt")) {
                // Keep the original guide registry scripts available as an editable source of truth.
            }
        }
        try (InputStream ignored = resource("/assets/buildcraftlib/compat/buildcraft/guide/util.txt")) {
            // Shared aliases used by the original guide.txt files.
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
        String path = "/assets/buildcrafttransport/compat/buildcraft/guide/en_us/pipe/quartz_fluid.md";
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
