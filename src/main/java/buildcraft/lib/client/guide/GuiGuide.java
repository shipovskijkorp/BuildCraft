/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.lib.client.guide;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.registries.ForgeRegistries;

import buildcraft.api.core.render.ISprite;
import buildcraft.api.recipes.IngredientStack;
import buildcraft.api.statements.IStatement;
import buildcraft.lib.BCLibConfig;
import buildcraft.lib.client.sprite.SpriteNineSliced;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.misc.ColourUtil;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.recipe.AssemblyRecipeBasic;

/**
 * Native BuildCraft guide screen backed by the original BC8 guide registry and markdown pages.
 * <p>
 * The cover/opening animation is intentionally skipped: using the item opens directly on the first guide spread.
 */
public final class GuiGuide extends Screen {
    private static final ResourceLocation LEFT_PAGE =
        new ResourceLocation("buildcraftlib", "textures/gui/guide/left_page.png");
    private static final ResourceLocation RIGHT_PAGE =
        new ResourceLocation("buildcraftlib", "textures/gui/guide/right_page.png");
    private static final ResourceLocation LEFT_PAGE_FIRST =
        new ResourceLocation("buildcraftlib", "textures/gui/guide/left_page_first.png");
    private static final ResourceLocation RIGHT_PAGE_BACK =
        new ResourceLocation("buildcraftlib", "textures/gui/guide/right_page_back.png");
    private static final ResourceLocation RIGHT_PAGE_LAST =
        new ResourceLocation("buildcraftlib", "textures/gui/guide/right_page_last.png");
    private static final ResourceLocation ICONS =
        new ResourceLocation("buildcraftlib", "textures/gui/guide/icons.png");

    private static final int PAGE_TEXTURE_WIDTH = 193;
    private static final int PAGE_TEXTURE_HEIGHT = 248;
    private static final int BOOK_WIDTH = PAGE_TEXTURE_WIDTH * 2;
    private static final int BOOK_HEIGHT = PAGE_TEXTURE_HEIGHT;
    private static final int PAGE_TEXT_WIDTH = 168;
    private static final int PAGE_TEXT_HEIGHT = 190;
    private static final int PAGE_TEXT_TOP = 25;
    private static final int CONTENT_ENTRY_HEIGHT = 17;
    private static final int CONTENT_SUBHEADING_HEIGHT = 15;
    private static final int CONTENT_CHAPTER_HEIGHT = 19;

    private static final GuiIcon CHAPTER_MARKER_ICON = new GuiIcon(ICONS, 0, 56, 32, 32);
    private static final GuiIcon CHAPTER_MARKER_LEFT_ICON = new GuiIcon(ICONS, 0, 56, 24, 32);
    private static final SpriteNineSliced CHAPTER_BAR =
        new SpriteNineSliced(CHAPTER_MARKER_ICON.sprite, 8, 8, 24, 24, 32);
    private static final SpriteNineSliced CHAPTER_TAB_LEFT =
        new SpriteNineSliced(CHAPTER_MARKER_LEFT_ICON.sprite, 8, 8, 24, 24, 24, 32);

    private static final GuiIcon SEARCH_ICON = new GuiIcon(ICONS, 26, 196, 12, 12);
    private static final GuiIcon SEARCH_TAB_CLOSED = new GuiIcon(ICONS, 58, 196, 14, 6);
    private static final GuiIcon SEARCH_TAB_OPEN = new GuiIcon(ICONS, 40, 209, 106, 14);
    /** Exact crafting grid sprite and dimensions used by BC8's GuideCrafting. */
    private static final GuiIcon CRAFTING_GRID = new GuiIcon(ICONS, 119, 0, 116, 54);
    /** Exact furnace sprite and dimensions used by BC8's GuideSmelting. */
    private static final GuiIcon SMELTING_GRID = new GuiIcon(ICONS, 119, 54, 80, 54);
    /** Exact assembly-table sprite and dimensions used by BC8's GuideAssembly. */
    private static final GuiIcon ASSEMBLY_GRID = new GuiIcon(ICONS, 119, 108, 98, 54);
    private static final int[] DOCUMENT_CHAPTER_COLOURS = { 0x9DD5C0, 0xFAC174, 0x27A4DD };

    private static final List<String> MAIN_TYPE_ORDER =
        List.of("action", "block", "item", "pipe", "trigger");

    /**
     * Logical category order for the Community Edition guide.
     * <p>
     * The old implementation sorted translated subtype and entry names alphabetically. That scattered related
     * progression chains (gears, engines, pipes, robotics and refining products) across the contents pages. The
     * manifest is authored in gameplay order, while this table controls the order of the category headings.
     */
    private static final Map<String, List<String>> SUBTYPE_ORDER = Map.of(
        "action", List.of("basic", "automation", "pipe_plug", "pipe_item", "robot", "robot_station"),
        "block", List.of("engine", "mining", "fluid", "refining", "automation", "construction", "laser",
            "robot_control"),
        "item", List.of("gear", "component", "tool", "area", "blueprint", "robot_control",
            "robot_station", "robot", "fluid", "pipe_plug"),
        "pipe", List.of("pipe_item", "pipe_fluid", "pipe_power"),
        "trigger", List.of("basic", "item", "fluid", "engine", "automation", "pipe_item", "pipe_fluid",
            "pipe_plug", "robot")
    );
    private static final Map<String, Integer> CHAPTER_COLOURS = Map.of(
        // Exact colour cycle used by GuideChapter.COLOURS in BC8.
        "action", 0x9DD5C0,
        "block", 0xFAC174,
        "item", 0x27A4DD,
        "pipe", 0x9DD5C0,
        "trigger", 0xFAC174
    );

    private static final int TEXT_COLOUR = 0x30251D;
    private static final int MUTED_COLOUR = 0x716355;
    private static final int LINK_COLOUR = 0x315E86;
    private static final int CHAPTER_COLOUR = 0x8F6D43;
    private static final int PAGE_NUMBER_COLOUR = 0x90816A;
    private static final int HOVER_COLOUR = 0xFFD3AD6C;
    private static final int SELECTED_COLOUR = 0x66C6A778;

    private final GuideContent content;
    private final Map<ResourceLocation, Integer> manifestOrder = new LinkedHashMap<>();
    private final List<GuideContent.Entry> filteredEntries = new ArrayList<>();
    private final List<ContentsPage> contentsPages = new ArrayList<>();
    private final List<ChapterTab> contentsChapters = new ArrayList<>();
    private final List<ClickRegion> clickRegions = new ArrayList<>();
    private final Deque<PageState> history = new ArrayDeque<>();

    private int left;
    private int top;
    private int tick;
    private int searchX;
    private int searchY;
    private int contentsSpread;
    private int documentSpread;
    private boolean showLore = true;
    private boolean showHints;
    private SortMode sortMode = SortMode.TYPE;
    private View view = View.CONTENTS;
    private @Nullable GuideContent.Entry currentEntry;
    private @Nullable DocumentLayout document;
    private @Nullable EditBox searchBox;
    private @Nullable ItemStack hoveredStack;
    private @Nullable Component hoveredText;

    private enum View {
        CONTENTS,
        DOCUMENT
    }

    private enum SortMode {
        TYPE,
        MODULE,
        ALPHABETICAL
    }

