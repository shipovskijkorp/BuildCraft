package buildcraft.lib.client.guide;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OriginalGuideResourcesTester {
    private static final String ROOT = "/assets/buildcraft/guide/";
    private static final String MANIFEST = ROOT + "original_manifest.json";
    private static final String LAYOUTS = ROOT + "page_layouts.json";
    private static final String LANGUAGES = ROOT + "languages.json";
    private static final String ENGLISH = ROOT + "text/en_us.json";
    private static final Pattern SLOT = Pattern.compile("\\{\\{bc_text:(\\d+)\\}\\}");

    @Test
    void manifestAndPackedPagesCoverTheWholeCurrentGuide() throws Exception {
        JsonObject manifest = json(MANIFEST);
        JsonObject layouts = json(LAYOUTS).getAsJsonObject("pages");
        JsonObject english = json(ENGLISH).getAsJsonObject("pages");

        Assertions.assertEquals(5, manifest.get("format").getAsInt());
        Assertions.assertEquals(1, manifest.get("layout_format").getAsInt());
        Assertions.assertEquals(1, manifest.get("text_pack_format").getAsInt());

        JsonArray entries = manifest.getAsJsonArray("entries");
        Assertions.assertTrue(entries.size() >= 209, "The current guide must retain every documented entry");
        Set<String> pageKeys = new HashSet<>();
        int listed = 0;
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            Assertions.assertFalse(entry.has("source"), "Packed entries must not reference loose Markdown files");
            Assertions.assertTrue(entry.has("page"), "Every guide entry needs a language-neutral page key");
            pageKeys.add(entry.get("page").getAsString());
            if (entry.get("listed").getAsBoolean()) {
                listed++;
            }
        }
        Assertions.assertTrue(listed >= 204, "The current guide must retain every contents entry");
        Assertions.assertEquals(pageKeys, layouts.keySet(), "Manifest and shared layouts disagree");
        Assertions.assertEquals(pageKeys, english.keySet(), "Default English text pack is incomplete");
        Assertions.assertEquals(207, pageKeys.size(), "Unexpected number of distinct authored guide pages");
    }

    @Test
    void sharedLayoutsAndEnglishHaveMatchingSlots() throws Exception {
        JsonObject layouts = json(LAYOUTS).getAsJsonObject("pages");
        JsonObject english = json(ENGLISH).getAsJsonObject("pages");

        for (Map.Entry<String, JsonElement> entry : layouts.entrySet()) {
            String page = entry.getKey();
            JsonObject layout = entry.getValue().getAsJsonObject();
            String template = layout.get("template").getAsString();
            int slotCount = layout.get("slots").getAsInt();
            JsonArray en = english.getAsJsonArray(page);
            Assertions.assertEquals(slotCount, en.size(), "English slot mismatch in " + page);

            Set<Integer> indexes = new HashSet<>();
            Matcher matcher = SLOT.matcher(template);
            while (matcher.find()) {
                indexes.add(Integer.parseInt(matcher.group(1)));
            }
            for (int index = 0; index < slotCount; index++) {
                Assertions.assertTrue(indexes.contains(index), "Unused text slot " + index + " in " + page);
                Assertions.assertTrue(en.get(index).isJsonPrimitive() && en.get(index).getAsJsonPrimitive().isString(),
                    "English slot is not text in " + page + " #" + index);
                Assertions.assertFalse(en.get(index).getAsString().isBlank(),
                    "Default English text cannot be blank in " + page + " #" + index);
            }
            String renderedEnglish = render(template, strings(en));
            Assertions.assertFalse(SLOT.matcher(renderedEnglish).find(), "Unresolved English slot in " + page);
        }
    }

    @Test
    void regionalEnglishAliasesAndRussianFallbackAreConfigured() throws Exception {
        JsonObject languages = json(LANGUAGES);
        JsonObject aliases = languages.getAsJsonObject("aliases");
        for (String locale : List.of("en_au", "en_ca", "en_gb", "en_nz")) {
            Assertions.assertEquals("en_us", aliases.get(locale).getAsString());
        }
        JsonArray russianFallback = languages.getAsJsonObject("fallbacks").getAsJsonArray("ru_ru");
        Assertions.assertEquals(1, russianFallback.size());
        Assertions.assertEquals("en_us", russianFallback.get(0).getAsString());

        Map<String, String> aliasMap = new HashMap<>();
        aliases.entrySet().forEach(entry -> aliasMap.put(entry.getKey(), entry.getValue().getAsString()));
        Map<String, List<String>> fallbackMap = Map.of("ru_ru", List.of("en_us"));
        Assertions.assertEquals("en_us", GuidePageStore.resolveAlias("EN-gb", aliasMap));
        Assertions.assertEquals(List.of("en_us"),
            GuidePageStore.buildLoadOrder("en_us", "en_gb", aliasMap, fallbackMap));
        Assertions.assertEquals(List.of("en_us", "ru_ru"),
            GuidePageStore.buildLoadOrder("en_us", "ru_ru", aliasMap, fallbackMap));
        String resolvedEnglish = GuidePageStore.resolveAlias("EN-GB", aliasMap);
        Assertions.assertEquals("buildcraft:guide/text/en_us.json",
            GuidePageStore.textPackLocation(resolvedEnglish).toString());
    }

    @Test
    void currentProjectGuideCoversEveryAddedModuleAndMajorWorkflow() throws Exception {
        JsonObject root = json(MANIFEST);
        Set<String> ids = new HashSet<>();
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
        JsonObject root = json(MANIFEST);
        int careers = 0;
        for (JsonElement element : root.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            if (!entry.get("id").getAsString().startsWith("buildcraftrobotics:robot/")) {
                continue;
            }
            Assertions.assertEquals("buildcraftrobotics:robot", entry.get("stack").getAsString());
            Assertions.assertTrue(entry.has("stack_nbt"), "Robot career entry must preserve its board variant");
            net.minecraft.nbt.TagParser.parseTag(entry.get("stack_nbt").getAsString());
            careers++;
        }
        Assertions.assertEquals(17, careers, "Every registered robot career must be documented");
    }

    @Test
    void everyCurrentEnergyFluidFamilyHasAReadableGuidePage() throws Exception {
        JsonObject root = json(MANIFEST);
        Set<String> ids = new HashSet<>();
        JsonObject forestry = null;
        for (JsonElement element : root.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            ids.add(entry.get("id").getAsString());
            if ("buildcraftcompat:pipe/propolis_item".equals(entry.get("id").getAsString())) {
                forestry = entry;
            }
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
    void everyListedPageHasOptionalHints() throws Exception {
        JsonObject manifest = json(MANIFEST);
        JsonObject layouts = json(LAYOUTS).getAsJsonObject("pages");
        JsonObject english = json(ENGLISH).getAsJsonObject("pages");
        Set<String> checked = new HashSet<>();

        for (JsonElement element : manifest.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            if (!entry.get("listed").getAsBoolean()) {
                continue;
            }
            String page = entry.get("page").getAsString();
            if (!checked.add(page)) {
                continue;
            }
            String template = layouts.getAsJsonObject(page).get("template").getAsString();
            String en = render(template, strings(english.getAsJsonArray(page)));
            Assertions.assertTrue(en.contains("<hint>"), "Missing hint section in " + page);
            Assertions.assertTrue(en.contains("<bold>Hint:</bold>"), "English hint is not labelled in " + page);

            GuideDocument hidden = GuideDocument.parse(en, true, false, false);
            GuideDocument visible = GuideDocument.parse(en, true, true, false);
            Assertions.assertTrue(visible.blocks.size() > hidden.blocks.size(),
                "Show Hints must reveal additional content in " + page);
        }
        Assertions.assertTrue(checked.size() >= 204,
            "The current guide should retain practical hints for every listed page");
    }

    @Test
    void deprecatedOriginalGuideShortcutsRemainFunctional() throws Exception {
        String markdown = rendered("buildcraftlib/item/guide", ENGLISH);
        GuideDocument document = GuideDocument.parse(markdown, true, false, false);
        Assertions.assertTrue(document.blocks.stream().anyMatch(block ->
            block.kind == GuideDocument.Kind.RECIPES
                && "recipes_usages".equals(block.secondary)
                && "buildcraftlib:guide".equals(block.target)
        ));
        Assertions.assertTrue(document.blocks.stream().anyMatch(block -> block.kind == GuideDocument.Kind.NEW_PAGE));
    }

    @Test
    void originalGuideRegistrySourcesAndInterfaceTexturesAreBundled() throws Exception {
        String[] namespaces = {
            "buildcraftlib", "buildcraftcore", "buildcraftbuilders", "buildcraftenergy",
            "buildcraftfactory", "buildcraftrobotics", "buildcraftsilicon", "buildcrafttransport",
            "buildcraftcompat"
        };
        for (String namespace : namespaces) {
            try (InputStream ignored = resource(ROOT + "registry/" + namespace + ".txt")) {
                // Centralized legacy registry scripts retained for resource compatibility.
            }
        }
        try (InputStream ignored = resource(ROOT + "registry/util.txt")) {
            // Shared registry aliases.
        }
        String[] textures = {
            "icons.png", "left_page.png", "left_page_back.png", "left_page_first.png",
            "note.png", "right_page.png", "right_page_back.png", "right_page_last.png"
        };
        for (String texture : textures) {
            try (InputStream ignored = resource(ROOT + "gui/" + texture)) {
                // The Guide Book interface remains beside the packed content.
            }
        }
    }

    @Test
    void looseMarkdownPagesAreNotBundledAnymore() {
        Assertions.assertNull(OriginalGuideResourcesTester.class.getResourceAsStream(
            ROOT + "en_us/buildcrafttransport/pipe/quartz_fluid.md"));
        Assertions.assertNull(OriginalGuideResourcesTester.class.getResourceAsStream(
            ROOT + "pages/en_us.json"));
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
    void everyLegacyTransportStackInTheManifestUsesTheModernRegistryShape() throws Exception {
        JsonObject root = json(MANIFEST);
        int remappedPipes = 0;
        for (JsonElement element : root.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            if (!entry.has("stack")) {
                continue;
            }
            String legacy = entry.get("stack").getAsString();
            if (!legacy.startsWith("buildcrafttransport:pipe_")) {
                continue;
            }
            String modern = GuideContent.remapLegacyStackId(legacy, 0);
            Assertions.assertTrue(modern.startsWith("buildcrafttransport:"));
            Assertions.assertFalse(modern.startsWith("buildcrafttransport:pipe_"));
            remappedPipes++;
        }
        Assertions.assertEquals(28, remappedPipes, "Every official item/fluid pipe entry must be migrated");
    }

    @Test
    void representativeArticleKeepsRecipesChaptersAndUsagesInSourceOrder() throws Exception {
        String markdown = rendered("buildcrafttransport/pipe/quartz_fluid", ENGLISH);
        GuideDocument document = GuideDocument.parse(markdown, true, false, false);
        List<GuideDocument.Block> blocks = document.blocks;
        int recipe = indexOf(blocks, GuideDocument.Kind.RECIPES, "recipes");
        int chapter = indexOf(blocks, GuideDocument.Kind.CHAPTER, null);
        int usages = indexOf(blocks, GuideDocument.Kind.RECIPES, "usages");
        Assertions.assertTrue(recipe >= 0 && chapter > recipe && usages > chapter);
        Assertions.assertEquals("buildcrafttransport:pipe_quartz_fluid", blocks.get(recipe).target);
        Assertions.assertEquals("Pipe Mechanics", blocks.get(chapter).text.getString());
    }

    @Test
    void coreDistributionBundlesOnlyEnglishLocalizationData() throws Exception {
        Path ordinaryDirectory = Path.of("src/main/resources/assets/buildcraft/lang");
        Path guideDirectory = Path.of("src/main/resources/assets/buildcraft/guide/text");

        Assertions.assertEquals(List.of("en_us.json"), jsonFileNames(ordinaryDirectory),
            "Non-English interface translations belong in BuildCraft Community Edition: Localizations");
        Assertions.assertEquals(List.of("en_us.json"), jsonFileNames(guideDirectory),
            "Non-English Guide Book text packs belong in BuildCraft Community Edition: Localizations");
    }

    private static List<String> jsonFileNames(Path directory) throws Exception {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .filter(name -> name.endsWith(".json"))
                .sorted()
                .toList();
        }
    }

    private static String rendered(String page, String languagePath) throws Exception {
        JsonObject layout = json(LAYOUTS).getAsJsonObject("pages").getAsJsonObject(page);
        JsonArray text = json(languagePath).getAsJsonObject("pages").getAsJsonArray(page);
        return render(layout.get("template").getAsString(), strings(text));
    }

    private static List<String> strings(JsonArray array) {
        List<String> values = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            values.add(element == null || element.isJsonNull() ? null : element.getAsString());
        }
        return values;
    }

    private static String render(String template, List<String> values) {
        Matcher matcher = SLOT.matcher(template);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            String value = index < values.size() && values.get(index) != null ? values.get(index) : "";
            matcher.appendReplacement(output, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static int indexOf(List<GuideDocument.Block> blocks, GuideDocument.Kind kind, String secondary) {
        for (int index = 0; index < blocks.size(); index++) {
            GuideDocument.Block block = blocks.get(index);
            if (block.kind == kind && (secondary == null || secondary.equals(block.secondary))) {
                return index;
            }
        }
        return -1;
    }

    private static JsonObject json(String path) throws Exception {
        try (InputStream stream = resource(path);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static InputStream resource(String path) {
        InputStream stream = OriginalGuideResourcesTester.class.getResourceAsStream(path);
        Assertions.assertNotNull(stream, "Missing guide resource " + path);
        return stream;
    }
}