    private GuiGuide() {
        super(Component.translatable("item.buildcraft.guide.name"));
        content = GuideContent.load();
        int order = 0;
        for (GuideContent.Entry entry : content.getAllEntries()) {
            manifestOrder.put(entry.id, order++);
        }
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new GuiGuide());
    }

    @Override
    protected void init() {
        super.init();
        left = (width - BOOK_WIDTH) / 2;
        top = (height - BOOK_HEIGHT) / 2;
        String oldSearch = searchBox == null ? "" : searchBox.getValue();
        searchX = left + 46;
        searchY = top + 9;
        searchBox = new EditBox(font, searchX, searchY, 80, 13, Component.literal("Search"));
        searchBox.setMaxLength(80);
        searchBox.setBordered(false);
        searchBox.setTextColor(TEXT_COLOUR);
        searchBox.setValue(oldSearch);
        searchBox.setResponder(value -> rebuildContents());
        rebuildContents();
        updateSearchVisibility();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String search = searchBox == null ? "" : searchBox.getValue();
        super.resize(minecraft, width, height);
        if (searchBox != null) {
            searchBox.setValue(search);
        }
    }

    @Override
    public void tick() {
        tick++;
        if (searchBox != null) searchBox.tick();
    }

    private void rebuildContents() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        filteredEntries.clear();
        for (GuideContent.Entry entry : content.getListedEntries()) {
            // The original item opens the main BuildCraft book. The three buildcraftlib:meta pages belong to the
            // separate configuration guide and must not leak into this contents tree.
            if (!"buildcraftcore:main".equals(entry.book) || !MAIN_TYPE_ORDER.contains(entry.type)) {
                continue;
            }
            if (query.isEmpty() || entry.searchText.contains(query)) {
                filteredEntries.add(entry);
            }
        }

        Comparator<GuideContent.Entry> comparator;
        switch (sortMode) {
            case MODULE:
                comparator = Comparator.comparingInt((GuideContent.Entry entry) -> moduleIndex(entry.module))
                    .thenComparingInt(entry -> typeIndex(entry.type))
                    .thenComparingInt(entry -> subtypeIndex(entry.type, entry.subtype))
                    .thenComparingInt(this::manifestIndex);
                break;
            case ALPHABETICAL:
                comparator = Comparator.comparing(entry -> entry.title().toLowerCase(Locale.ROOT));
                break;
            case TYPE:
            default:
                comparator = Comparator.comparingInt((GuideContent.Entry entry) -> typeIndex(entry.type))
                    .thenComparingInt(entry -> subtypeIndex(entry.type, entry.subtype))
                    .thenComparingInt(this::manifestIndex);
                break;
        }
        filteredEntries.sort(comparator.thenComparing(entry -> entry.id.toString()));
        buildContentsPages();
        contentsSpread = Mth.clamp(contentsSpread, 0, maxContentsSpread());
    }

    private void buildContentsPages() {
        contentsPages.clear();
        contentsChapters.clear();

        List<ContentsLine> lines = new ArrayList<>();
        switch (sortMode) {
            case MODULE:
                appendModuleOrderedLines(lines);
                break;
            case ALPHABETICAL:
                appendAlphabeticalLines(lines);
                break;
            case TYPE:
            default:
                appendTypeOrderedLines(lines);
                break;
        }

        if (lines.isEmpty()) {
            lines.add(ContentsLine.message(Component.literal("No results")));
        }

        ContentsPage page = new ContentsPage();
        contentsPages.add(page);
        int usedHeight = 0;
        for (int index = 0; index < lines.size(); index++) {
            ContentsLine line = lines.get(index);
            int required = line.height;
            if (line.kind != ContentsLineKind.ENTRY && index + 1 < lines.size()) {
                required += lines.get(index + 1).height;
            }
            if (!page.lines.isEmpty() && usedHeight + required > PAGE_TEXT_HEIGHT) {
                page = new ContentsPage();
                contentsPages.add(page);
                usedHeight = 0;
            }
            line.y = usedHeight;
            page.lines.add(line);
            if (line.kind == ContentsLineKind.CHAPTER) {
                contentsChapters.add(new ChapterTab(
                    line.groupKey, line.component.getString(), line.colour, contentsPages.size() + 1
                ));
            }
            usedHeight += line.height;
        }
    }

    private void appendTypeOrderedLines(List<ContentsLine> lines) {
        for (String type : MAIN_TYPE_ORDER) {
            List<GuideContent.Entry> typeEntries = filteredEntries.stream()
                .filter(entry -> type.equals(entry.type))
                .collect(Collectors.toList());
            if (typeEntries.isEmpty()) continue;

            String typeName = typeEntries.get(0).typeName();
            lines.add(ContentsLine.chapter(type, typeName, chapterColour(type)));

            Map<String, List<GuideContent.Entry>> bySubtype = new LinkedHashMap<>();
            for (GuideContent.Entry entry : typeEntries) {
                bySubtype.computeIfAbsent(entry.subtype, ignored -> new ArrayList<>()).add(entry);
            }
            for (List<GuideContent.Entry> subtypeEntries : bySubtype.values()) {
                lines.add(ContentsLine.subheading(subtypeEntries.get(0).subtypeName()));
                for (GuideContent.Entry entry : subtypeEntries) lines.add(ContentsLine.entry(entry));
            }
        }
    }

    private void appendModuleOrderedLines(List<ContentsLine> lines) {
        Map<String, List<GuideContent.Entry>> byModule = new LinkedHashMap<>();
        for (GuideContent.Entry entry : filteredEntries) {
            byModule.computeIfAbsent(entry.module, ignored -> new ArrayList<>()).add(entry);
        }
        int chapterIndex = 0;
        for (Map.Entry<String, List<GuideContent.Entry>> module : byModule.entrySet()) {
            List<GuideContent.Entry> moduleEntries = module.getValue();
            if (moduleEntries.isEmpty()) continue;
            String groupKey = "module:" + module.getKey();
            lines.add(ContentsLine.chapter(groupKey, moduleEntries.get(0).moduleName(),
                originalChapterColour(chapterIndex++)));

            Map<String, List<GuideContent.Entry>> byType = new LinkedHashMap<>();
            for (GuideContent.Entry entry : moduleEntries) {
                byType.computeIfAbsent(entry.type, ignored -> new ArrayList<>()).add(entry);
            }
            for (List<GuideContent.Entry> typeEntries : byType.values()) {
                lines.add(ContentsLine.subheading(typeEntries.get(0).typeName()));
                for (GuideContent.Entry entry : typeEntries) lines.add(ContentsLine.entry(entry));
            }
        }
    }

    private void appendAlphabeticalLines(List<ContentsLine> lines) {
        // The third BC8 TypeOrder has no grouping tags at all: it is a single flat, alphabetically sorted list.
        for (GuideContent.Entry entry : filteredEntries) {
            lines.add(ContentsLine.entry(entry));
        }
    }

    private static int originalChapterColour(int index) {
        switch (Math.floorMod(index, 3)) {
            case 0: return 0x9DD5C0;
            case 1: return 0xFAC174;
            default: return 0x27A4DD;
        }
    }

    private static int typeIndex(String type) {
        int index = MAIN_TYPE_ORDER.indexOf(type);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private static int subtypeIndex(String type, String subtype) {
        List<String> order = SUBTYPE_ORDER.get(type);
        if (order == null) return Integer.MAX_VALUE;
        int index = order.indexOf(subtype);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private int manifestIndex(GuideContent.Entry entry) {
        return manifestOrder.getOrDefault(entry.id, Integer.MAX_VALUE);
    }

    private static int moduleIndex(String module) {
        switch (module) {
            case "buildcraftcore": return 0;
            case "buildcraftbuilders": return 1;
            case "buildcraftenergy": return 2;
            case "buildcraftfactory": return 3;
            case "buildcraftrobotics": return 4;
            case "buildcraftsilicon": return 5;
            case "buildcrafttransport": return 6;
            case "buildcraftcompat": return 7;
            default: return 100;
        }
    }

    private static int chapterColour(String key) {
        Integer colour = CHAPTER_COLOURS.get(key);
        if (colour != null) return colour;
        int hash = key.hashCode();
        int red = 112 + ((hash >>> 16) & 0x3F);
        int green = 112 + ((hash >>> 8) & 0x3F);
        int blue = 112 + (hash & 0x3F);
        return (red << 16) | (green << 8) | blue;
    }

    private void updateSearchVisibility() {
        if (searchBox != null) {
            searchBox.setVisible(view == View.CONTENTS && contentsSpread > 0);
            if (searchBox.isFocused()) {
                // EditBox#setFocused(boolean) is protected in 1.19.2. Clicking outside the widget
                // clears focus through its public input path without depending on protected API.
                searchBox.mouseClicked(-1, -1, 0);
            }
        }
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        hoveredStack = null;
        hoveredText = null;
        clickRegions.clear();

        renderBookBackground(pose);
        if (view == View.CONTENTS) {
            renderContents(pose, mouseX, mouseY);
        } else {
            renderDocument(pose, mouseX, mouseY);
        }
        renderNavigation(pose, mouseX, mouseY);

        if (hoveredStack != null && !hoveredStack.isEmpty()) {
            renderTooltip(pose, hoveredStack, mouseX, mouseY);
        } else if (hoveredText != null) {
            renderTooltip(pose, hoveredText, mouseX, mouseY);
        }
    }

    private void renderBookBackground(PoseStack pose) {
        int firstPage = currentFirstPage();
        int pageCount = currentPageCount();

        RenderSystem.setShaderTexture(0, firstPage == 0 ? LEFT_PAGE_FIRST : LEFT_PAGE);
        blit(pose, left, top, 0, 0, PAGE_TEXTURE_WIDTH, PAGE_TEXTURE_HEIGHT, 256, 256);

        ResourceLocation rightTexture;
        if (firstPage + 1 >= pageCount) {
            rightTexture = RIGHT_PAGE_BACK;
        } else if (firstPage + 1 == pageCount - 1) {
            rightTexture = RIGHT_PAGE_LAST;
        } else {
            rightTexture = RIGHT_PAGE;
        }
        RenderSystem.setShaderTexture(0, rightTexture);
        blit(pose, left + PAGE_TEXTURE_WIDTH, top, 0, 0, PAGE_TEXTURE_WIDTH, PAGE_TEXTURE_HEIGHT, 256, 256);
    }

    private void renderContents(PoseStack pose, int mouseX, int mouseY) {
        int firstPage = contentsSpread * 2;
        if (firstPage == 0) {
            renderContentsIntroLeft(pose, mouseX, mouseY);
            renderContentsIntroRight(pose);
        } else {
            renderContentsPage(pose, firstPage, left + 23, mouseX, mouseY);
            renderContentsPage(pose, firstPage + 1, left + PAGE_TEXTURE_WIDTH + 4, mouseX, mouseY);
        }
        // BC8 draws the search tab on every even page, including the title page. The ordering buttons only appear
        // once the actual contents start on page three.
        renderContentsSearch(pose, mouseX, mouseY, firstPage > 0);
        renderContentsChapters(pose, mouseX, mouseY);
    }

    private void renderContentsIntroLeft(PoseStack pose, int mouseX, int mouseY) {
        int pageX = left + 23;
        // GuidePageContents starts at the normal page text origin. The two manually wrapped lines reproduce the
        // original 3x title while still fitting Minecraft's modern font metrics.
        float titleScale = 3.0F;
        int titleLineHeight = Math.round(font.lineHeight * titleScale);
        drawScaledCentred(pose, "BuildCraft", pageX, top + PAGE_TEXT_TOP, PAGE_TEXT_WIDTH, titleScale, 0x17120E);
        drawScaledCentred(pose, "Guide Book", pageX, top + PAGE_TEXT_TOP + titleLineHeight,
            PAGE_TEXT_WIDTH, titleScale, 0x17120E);
        drawCentred(pose, Component.literal("Community Edition"), pageX,
            top + PAGE_TEXT_TOP + titleLineHeight * 2,
            PAGE_TEXT_WIDTH, TEXT_COLOUR);

        drawScaledCentred(pose, "Options", pageX, top + PAGE_TEXT_TOP + PAGE_TEXT_HEIGHT - 80,
            PAGE_TEXT_WIDTH, 2.0F, 0x17120E);
        String lore = "Show Lore " + (showLore ? "[x]" : "[ ]");
        String hints = "Show Hints " + (showHints ? "[x]" : "[ ]");
        int loreX = pageX + (PAGE_TEXT_WIDTH - font.width(lore)) / 2;
        int hintX = pageX + (PAGE_TEXT_WIDTH - font.width(hints)) / 2;
        int loreY = top + PAGE_TEXT_TOP + PAGE_TEXT_HEIGHT - 52;
        int hintY = top + PAGE_TEXT_TOP + PAGE_TEXT_HEIGHT - 38;
        font.draw(pose, lore, loreX, loreY, isInside(mouseX, mouseY, loreX, loreY, font.width(lore), 10)
            ? LINK_COLOUR : TEXT_COLOUR);
        font.draw(pose, hints, hintX, hintY, isInside(mouseX, mouseY, hintX, hintY, font.width(hints), 10)
            ? LINK_COLOUR : TEXT_COLOUR);
        clickRegions.add(new ClickRegion(loreX, loreY, font.width(lore), 11, () -> {
            showLore = !showLore;
            rebuildOpenDocument();
        }));
        clickRegions.add(new ClickRegion(hintX, hintY, font.width(hints), 11, () -> {
            showHints = !showHints;
            rebuildOpenDocument();
        }));
    }

    private void renderContentsIntroRight(PoseStack pose) {
        int pageX = left + PAGE_TEXTURE_WIDTH + 4;
        List<String> modules = new ArrayList<>();
        addLoadedModule(modules, "buildcraftcore", "BuildCraft Core");
        addLoadedModule(modules, "buildcraftbuilders", "BuildCraft Builders");
        addLoadedModule(modules, "buildcraftenergy", "BuildCraft Energy");
        addLoadedModule(modules, "buildcraftfactory", "BuildCraft Factory");
        addLoadedModule(modules, "buildcraftrobotics", "BuildCraft Robotics");
        addLoadedModule(modules, "buildcraftsilicon", "BuildCraft Silicon");
        addLoadedModule(modules, "buildcrafttransport", "BuildCraft Transport");
        addLoadedModule(modules, "buildcraftcompat", "BuildCraft Compat");

        int perLineHeight = font.lineHeight + 3;
        int blockHeight = (modules.size() + 1) * perLineHeight;
        int y = top + PAGE_TEXT_TOP + (PAGE_TEXT_HEIGHT - blockHeight) / 2;
        Component heading = Component.literal("Loaded Mods:").withStyle(ChatFormatting.BOLD);
        drawCentred(pose, heading, pageX, y, PAGE_TEXT_WIDTH, 0x17120E);
        y += perLineHeight;
        for (String module : modules) {
            drawCentred(pose, Component.literal(module), pageX, y, PAGE_TEXT_WIDTH, TEXT_COLOUR);
            y += perLineHeight;
        }
    }

    private void addLoadedModule(List<String> modules, String id, String displayName) {
        for (GuideContent.Entry entry : content.getListedEntries()) {
            if ("buildcraftcore:main".equals(entry.book) && id.equals(entry.module)) {
                modules.add(displayName);
                return;
            }
        }
    }

    private void renderContentsPage(PoseStack pose, int individualPage, int pageX, int mouseX, int mouseY) {
        int contentIndex = individualPage - 2;
        if (contentIndex < 0 || contentIndex >= contentsPages.size()) return;
        ContentsPage page = contentsPages.get(contentIndex);
        for (ContentsLine line : page.lines) {
            renderContentsLine(pose, line, pageX, top + PAGE_TEXT_TOP + line.y, mouseX, mouseY);
        }
    }

    private void renderContentsSearch(PoseStack pose, int mouseX, int mouseY, boolean showOrders) {
        int pageX = left + 23;
        boolean open = searchBox != null && (searchBox.isFocused() || !searchBox.getValue().isEmpty());
        if (open) {
            SEARCH_TAB_OPEN.drawAt(pose, pageX - 2, top + 3);
            SEARCH_ICON.drawAt(pose, pageX + 8, top + 7);
        } else {
            SEARCH_TAB_CLOSED.drawAt(pose, pageX + 8, top + 5);
            SEARCH_ICON.drawAt(pose, pageX + 8, top + 6);
        }
        if (showOrders) {
            renderSortButtons(pose, mouseX, mouseY);
        }
        if (searchBox != null) {
            searchBox.render(pose, mouseX, mouseY, 0);
            clickRegions.add(new ClickRegion(pageX - 2, top + 3, 106, 16, () -> {
                if (contentsSpread == 0) {
                    contentsSpread = Math.min(1, maxContentsSpread());
                    updateSearchVisibility();
                }
                searchBox.mouseClicked(searchX + 1, searchY + 1, 0);
            }));
        }
    }

    private void renderSortButtons(PoseStack pose, int mouseX, int mouseY) {
        int x = left + 13;
        int y = top + 15;
        for (int index = 0; index < SortMode.values().length; index++) {
            SortMode mode = SortMode.values()[index];
            boolean selected = sortMode == mode;
            boolean hovered = isInside(mouseX, mouseY, x, y + index * 14, 14, 14);
            RenderSystem.setShaderTexture(0, ICONS);
            int u = index * 14;
            int v = selected ? 14 : 0;
            if (hovered) v += 28;
            blit(pose, x, y + index * 14, u, v, 14, 14, 256, 256);
            int clickY = y + index * 14;
            clickRegions.add(new ClickRegion(x, clickY, 14, 14, () -> {
                sortMode = mode;
                rebuildContents();
                contentsSpread = Math.min(Math.max(1, contentsSpread), maxContentsSpread());
                updateSearchVisibility();
            }));
        }
    }

    private void renderContentsLine(PoseStack pose, ContentsLine line, int x, int y, int mouseX, int mouseY) {
        switch (line.kind) {
            case CHAPTER: {
                drawTintedNineSlice(pose, CHAPTER_BAR, x + 7, y - 4, PAGE_TEXT_WIDTH - 24, 16, line.colour);
                Component text = line.component.copy().withStyle(ChatFormatting.UNDERLINE);
                font.draw(pose, text, x + 16, y, TEXT_COLOUR);
                break;
            }
            case SUBHEADING: {
                int textX = x + 32;
                Component text = line.component.copy().withStyle(ChatFormatting.UNDERLINE);
                font.draw(pose, text, textX, y, TEXT_COLOUR);
                break;
            }
            case ENTRY: {
                GuideContent.Entry entry = line.entry;
                if (entry == null) break;
                int iconX = x + 14;
                int iconY = y - 5;
                int textX = x + 32;
                boolean hovered = isInside(mouseX, mouseY, x + 12, y - 5, PAGE_TEXT_WIDTH - 12, 18);
                if (hovered) {
                    int titleWidth = Math.min(font.width(entry.title()), PAGE_TEXT_WIDTH - 34);
                    fill(pose, textX - 2, y - 2, textX + titleWidth + 2, y + 12, HOVER_COLOUR);
                }
                renderEntryIcon(pose, entry, iconX, iconY, mouseX, mouseY);
                String title = font.plainSubstrByWidth(entry.title(), PAGE_TEXT_WIDTH - 34);
                font.draw(pose, title, textX, y, entryTextColour(entry));
                clickRegions.add(new ClickRegion(x + 12, y - 5, PAGE_TEXT_WIDTH - 12, 18,
                    () -> openEntry(entry, true)));
                break;
            }
            case MESSAGE:
                drawCentred(pose, line.component, x, y + 4, PAGE_TEXT_WIDTH, MUTED_COLOUR);
                break;
            default:
                break;
        }
    }

    private void renderEntryIcon(PoseStack pose, GuideContent.Entry entry, int x, int y, int mouseX, int mouseY) {
        if (!entry.stack.isEmpty()) {
            itemRenderer.renderAndDecorateItem(entry.stack, x, y);
            if (isInside(mouseX, mouseY, x, y, 16, 16)) hoveredStack = entry.stack;
            return;
        }
        IStatement statement = GuideContent.resolveStatement(entry.statement);
        ISprite sprite = statement == null ? null : statement.getSprite();
        if (sprite != null) {
            GuiIcon.drawAt(pose, sprite, x, y, 16, 16);
            return;
        }
        int colour = chapterColour(entry.type);
        fill(pose, x + 2, y + 2, x + 14, y + 14, 0xFF000000 | colour);
        fill(pose, x + 4, y + 4, x + 12, y + 12, 0xFF202020);
    }

    private static int entryTextColour(GuideContent.Entry entry) {
        if (entry.stack.isEmpty()) {
            return TEXT_COLOUR;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(entry.stack.getItem());
        if (id == null) {
            return TEXT_COLOUR;
        }
        DyeColor colour = null;
        if (id.getNamespace().equals("buildcrafttransport") && id.getPath().startsWith("wire/")) {
            colour = DyeColor.byName(id.getPath().substring("wire/".length()), null);
        } else if (id.getNamespace().equals("buildcraftsilicon") && id.getPath().equals("plug/lens")) {
            int damage = entry.stack.getDamageValue();
            if (damage < 32) {
                colour = DyeColor.byId(damage & 15);
            }
        }
        return colour == null || !BCLibConfig.useColouredLabels
            ? TEXT_COLOUR : ColourUtil.getLightHex(colour);
    }


    private void renderContentsChapters(PoseStack pose, int mouseX, int mouseY) {
        if (contentsChapters.isEmpty()) return;
        int step = font.lineHeight + 8;
        for (int index = 0; index < contentsChapters.size(); index++) {
            ChapterTab tab = contentsChapters.get(index);
            int textWidth = font.width(tab.label);
            int y = top + step * (index + 1);
            boolean hovered = isInside(mouseX, mouseY, left - textWidth - 5, y - 4, textWidth + 16, 16);
            int extension = hovered ? 5 : 0;
            int x = left - textWidth - extension + 5;
            drawTintedNineSlice(pose, CHAPTER_TAB_LEFT, x - 6, y - 4,
                textWidth + 12 + extension, 16, tab.colour);
            font.draw(pose, Component.literal(tab.label).withStyle(ChatFormatting.UNDERLINE), x, y, TEXT_COLOUR);
            clickRegions.add(new ClickRegion(left - textWidth - 5 - extension, y - 4,
                textWidth + 16 + extension, 16, () -> {
                    contentsSpread = Mth.clamp(tab.pageIndex / 2, 0, maxContentsSpread());
                    updateSearchVisibility();
                }));
        }
    }

    private static void drawTintedNineSlice(PoseStack pose, SpriteNineSliced sprite, double x, double y,
        double width, double height, int colour) {
        float red = ((colour >>> 16) & 0xFF) / 255.0F;
        float green = ((colour >>> 8) & 0xFF) / 255.0F;
        float blue = (colour & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        sprite.draw(pose, x, y, width, height);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void openEntry(GuideContent.Entry entry, boolean clearHistory) {
        if (clearHistory) history.clear();
        currentEntry = entry;
        document = layoutDocument(entry);
        documentSpread = 0;
        view = View.DOCUMENT;
        updateSearchVisibility();
    }

    private void openLinkedEntry(GuideContent.Entry entry) {
        if (currentEntry != null) history.push(new PageState(currentEntry, documentSpread));
        currentEntry = entry;
        document = layoutDocument(entry);
        documentSpread = 0;
        view = View.DOCUMENT;
        updateSearchVisibility();
    }

    private void rebuildOpenDocument() {
        if (currentEntry != null) {
            int oldSpread = documentSpread;
            document = layoutDocument(currentEntry);
            documentSpread = Mth.clamp(oldSpread, 0, document.maxSpread());
        }
    }

    private void renderDocument(PoseStack pose, int mouseX, int mouseY) {
        if (currentEntry == null || document == null) {
            returnToContents();
            return;
        }
        String title = font.plainSubstrByWidth(currentEntry.title(), BOOK_WIDTH - 70);
        drawCentred(pose, Component.literal(title), left + 23, top + 10,
            PAGE_TEXT_WIDTH * 2 + 3, CHAPTER_COLOUR);

        int firstPage = documentSpread * 2;
        renderDocumentPage(pose, document.page(firstPage), left + 23, top + PAGE_TEXT_TOP, mouseX, mouseY);
        renderDocumentPage(pose, document.page(firstPage + 1), left + PAGE_TEXTURE_WIDTH + 4,
            top + PAGE_TEXT_TOP, mouseX, mouseY);
        renderDocumentChapters(pose, mouseX, mouseY);
    }

    private void renderDocumentChapters(PoseStack pose, int mouseX, int mouseY) {
        if (document == null) return;
        int step = font.lineHeight + 8;
        int tabIndex = 0;
        tabIndex = drawDocumentChapterTab(pose, mouseX, mouseY, tabIndex, "Contents",
            DOCUMENT_CHAPTER_COLOURS[0], this::returnToContents);
        for (DocumentChapter chapter : document.chapters) {
            if (chapter.level != 0) continue;
            int targetSpread = chapter.pageIndex / 2;
            tabIndex = drawDocumentChapterTab(pose, mouseX, mouseY, tabIndex, chapter.title,
                chapter.colour, () -> {
                    documentSpread = Mth.clamp(targetSpread, 0, document.maxSpread());
                    updateSearchVisibility();
                });
        }
    }

    private int drawDocumentChapterTab(PoseStack pose, int mouseX, int mouseY, int index, String rawLabel,
        int colour, Runnable action) {
        String label = font.plainSubstrByWidth(rawLabel, 128);
        int textWidth = font.width(label);
        int y = top + (font.lineHeight + 8) * (index + 1);
        boolean hovered = isInside(mouseX, mouseY, left - textWidth - 5, y - 4, textWidth + 16, 16);
        int extension = hovered ? 5 : 0;
        int x = left - textWidth - extension + 5;
        drawTintedNineSlice(pose, CHAPTER_TAB_LEFT, x - 6, y - 4,
            textWidth + 12 + extension, 16, colour);
        font.draw(pose, Component.literal(label).withStyle(ChatFormatting.UNDERLINE), x, y, TEXT_COLOUR);
        clickRegions.add(new ClickRegion(left - textWidth - 5 - extension, y - 4,
            textWidth + 16 + extension, 16, action));
        return index + 1;
    }

    private void renderDocumentPage(PoseStack pose, @Nullable RenderPage page, int originX, int originY,
        int mouseX, int mouseY) {
        if (page == null) return;
        for (RenderElement element : page.elements) {
            int x = originX + element.x;
            int y = originY + element.y;
            switch (element.kind) {
                case TEXT:
                    font.draw(pose, element.line, x, y, TEXT_COLOUR);
                    if (element.target != null) {
                        if (isInside(mouseX, mouseY, x, y, element.width, 10)) {
                            fill(pose, x, y + 9, x + element.width, y + 10, 0xAA315E86);
                        }
                        clickRegions.add(new ClickRegion(x, y, Math.max(1, element.width), 10,
                            () -> followTarget(element.target, null)));
                    }
                    break;
                case CHAPTER:
                    if (element.chapterBar) {
                        int indent = Math.max(0, element.x - 12);
                        drawTintedNineSlice(pose, CHAPTER_BAR, x - 5, y - 4,
                            Math.max(24, PAGE_TEXT_WIDTH - 24 - indent), element.height, element.colour);
                    }
                    font.draw(pose, element.line, x, y, TEXT_COLOUR);
                    break;
                case CODE:
                    fill(pose, x - 2, y - 1, x + PAGE_TEXT_WIDTH - 2, y + 10, 0x356A5A49);
                    font.draw(pose, element.line, x, y, MUTED_COLOUR);
                    break;
                case LINK:
                    renderDocumentLink(pose, element, x, y, mouseX, mouseY);
                    break;
                case IMAGE:
                    renderDocumentImage(pose, element, x, y, mouseX, mouseY);
                    break;
                case RECIPE:
                    renderRecipe(pose, element.recipe, element.stack, x, y, mouseX, mouseY);
                    break;
                default:
                    break;
            }
        }
    }

    private void renderDocumentLink(PoseStack pose, RenderElement element, int x, int y, int mouseX, int mouseY) {
        boolean hovered = isInside(mouseX, mouseY, x, y, PAGE_TEXT_WIDTH, 19);
        if (hovered) fill(pose, x, y, x + PAGE_TEXT_WIDTH, y + 19, HOVER_COLOUR);
        if (element.stack != null && !element.stack.isEmpty()) {
            itemRenderer.renderAndDecorateItem(element.stack, x + 1, y + 1);
            if (isInside(mouseX, mouseY, x + 1, y + 1, 16, 16)) hoveredStack = element.stack;
        }
        String title = element.component == null ? element.target : element.component.getString();
        if (title == null) title = "Missing link";
        font.draw(pose, font.plainSubstrByWidth(title, 143), x + 21, y + 5,
            hovered ? LINK_COLOUR : TEXT_COLOUR);
        String target = element.target;
        String secondary = element.secondary;
        clickRegions.add(new ClickRegion(x, y, PAGE_TEXT_WIDTH, 19, () -> followTarget(target, secondary)));
    }

    private void renderDocumentImage(PoseStack pose, RenderElement element, int x, int y, int mouseX, int mouseY) {
        if (element.stack != null && !element.stack.isEmpty()) {
            pose.pushPose();
            float scale = Math.max(1.0F, Math.min(element.width, element.height) / 16.0F);
            pose.translate(x + (element.width - 16 * scale) / 2.0F, y, 0);
            pose.scale(scale, scale, 1);
            itemRenderer.renderAndDecorateItem(element.stack, 0, 0);
            pose.popPose();
            int itemX = x + Math.round((element.width - 16 * scale) / 2.0F);
            int itemWidth = Math.max(16, Math.round(16 * scale));
            registerStackInteraction(element.stack, itemX, y, itemWidth, Math.max(16, Math.round(16 * scale)),
                mouseX, mouseY);
            return;
        }
        if (element.texture != null) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, element.texture);
            int sourceWidth = Math.max(1, element.sourceWidth);
            int sourceHeight = Math.max(1, element.sourceHeight);
            pose.pushPose();
            pose.translate(x, y, 0);
            pose.scale(element.width / (float) sourceWidth, element.height / (float) sourceHeight, 1);
            blit(pose, 0, 0, 0, 0, sourceWidth, sourceHeight, sourceWidth, sourceHeight);
            pose.popPose();
        }
    }

    private void followTarget(@Nullable String target, @Nullable String type) {
        if (target == null || target.isEmpty()) return;
        if (target.startsWith("http://") || target.startsWith("https://")) {
            try {
                Util.getPlatform().openUri(new URI(target));
            } catch (Exception ignored) {
            }
            return;
        }
        if ("item_stack".equals(type)) {
            ItemStack stack = GuideContent.resolveStackForTag(target);
            if (stack.isEmpty()) return;
            GuideContent.Entry matching = findByStack(stack);
            openLinkedEntry(matching == null ? GuideContent.createGeneratedItemEntry(stack) : matching);
            return;
        }
        GuideContent.Entry entry = content.get(target);
        if (entry != null) openLinkedEntry(entry);
    }

    @Nullable
    private GuideContent.Entry findByStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        GuideContent.Entry itemFallback = null;
        for (GuideContent.Entry entry : content.getAllEntries()) {
            if (entry.stack.isEmpty() || entry.stack.getItem() != stack.getItem()) continue;
            if (itemFallback == null) itemFallback = entry;
            if (entry.stack.getDamageValue() == stack.getDamageValue()
                && ItemStack.isSameItemSameTags(entry.stack, stack)) {
                return entry;
            }
        }
        return itemFallback;
    }

    private DocumentLayout layoutDocument(GuideContent.Entry entry) {
        GuideDocument parsed = GuideDocument.parse(entry.markdown, showLore, showHints, BCLibConfig.guideShowDetail);
        LayoutBuilder layout = new LayoutBuilder(entry.title());
        for (GuideDocument.Block block : parsed.blocks) {
            switch (block.kind) {
                case TEXT:
                    layout.addText(block.text == null ? Component.empty() : block.text, block.target);
                    break;
                case SPACE:
                    layout.addSpace(6);
                    break;
                case CHAPTER:
                    layout.addChapter(block.text == null ? Component.empty() : block.text, block.level);
                    break;
                case NEW_PAGE:
                    layout.newPage();
                    break;
                case CODE:
                    layout.addCode(block.text == null ? Component.empty() : block.text);
                    break;
                case LINK:
                    layout.addLink(block.target, block.secondary);
                    break;
                case IMAGE:
                    layout.addImage(block.target, block.width, block.height);
                    break;
                case RECIPES:
                    layout.addRecipeTag(block.secondary, block.target, block.attributes);
                    break;
                default:
                    break;
            }
        }
        // BC8 appended all crafting information to item pages after loading their authored page parts.
        // Avoid duplicates when the markdown already contains explicit <recipe(s)> or <usage(s)> tags.
        if (!entry.stack.isEmpty()) {
            layout.addAutomaticCrafting(entry.stack);
        }
        return layout.finish();
    }

    private final class LayoutBuilder {
        private final List<RenderPage> pages = new ArrayList<>();
        private final List<DocumentChapter> chapters = new ArrayList<>();
        private RenderPage page = new RenderPage();
        private final Set<ResourceLocation> renderedRecipes = new LinkedHashSet<>();
        private int y;
        // Contents is colour 0. The synthetic page title starts at colour 1, exactly like GuidePage in BC8.
        private int chapterColourIndex = 1;

        LayoutBuilder(String title) {
            pages.add(page);
            addChapter(Component.literal(title), 0);
        }

        void newPage() {
            page = new RenderPage();
            pages.add(page);
            y = 0;
        }

        void ensure(int height) {
            if (y > 0 && y + height > PAGE_TEXT_HEIGHT) newPage();
        }

        void addSpace(int height) {
            if (y > 0) y = Math.min(PAGE_TEXT_HEIGHT, y + height);
        }

        void addText(Component component, @Nullable String target) {
            List<FormattedCharSequence> lines = font.split(component, PAGE_TEXT_WIDTH);
            if (lines.isEmpty()) {
                addSpace(6);
                return;
            }
            for (FormattedCharSequence line : lines) {
                ensure(10);
                int width = font.width(line);
                page.elements.add(RenderElement.text(y, line, width, target));
                y += 10;
            }
            y += 2;
        }

        void addChapter(Component component, int level) {
            int safeLevel = Math.max(0, level);
            int indent = Math.min(20, safeLevel * 7);
            Component styled = component.copy().withStyle(ChatFormatting.UNDERLINE);
            List<FormattedCharSequence> lines = font.split(styled, PAGE_TEXT_WIDTH - 24 - indent);
            if (lines.isEmpty()) lines = List.of(Component.empty().getVisualOrderText());
            int blockHeight = Math.max(16, lines.size() * 11 + 6);
            // GuideChapter guaranteed room for roughly four text rows, preventing a chapter marker from being left
            // alone at the bottom of a page while its first paragraph starts on the next one.
            ensure(Math.max(blockHeight, font.lineHeight * 4));

            int colour = DOCUMENT_CHAPTER_COLOURS[Math.floorMod(chapterColourIndex++, DOCUMENT_CHAPTER_COLOURS.length)];
            int pageIndex = pages.size() - 1;
            chapters.add(new DocumentChapter(component.getString(), colour, pageIndex, safeLevel));
            for (int index = 0; index < lines.size(); index++) {
                page.elements.add(RenderElement.chapter(12 + indent, y, lines.get(index), colour,
                    index == 0, blockHeight));
                y += 11;
            }
            y += 7;
        }

        void addCode(Component component) {
            List<FormattedCharSequence> lines = font.split(component, PAGE_TEXT_WIDTH - 4);
            if (lines.isEmpty()) lines = List.of(Component.empty().getVisualOrderText());
            for (FormattedCharSequence line : lines) {
                ensure(10);
                page.elements.add(RenderElement.code(2, y, line));
                y += 10;
            }
        }

        void addLink(@Nullable String target, @Nullable String type) {
            ensure(20);
            GuideContent.Entry linked = target == null ? null : content.get(target);
            ItemStack stack = ItemStack.EMPTY;
            Component title;
            if ("item_stack".equals(type)) {
                stack = GuideContent.resolveStackForTag(target);
                title = stack.isEmpty() ? Component.literal(target == null ? "Missing item" : target)
                    : stack.getHoverName();
            } else if (linked != null) {
                stack = linked.stack;
                title = Component.literal(linked.title());
            } else {
                title = Component.literal(target == null ? "Missing link" : target);
            }
            page.elements.add(RenderElement.link(y, title, target, type, stack));
            y += 21;
        }

        void addImage(@Nullable String source, int requestedWidth, int requestedHeight) {
            if (source == null) return;
            ItemStack imageStack = imageStack(source);
            int width = requestedWidth > 0 ? requestedWidth : 160;
            int height = requestedHeight > 0 ? requestedHeight : 160;
            if (!imageStack.isEmpty() && requestedWidth <= 0) width = 64;
            if (!imageStack.isEmpty() && requestedHeight <= 0) height = 64;
            if (width > PAGE_TEXT_WIDTH) {
                float scale = PAGE_TEXT_WIDTH / (float) width;
                width = PAGE_TEXT_WIDTH;
                height = Math.max(1, Math.round(height * scale));
            }
            if (height > PAGE_TEXT_HEIGHT) {
                float scale = PAGE_TEXT_HEIGHT / (float) height;
                height = PAGE_TEXT_HEIGHT;
                width = Math.max(1, Math.round(width * scale));
            }
            ensure(height + 4);
            int x = (PAGE_TEXT_WIDTH - width) / 2;
            ResourceLocation texture = imageStack.isEmpty() ? textureLocation(source) : null;
            int[] sourceSize = sourceTextureSize(source);
            page.elements.add(RenderElement.image(x, y, width, height, sourceSize[0], sourceSize[1], texture, imageStack));
            y += height + 4;
        }

        void addRecipeTag(@Nullable String tagType, @Nullable String rawStack, java.util.Map<String, String> attributes) {
            if (tagType == null) return;
            ItemStack stack = GuideContent.resolveStackForTag(rawStack, attributes);
            if (stack.isEmpty()) return;
            List<Recipe<?>> recipes = recipesFor(stack);
            List<Recipe<?>> usages = usagesFor(stack);
            switch (tagType) {
                case "recipe":
                    if (!recipes.isEmpty()) addRecipe(recipes.get(0), stack);
                    break;
                case "recipes":
                    for (Recipe<?> recipe : recipes) addRecipe(recipe, stack);
                    break;
                case "usages":
                    for (Recipe<?> recipe : usages) addRecipe(recipe, ItemStack.EMPTY);
                    break;
                case "recipes_usages":
                    if (!recipes.isEmpty()) {
                        newPage();
                        addChapter(recipeChapter(true, recipes.size()),
                            parseInt(attributes.get("chapter_level"), 0));
                        for (Recipe<?> recipe : recipes) addRecipe(recipe, stack);
                    }
                    Set<ResourceLocation> recipeIds = recipes.stream().map(Recipe::getId).collect(Collectors.toSet());
                    List<Recipe<?>> uniqueUsages = usages.stream().filter(recipe -> !recipeIds.contains(recipe.getId()))
                        .collect(Collectors.toList());
                    if (!uniqueUsages.isEmpty()) {
                        // BC8 only forced another page here when the recipe section did not contain exactly one
                        // recipe. A single recipe and a single usage are allowed to share a page when they fit.
                        if (recipes.size() != 1) newPage();
                        addChapter(recipeChapter(false, uniqueUsages.size()),
                            parseInt(attributes.get("chapter_level"), 0));
                        for (Recipe<?> recipe : uniqueUsages) addRecipe(recipe, ItemStack.EMPTY);
                    }
                    break;
                default:
                    break;
            }
        }

        void addRecipe(Recipe<?> recipe, ItemStack focusedOutput) {
            if (!renderedRecipes.add(recipe.getId())) return;
            ensure(60);
            page.elements.add(RenderElement.recipe(y, recipe, focusedOutput));
            y += 60;
        }

        void addAutomaticCrafting(ItemStack stack) {
            List<Recipe<?>> recipes = recipesFor(stack);
            List<Recipe<?>> usages = usagesFor(stack);
            Set<ResourceLocation> directIds = recipes.stream().map(Recipe::getId).collect(Collectors.toSet());
            List<Recipe<?>> missingRecipes = recipes.stream()
                .filter(recipe -> !renderedRecipes.contains(recipe.getId()))
                .collect(Collectors.toList());
            List<Recipe<?>> missingUsages = usages.stream()
                .filter(recipe -> !directIds.contains(recipe.getId()))
                .filter(recipe -> !renderedRecipes.contains(recipe.getId()))
                .collect(Collectors.toList());
            if (!missingRecipes.isEmpty()) {
                newPage();
                addChapter(recipeChapter(true, missingRecipes.size()), 0);
                for (Recipe<?> recipe : missingRecipes) addRecipe(recipe, stack);
            }
            if (!missingUsages.isEmpty()) {
                if (missingUsages.size() != 1) newPage();
                addChapter(recipeChapter(false, missingUsages.size()), 0);
                for (Recipe<?> recipe : missingUsages) addRecipe(recipe, ItemStack.EMPTY);
            }
        }

        private Component recipeChapter(boolean creating, int count) {
            String key = creating
                ? (count == 1 ? "buildcraft.guide.recipe.create" : "buildcraft.guide.recipe.create.plural")
                : (count == 1 ? "buildcraft.guide.recipe.use" : "buildcraft.guide.recipe.use.plural");
            return Component.literal(GuideContent.translateOrLiteral(key));
        }

        DocumentLayout finish() {
            while (pages.size() > 1 && pages.get(pages.size() - 1).elements.isEmpty()) {
                pages.remove(pages.size() - 1);
            }
            return new DocumentLayout(pages, chapters);
        }
    }

    private ItemStack imageStack(String source) {
        try {
            ResourceLocation location = new ResourceLocation(source);
            String path = location.getPath();
            if (path.startsWith("items/")) {
                String itemPath = path.substring("items/".length());
                return GuideContent.resolveStackForTag(location.getNamespace() + ":" + itemPath);
            }
            if (!path.startsWith("textures/") && !path.endsWith(".png")) {
                Item item = ForgeRegistries.ITEMS.getValue(location);
                if (item != null) return item.getDefaultInstance();
            }
        } catch (RuntimeException ignored) {
        }
        return ItemStack.EMPTY;
    }

    private int[] sourceTextureSize(String source) {
        if (source.endsWith("marker_path.png") || source.endsWith("guide_book.png")) {
            return new int[] { 16, 16 };
        }
        // The only full GUI image referenced by the original pages is the combustion-engine screen.
        return new int[] { 256, 256 };
    }

    @Nullable
    private ResourceLocation textureLocation(String source) {
        try {
            return new ResourceLocation(source);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private List<Recipe<?>> recipesFor(ItemStack output) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return List.of();
        return minecraft.level.getRecipeManager().getRecipes().stream()
            .filter(recipe -> recipeOutputs(recipe).stream().anyMatch(result -> guideStacksMatch(output, result)))
            .sorted(Comparator.comparing(recipe -> recipe.getId().toString()))
            .collect(Collectors.toList());
    }

    private List<Recipe<?>> usagesFor(ItemStack input) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return List.of();
        return minecraft.level.getRecipeManager().getRecipes().stream()
            .filter(recipe -> {
                if (recipe instanceof AssemblyRecipeBasic) {
                    ItemStack result = focusedRecipeOutput(recipe, ItemStack.EMPTY);
                    if (!result.isEmpty()) {
                        try {
                            return ((AssemblyRecipeBasic) recipe).getInputsFor(result).stream()
                                .anyMatch(definition -> definition.ingredient.test(input));
                        } catch (RuntimeException ignored) {
                            // Fall back to the generic ingredient list below for malformed/dynamic recipes.
                        }
                    }
                }
                return recipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(input));
            })
            .sorted(Comparator.comparing(recipe -> recipe.getId().toString()))
            .collect(Collectors.toList());
    }

    private static List<ItemStack> recipeOutputs(Recipe<?> recipe) {
        List<ItemStack> outputs = new ArrayList<>();
        if (recipe instanceof AssemblyRecipeBasic) {
            try {
                for (ItemStack preview : ((AssemblyRecipeBasic) recipe).getOutputPreviews()) {
                    if (preview != null && !preview.isEmpty()) outputs.add(preview);
                }
            } catch (RuntimeException ignored) {
                // A dynamic assembly recipe may only expose getResultItem in the current registry state.
            }
        }
        ItemStack result = recipe.getResultItem();
        if (!result.isEmpty() && outputs.stream().noneMatch(stack -> guideStacksMatch(stack, result))) {
            outputs.add(result);
        }
        return outputs;
    }

    private static boolean guideStacksMatch(ItemStack requested, ItemStack candidate) {
        if (requested.isEmpty() || candidate.isEmpty() || requested.getItem() != candidate.getItem()) return false;
        // BC8's recipe indices matched metadata variants, which is essential for lenses, filters and other legacy
        // damage-value items. Only require NBT equality when the authored target actually specifies NBT.
        if (requested.getDamageValue() != candidate.getDamageValue()) return false;
        return !requested.hasTag() || ItemStack.isSameItemSameTags(requested, candidate);
    }

    private ItemStack focusedRecipeOutput(Recipe<?> recipe, ItemStack requested) {
        if (!requested.isEmpty()) {
            for (ItemStack output : recipeOutputs(recipe)) {
                // Keep the recipe's concrete NBT/output count. Generic guide targets such as the facade item often
                // omit the dynamic state data that the assembly recipe needs to resolve its real ingredients.
                if (guideStacksMatch(requested, output)) return output.copy();
            }
        }
        List<ItemStack> outputs = recipeOutputs(recipe);
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).copy();
    }

    private void renderRecipe(PoseStack pose, @Nullable Recipe<?> recipe, @Nullable ItemStack requestedOutput,
        int x, int y, int mouseX, int mouseY) {
        if (recipe == null) return;
        ItemStack focus = requestedOutput == null ? ItemStack.EMPTY : requestedOutput;
        if (recipe instanceof AssemblyRecipeBasic) {
            renderAssemblyRecipe(pose, (AssemblyRecipeBasic) recipe, focus, x, y, mouseX, mouseY);
        } else if (recipe instanceof AbstractCookingRecipe) {
            renderSmeltingRecipe(pose, (AbstractCookingRecipe) recipe, focus, x, y, mouseX, mouseY);
        } else {
            renderCraftingRecipe(pose, recipe, focus, x, y, mouseX, mouseY);
        }
    }

    private void renderCraftingRecipe(PoseStack pose, Recipe<?> recipe, ItemStack requestedOutput,
        int x, int y, int mouseX, int mouseY) {
        CRAFTING_GRID.drawAt(pose, x, y);

        List<Ingredient> ingredients = recipe.getIngredients();
        int recipeWidth = 3;
        int recipeHeight = 3;
        if (recipe instanceof IShapedRecipe<?>) {
            recipeWidth = Mth.clamp(((IShapedRecipe<?>) recipe).getRecipeWidth(), 1, 3);
            recipeHeight = Mth.clamp(((IShapedRecipe<?>) recipe).getRecipeHeight(), 1, 3);
        } else if (ingredients.size() <= 3) {
            recipeWidth = Math.max(1, ingredients.size());
            recipeHeight = 1;
        } else if (ingredients.size() <= 6) {
            recipeWidth = 3;
            recipeHeight = 2;
        }

        int ingredientIndex = 0;
        for (int row = 0; row < recipeHeight; row++) {
            for (int column = 0; column < recipeWidth; column++) {
                if (ingredientIndex >= ingredients.size()) break;
                ItemStack stack = ingredientStack(ingredients.get(ingredientIndex), ingredientIndex);
                int slotX = x + 1 + column * 18;
                int slotY = y + 1 + row * 18;
                if (!stack.isEmpty()) {
                    itemRenderer.renderAndDecorateItem(stack, slotX, slotY);
                    itemRenderer.renderGuiItemDecorations(font, stack, slotX, slotY);
                    registerStackInteraction(stack, slotX, slotY, 16, 16, mouseX, mouseY);
                }
                ingredientIndex++;
            }
        }

        ItemStack result = focusedRecipeOutput(recipe, requestedOutput);
        int resultX = x + 95;
        int resultY = y + 19;
        renderRecipeStack(result, resultX, resultY, mouseX, mouseY);
    }

    private void renderSmeltingRecipe(PoseStack pose, AbstractCookingRecipe recipe, ItemStack requestedOutput,
        int x, int y, int mouseX, int mouseY) {
        SMELTING_GRID.drawAt(pose, x, y);
        List<Ingredient> ingredients = recipe.getIngredients();
        ItemStack input = ingredients.isEmpty() ? ItemStack.EMPTY : ingredientStack(ingredients.get(0), 0);
        renderRecipeStack(input, x + 1, y + 1, mouseX, mouseY);
        renderRecipeStack(focusedRecipeOutput(recipe, requestedOutput), x + 59, y + 19, mouseX, mouseY);
        renderRecipeStack(new ItemStack(Items.FURNACE), x + 1, y + 37, mouseX, mouseY);
    }

    private void renderAssemblyRecipe(PoseStack pose, AssemblyRecipeBasic recipe, ItemStack requestedOutput,
        int x, int y, int mouseX, int mouseY) {
        ASSEMBLY_GRID.drawAt(pose, x, y);
        ItemStack output = focusedRecipeOutput(recipe, requestedOutput);
        List<IngredientStack> inputs = new ArrayList<>();
        if (!output.isEmpty()) {
            try {
                inputs.addAll(recipe.getInputsFor(output));
            } catch (RuntimeException ignored) {
                // Use the generic ingredients below if a dynamic recipe cannot resolve this preview output.
            }
        }
        if (inputs.isEmpty()) {
            int index = 0;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (index >= 6) break;
                ItemStack stack = ingredientStack(ingredient, index);
                renderRecipeStack(stack, x + 1 + (index % 2) * 18, y + 1 + (index / 2) * 18,
                    mouseX, mouseY);
                index++;
            }
        } else {
            for (int index = 0; index < Math.min(6, inputs.size()); index++) {
                IngredientStack definition = inputs.get(index);
                ItemStack stack = ingredientStack(definition.ingredient, index);
                if (!stack.isEmpty()) stack.setCount(Math.max(1, definition.count));
                renderRecipeStack(stack, x + 1 + (index % 2) * 18, y + 1 + (index / 2) * 18,
                    mouseX, mouseY);
            }
        }
        renderRecipeStack(output, x + 77, y + 19, mouseX, mouseY);
        if (!output.isEmpty() && isInside(mouseX, mouseY, x + 50, y + 4, 6, 46)) {
            try {
                hoveredText = LocaleUtil.localizeMj(recipe.getRequiredMicroJoulesFor(output));
            } catch (RuntimeException ignored) {
                // A broken third-party recipe should not make the guide screen unusable.
            }
        }
    }

    private void renderRecipeStack(ItemStack stack, int x, int y, int mouseX, int mouseY) {
        if (stack.isEmpty()) return;
        itemRenderer.renderAndDecorateItem(stack, x, y);
        itemRenderer.renderGuiItemDecorations(font, stack, x, y);
        registerStackInteraction(stack, x, y, 16, 16, mouseX, mouseY);
    }

    private void registerStackInteraction(ItemStack stack, int x, int y, int width, int height,
        int mouseX, int mouseY) {
        if (stack.isEmpty()) return;
        ItemStack copy = stack.copy();
        if (isInside(mouseX, mouseY, x, y, width, height)) hoveredStack = copy;
        clickRegions.add(new ClickRegion(x, y, width, height, () -> openStackPage(copy)));
    }

    private void openStackPage(ItemStack stack) {
        GuideContent.Entry matching = findByStack(stack);
        openLinkedEntry(matching == null ? GuideContent.createGeneratedItemEntry(stack) : matching);
    }

    private ItemStack ingredientStack(Ingredient ingredient, int offset) {
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) return ItemStack.EMPTY;
        return stacks[Math.floorMod(tick / 30 + offset, stacks.length)];
    }

    private void renderNavigation(PoseStack pose, int mouseX, int mouseY) {
        int firstPage = currentFirstPage();
        int pageCount = currentPageCount();
        int spread = view == View.CONTENTS ? contentsSpread : documentSpread;
        int maxSpread = Math.max(0, (pageCount - 1) / 2);
        int navigationY = top + PAGE_TEXT_TOP + PAGE_TEXT_HEIGHT;
        int pageNumberY = navigationY + 6;

        if (firstPage > 0) {
            int x = left + 23;
            int y = navigationY;
            drawPageArrow(pose, x, y, false, isInside(mouseX, mouseY, x - 3, y - 4, 24, 18));
            clickRegions.add(new ClickRegion(x - 3, y - 4, 24, 18, () -> changeSpread(-1)));
        }
        if (spread < maxSpread && firstPage + 2 < pageCount) {
            int x = left + PAGE_TEXTURE_WIDTH + 4 + PAGE_TEXT_WIDTH - 18;
            int y = navigationY;
            drawPageArrow(pose, x, y, true, isInside(mouseX, mouseY, x - 3, y - 4, 24, 18));
            clickRegions.add(new ClickRegion(x - 3, y - 4, 24, 18, () -> changeSpread(1)));
        }
        if (view == View.DOCUMENT) {
            int x = left + PAGE_TEXTURE_WIDTH - 9;
            int y = top + PAGE_TEXTURE_HEIGHT - 11;
            boolean hovered = isInside(mouseX, mouseY, x - 2, y - 2, 21, 13);
            RenderSystem.setShaderTexture(0, ICONS);
            blit(pose, x, y, 48, hovered ? 152 : 139, 17, 9, 256, 256);
            clickRegions.add(new ClickRegion(x - 2, y - 2, 21, 13, this::goBack));
        }

        if (firstPage < pageCount) {
            drawCentred(pose, Component.literal((firstPage + 1) + " / " + pageCount),
                left + 23, pageNumberY, PAGE_TEXT_WIDTH, PAGE_NUMBER_COLOUR);
        }
        if (firstPage + 1 < pageCount) {
            drawCentred(pose, Component.literal((firstPage + 2) + " / " + pageCount),
                left + PAGE_TEXTURE_WIDTH + 4, pageNumberY, PAGE_TEXT_WIDTH, PAGE_NUMBER_COLOUR);
        }
    }

    private int currentFirstPage() {
        return (view == View.CONTENTS ? contentsSpread : documentSpread) * 2;
    }

    private int currentPageCount() {
        if (view == View.CONTENTS) {
            return Math.max(2, 2 + contentsPages.size());
        }
        return document == null ? 0 : Math.max(1, document.pages.size());
    }

    private void drawPageArrow(PoseStack pose, int x, int y, boolean forward, boolean hovered) {
        RenderSystem.setShaderTexture(0, ICONS);
        int u = forward ? 0 : 23;
        int v = hovered ? 152 : 139;
        blit(pose, x, y, u, v, 18, 10, 256, 256);
    }

    private void changeSpread(int amount) {
        if (view == View.CONTENTS) {
            contentsSpread = Mth.clamp(contentsSpread + amount, 0, maxContentsSpread());
        } else if (document != null) {
            documentSpread = Mth.clamp(documentSpread + amount, 0, document.maxSpread());
        }
        updateSearchVisibility();
    }

    private void goBack() {
        if (!history.isEmpty()) {
            PageState state = history.pop();
            currentEntry = state.entry;
            document = layoutDocument(state.entry);
            documentSpread = Mth.clamp(state.spread, 0, document.maxSpread());
        } else {
            returnToContents();
        }
    }

    private void returnToContents() {
        view = View.CONTENTS;
        currentEntry = null;
        document = null;
        documentSpread = 0;
        updateSearchVisibility();
    }

    private int maxContentsSpread() {
        return Math.max(0, (currentContentsPageCount() - 1) / 2);
    }

    private int currentContentsPageCount() {
        return Math.max(2, 2 + contentsPages.size());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (searchBox != null && searchBox.visible && searchBox.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            for (int index = clickRegions.size() - 1; index >= 0; index--) {
                ClickRegion region = clickRegions.get(index);
                if (region.contains(mouseX, mouseY)) {
                    region.action.run();
                    return true;
                }
            }
        }
        if (button == 1 && searchBox != null && searchBox.visible
            && isInside(mouseX, mouseY, searchX, searchY, 80, 13)) {
            searchBox.setValue("");
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            changeSpread(delta < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.visible && searchBox.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (keyCode == 263) {
            changeSpread(-1);
            return true;
        }
        if (keyCode == 262) {
            changeSpread(1);
            return true;
        }
        if (keyCode == 259 && view == View.DOCUMENT) {
            goBack();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox != null && searchBox.visible && searchBox.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawCentred(PoseStack pose, Component text, int x, int y, int availableWidth, int colour) {
        font.draw(pose, text, x + (availableWidth - font.width(text)) / 2.0F, y, colour);
    }

    private void drawScaledCentred(PoseStack pose, String text, int x, int y, int availableWidth, float scale,
        int colour) {
        float scaledWidth = font.width(text) * scale;
        pose.pushPose();
        pose.translate(x + (availableWidth - scaledWidth) / 2.0F, y, 0);
        pose.scale(scale, scale, 1.0F);
        font.draw(pose, text, 0, 0, colour);
        pose.popPose();
    }

    private static int parseInt(@Nullable String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private static final class ContentsPage {
        final List<ContentsLine> lines = new ArrayList<>();
    }

    private enum ContentsLineKind {
        CHAPTER,
        SUBHEADING,
        ENTRY,
        MESSAGE
    }

    private static final class ContentsLine {
        final ContentsLineKind kind;
        final Component component;
        final @Nullable GuideContent.Entry entry;
        final String groupKey;
        final int colour;
        final int height;
        int y;

        private ContentsLine(ContentsLineKind kind, Component component, @Nullable GuideContent.Entry entry,
            String groupKey, int colour, int height) {
            this.kind = kind;
            this.component = component;
            this.entry = entry;
            this.groupKey = groupKey;
            this.colour = colour;
            this.height = height;
        }

        static ContentsLine chapter(String key, String label, int colour) {
            return new ContentsLine(ContentsLineKind.CHAPTER, Component.literal(label), null, key, colour,
                CONTENT_CHAPTER_HEIGHT);
        }

        static ContentsLine subheading(String label) {
            return new ContentsLine(ContentsLineKind.SUBHEADING, Component.literal(label), null, "", 0,
                CONTENT_SUBHEADING_HEIGHT);
        }

        static ContentsLine entry(GuideContent.Entry entry) {
            return new ContentsLine(ContentsLineKind.ENTRY, Component.literal(entry.title()), entry, "", 0,
                CONTENT_ENTRY_HEIGHT);
        }

        static ContentsLine message(Component message) {
            return new ContentsLine(ContentsLineKind.MESSAGE, message, null, "", 0, CONTENT_ENTRY_HEIGHT);
        }
    }

    private static final class ChapterTab {
        final String key;
        final String label;
        final int colour;
        final int pageIndex;

        ChapterTab(String key, String label, int colour, int pageIndex) {
            this.key = key;
            this.label = label;
            this.colour = colour;
            this.pageIndex = pageIndex;
        }
    }

    private static final class ClickRegion {
        final int x;
        final int y;
        final int width;
        final int height;
        final Runnable action;

        ClickRegion(int x, int y, int width, int height, Runnable action) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.action = action;
        }

        boolean contains(double mouseX, double mouseY) {
            return isInside(mouseX, mouseY, x, y, width, height);
        }
    }

    private static final class PageState {
        final GuideContent.Entry entry;
        final int spread;

        PageState(GuideContent.Entry entry, int spread) {
            this.entry = entry;
            this.spread = spread;
        }
    }

    private static final class DocumentLayout {
        final List<RenderPage> pages;
        final List<DocumentChapter> chapters;

        DocumentLayout(List<RenderPage> pages, List<DocumentChapter> chapters) {
            this.pages = pages;
            this.chapters = chapters;
        }

        @Nullable
        RenderPage page(int index) {
            return index < 0 || index >= pages.size() ? null : pages.get(index);
        }

        int maxSpread() {
            return Math.max(0, (pages.size() - 1) / 2);
        }
    }

    private static final class DocumentChapter {
        final String title;
        final int colour;
        final int pageIndex;
        final int level;

        DocumentChapter(String title, int colour, int pageIndex, int level) {
            this.title = title;
            this.colour = colour;
            this.pageIndex = pageIndex;
            this.level = level;
        }
    }

    private static final class RenderPage {
        final List<RenderElement> elements = new ArrayList<>();
    }

    private enum ElementKind {
        TEXT,
        CHAPTER,
        CODE,
        LINK,
        IMAGE,
        RECIPE
    }

    private static final class RenderElement {
        final ElementKind kind;
        final int x;
        final int y;
        final int width;
        final int height;
        final int sourceWidth;
        final int sourceHeight;
        final int colour;
        final boolean chapterBar;
        final @Nullable FormattedCharSequence line;
        final @Nullable Component component;
        final @Nullable String target;
        final @Nullable String secondary;
        final @Nullable ItemStack stack;
        final @Nullable ResourceLocation texture;
        final @Nullable Recipe<?> recipe;

        private RenderElement(ElementKind kind, int x, int y, int width, int height, int sourceWidth,
            int sourceHeight, int colour, boolean chapterBar, @Nullable FormattedCharSequence line,
            @Nullable Component component, @Nullable String target, @Nullable String secondary,
            @Nullable ItemStack stack, @Nullable ResourceLocation texture, @Nullable Recipe<?> recipe) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
            this.colour = colour;
            this.chapterBar = chapterBar;
            this.line = line;
            this.component = component;
            this.target = target;
            this.secondary = secondary;
            this.stack = stack;
            this.texture = texture;
            this.recipe = recipe;
        }

        static RenderElement text(int y, FormattedCharSequence line, int width, @Nullable String target) {
            return new RenderElement(ElementKind.TEXT, 0, y, width, 10, 0, 0, 0, false,
                line, null, target, null, null, null, null);
        }

        static RenderElement chapter(int x, int y, FormattedCharSequence line, int colour,
            boolean chapterBar, int blockHeight) {
            return new RenderElement(ElementKind.CHAPTER, x, y, 0, blockHeight, 0, 0, colour, chapterBar,
                line, null, null, null, null, null, null);
        }

        static RenderElement code(int x, int y, FormattedCharSequence line) {
            return new RenderElement(ElementKind.CODE, x, y, PAGE_TEXT_WIDTH - 4, 10, 0, 0, 0, false,
                line, null, null, null, null, null, null);
        }

        static RenderElement link(int y, Component title, @Nullable String target, @Nullable String type,
            ItemStack stack) {
            return new RenderElement(ElementKind.LINK, 0, y, PAGE_TEXT_WIDTH, 19, 0, 0, 0, false,
                null, title, target, type, stack, null, null);
        }

        static RenderElement image(int x, int y, int width, int height, int sourceWidth, int sourceHeight,
            @Nullable ResourceLocation texture, ItemStack stack) {
            return new RenderElement(ElementKind.IMAGE, x, y, width, height, sourceWidth, sourceHeight, 0, false,
                null, null, null, null, stack, texture, null);
        }

        static RenderElement recipe(int y, Recipe<?> recipe, ItemStack focusedOutput) {
            int width = recipe instanceof AssemblyRecipeBasic ? ASSEMBLY_GRID.width
                : recipe instanceof AbstractCookingRecipe ? SMELTING_GRID.width : CRAFTING_GRID.width;
            return new RenderElement(ElementKind.RECIPE, (PAGE_TEXT_WIDTH - width) / 2, y,
                width, 60, 0, 0, 0, false, null, null, null, null,
                focusedOutput.isEmpty() ? null : focusedOutput.copy(), null, recipe);
        }

    }
}
