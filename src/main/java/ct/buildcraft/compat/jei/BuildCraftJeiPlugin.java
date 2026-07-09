package ct.buildcraft.compat.jei;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;

import ct.buildcraft.api.mj.MjAPI;
import ct.buildcraft.api.recipes.BuildcraftRecipeRegistry;
import ct.buildcraft.api.recipes.IRefineryRecipeManager;
import ct.buildcraft.api.recipes.IRefineryRecipeManager.IHeatExchangerRecipe;
import ct.buildcraft.api.recipes.IngredientStack;
import ct.buildcraft.api.robots.EntityRobotBase;
import ct.buildcraft.factory.BCFactoryItems;
import ct.buildcraft.lib.misc.ItemStackKey;
import ct.buildcraft.lib.recipe.AssemblyRecipeBasic;
import ct.buildcraft.lib.recipe.ChangingItemStack;
import ct.buildcraft.robotics.BCRoboticsBoards;
import ct.buildcraft.robotics.BCRoboticsBoards.BoardEntry;
import ct.buildcraft.robotics.BCRoboticsItems;
import ct.buildcraft.robotics.item.ItemRedstoneBoard;
import ct.buildcraft.robotics.item.ItemRobot;
import ct.buildcraft.silicon.BCSiliconItems;
import ct.buildcraft.silicon.BCSiliconRecipes;
import ct.buildcraft.silicon.item.ItemPluggableFacade;
import ct.buildcraft.silicon.recipe.FacadeAssemblyRecipes;
import ct.buildcraft.silicon.plug.FacadeBlockStateInfo;
import ct.buildcraft.silicon.plug.FacadeStateManager;
import ct.buildcraft.transport.BCTransportItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

@JeiPlugin
public class BuildCraftJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation("buildcraftlib", "jei_plugin");

    public static final RecipeType<AssemblyRecipeBasic> ASSEMBLY = RecipeType.create("buildcraftsilicon", "assembly", AssemblyRecipeBasic.class);
    public static final RecipeType<ProgrammingRecipeView> PROGRAMMING = RecipeType.create("buildcraftsilicon", "programming", ProgrammingRecipeView.class);
    public static final RecipeType<IntegrationRecipeView> INTEGRATION = RecipeType.create("buildcraftsilicon", "integration", IntegrationRecipeView.class);
    public static final RecipeType<IRefineryRecipeManager.IDistillationRecipe> DISTILLATION = RecipeType.create("buildcraftfactory", "distillation", IRefineryRecipeManager.IDistillationRecipe.class);
    public static final RecipeType<HeatExchangeRecipeView> HEAT_EXCHANGE = RecipeType.create("buildcraftfactory", "heat_exchange", HeatExchangeRecipeView.class);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(BCSiliconItems.PLUG_FACADE_ITEM.get(),
                (stack, context) -> getFacadeSubtype(stack));
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new AssemblyCategory(guiHelper),
                new ProgrammingCategory(guiHelper),
                new IntegrationCategory(guiHelper),
                new DistillationCategory(guiHelper),
                new HeatExchangeCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            List<AssemblyRecipeBasic> assemblyRecipes = new ArrayList<>(level.getRecipeManager().getAllRecipesFor(BCSiliconRecipes.ASSEMBLY_TYPE.get()));
            boolean hadFacadeRecipe = assemblyRecipes.removeIf(FacadeAssemblyRecipes.class::isInstance);
            if (hadFacadeRecipe) {
                assemblyRecipes.add(FacadeAssemblyJeiRecipe.create());
            }
            registration.addRecipes(ASSEMBLY, assemblyRecipes);
        }

        BCRoboticsBoards.init();
        List<ProgrammingRecipeView> programming = new ArrayList<>();
        List<IntegrationRecipeView> integration = new ArrayList<>();
        for (BoardEntry board : BCRoboticsBoards.robotEntries()) {
            programming.add(new ProgrammingRecipeView(board));
            integration.add(new IntegrationRecipeView(board));
        }
        registration.addRecipes(PROGRAMMING, programming);
        registration.addRecipes(INTEGRATION, integration);

        if (BuildcraftRecipeRegistry.refineryRecipes != null) {
            registration.addRecipes(DISTILLATION, new ArrayList<>(BuildcraftRecipeRegistry.refineryRecipes.getDistillationRegistry().getAllRecipes()));

            List<HeatExchangeRecipeView> heatExchange = new ArrayList<>();
            for (IRefineryRecipeManager.IHeatableRecipe recipe : BuildcraftRecipeRegistry.refineryRecipes.getHeatableRegistry().getAllRecipes()) {
                if (recipe.out() != null && !recipe.out().isEmpty()) {
                    heatExchange.add(new HeatExchangeRecipeView(recipe, true));
                }
            }
            for (IRefineryRecipeManager.ICoolableRecipe recipe : BuildcraftRecipeRegistry.refineryRecipes.getCoolableRegistry().getAllRecipes()) {
                if (recipe.out() != null && !recipe.out().isEmpty()) {
                    heatExchange.add(new HeatExchangeRecipeView(recipe, false));
                }
            }
            registration.addRecipes(HEAT_EXCHANGE, heatExchange);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(BCSiliconItems.ASSEMBLY_TABLE_ITEM.get()), ASSEMBLY);
        registration.addRecipeCatalyst(new ItemStack(BCSiliconItems.PROGRAMMING_TABLE_ITEM.get()), PROGRAMMING);
        registration.addRecipeCatalyst(new ItemStack(BCSiliconItems.INTERGRATION_TABLE_ITEM.get()), INTEGRATION);
        registration.addRecipeCatalyst(new ItemStack(BCFactoryItems.DISTILLER_BLOCK_ITEM.get()), DISTILLATION);
        registration.addRecipeCatalyst(new ItemStack(BCFactoryItems.HEAT_EXCHANGE_BLOCK_ITEM.get()), HEAT_EXCHANGE);
    }

    private static List<ItemStack> expandIngredient(IngredientStack ingredientStack) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : ingredientStack.ingredient.getItems()) {
            ItemStack copy = stack.copy();
            copy.setCount(Math.max(1, ingredientStack.count));
            stacks.add(copy);
        }
        return stacks;
    }

    private static List<ItemStack> expandIngredient(Ingredient ingredient, int count) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) {
            ItemStack copy = stack.copy();
            copy.setCount(Math.max(1, count));
            stacks.add(copy);
        }
        return stacks;
    }

    private static List<ItemStack> expandChangingStack(ChangingItemStack changing) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStackKey key : changing.getOptions()) {
            ItemStack copy = key.baseStack.copy();
            if (!copy.isEmpty()) {
                stacks.add(copy);
            }
        }
        return stacks;
    }

    private static String formatMj(long microJoules) {
        return MjAPI.formatMj(Math.max(0L, microJoules)) + " MJ";
    }

    public record ProgrammingRecipeView(BoardEntry board) {
        public ItemStack input() {
            return new ItemStack(BCRoboticsItems.REDSTONE_BOARD.get());
        }

        public ItemStack output() {
            return ItemRedstoneBoard.createStack(board);
        }

        public long requiredMicroJoules() {
            return board.energyCost() * MjAPI.MJ;
        }
    }

    public record IntegrationRecipeView(BoardEntry board) {
        public ItemStack robotInput() {
            return new ItemStack(BCRoboticsItems.ROBOT.get());
        }

        public ItemStack boardInput() {
            return ItemRedstoneBoard.createStack(board);
        }

        public ItemStack output() {
            return ItemRobot.createRobotStack(board, EntityRobotBase.SAFETY_ENERGY);
        }

        public long requiredMicroJoules() {
            return 50_000L * MjAPI.MJ;
        }
    }

    public record HeatExchangeRecipeView(IHeatExchangerRecipe recipe, boolean heating) {
    }

    private static List<FacadeBlockStateInfo> getVisibleFacadeInfos() {
        List<FacadeBlockStateInfo> infos = new ArrayList<>();
        for (FacadeBlockStateInfo info : FacadeStateManager.validFacadeStates.values()) {
            if (info.isVisible && !info.requiredStack.isEmpty()) {
                infos.add(info);
            }
        }
        infos.sort(Comparator
                .comparing((FacadeBlockStateInfo info) -> String.valueOf(ForgeRegistries.BLOCKS.getKey(info.state.getBlock())))
                .thenComparing(info -> info.state.toString())
                .thenComparing(info -> String.valueOf(ForgeRegistries.ITEMS.getKey(info.requiredStack.getItem()))));
        return infos;
    }

    private static ItemStack createFacadeBaseRequirementStack() {
        if (!BCTransportItems.PIPE_STRUCTURE.isPresent()) {
            return new ItemStack(Blocks.COBBLESTONE_WALL);
        }
        return new ItemStack(BCTransportItems.PIPE_STRUCTURE.get(), 3);
    }

    private static String getFacadeSubtype(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? IIngredientSubtypeInterpreter.NONE : tag.toString();
    }

    private static ItemStack getFocusedItemStack(IFocus<?> focus) {
        return focus.getTypedValue().getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
    }

    private static FacadeBlockStateInfo getFocusedFacadeInfo(IFocusGroup focuses) {
        return focuses.getFocuses(RecipeIngredientRole.OUTPUT)
                .map(BuildCraftJeiPlugin::getFocusedItemStack)
                .filter(stack -> stack.getItem() == BCSiliconItems.PLUG_FACADE_ITEM.get())
                .map(ItemPluggableFacade::getStates)
                .filter(instance -> instance.phasedStates.length > 0)
                .map(instance -> instance.phasedStates[0].stateInfo)
                .findFirst()
                .orElse(null);
    }

    private static List<FacadeBlockStateInfo> getFocusedFacadeInputInfos(IFocusGroup focuses) {
        List<ItemStack> focusedInputs = focuses.getFocuses(RecipeIngredientRole.INPUT)
                .map(BuildCraftJeiPlugin::getFocusedItemStack)
                .filter(stack -> !stack.isEmpty())
                .toList();
        if (focusedInputs.isEmpty()) {
            return List.of();
        }

        ItemStack baseRequirement = createFacadeBaseRequirementStack();
        baseRequirement.setCount(1);
        List<FacadeBlockStateInfo> infos = new ArrayList<>();
        for (FacadeBlockStateInfo info : getVisibleFacadeInfos()) {
            ItemStack required = info.requiredStack.copy();
            required.setCount(1);
            for (ItemStack focused : focusedInputs) {
                ItemStack focusedCopy = focused.copy();
                focusedCopy.setCount(1);
                if (ItemStack.isSameItemSameTags(baseRequirement, focusedCopy)) {
                    continue;
                }
                if (ItemStack.isSameItemSameTags(required, focusedCopy)) {
                    infos.add(info);
                    break;
                }
            }
        }
        return infos;
    }

    private static List<ItemStack> createFacadeRequirementStacks(List<FacadeBlockStateInfo> infos) {
        List<ItemStack> stacks = new ArrayList<>();
        for (FacadeBlockStateInfo info : infos) {
            stacks.add(info.requiredStack.copy());
        }
        return stacks;
    }

    private static List<ItemStack> createFacadeOutputStacks(List<FacadeBlockStateInfo> infos, boolean hollow) {
        List<ItemStack> stacks = new ArrayList<>();
        for (FacadeBlockStateInfo info : infos) {
            stacks.add(FacadeAssemblyRecipes.createFacadeStack(info, hollow));
        }
        return stacks;
    }

    private static class FacadeAssemblyJeiRecipe extends FacadeAssemblyRecipes {
        private static final ResourceLocation ID = new ResourceLocation("buildcraftsilicon", "jei/facades");

        private FacadeAssemblyJeiRecipe() {
            super(ID);
        }

        static FacadeAssemblyJeiRecipe create() {
            return new FacadeAssemblyJeiRecipe();
        }

        @Override
        public ChangingItemStack[] getRecipeInputs() {
            ChangingItemStack[] inputs = new ChangingItemStack[2];
            inputs[0] = new ChangingItemStack(createFacadeBaseRequirementStack());

            NonNullList<ItemStack> facadeInputs = NonNullList.create();
            for (FacadeBlockStateInfo info : getVisibleFacadeInfos()) {
                facadeInputs.add(info.requiredStack.copy());
            }
            if (facadeInputs.isEmpty()) {
                facadeInputs.add(ItemStack.EMPTY);
            }
            inputs[1] = new ChangingItemStack(facadeInputs);
            inputs[1].setTimeGap(500);
            return inputs;
        }

        @Override
        public ChangingItemStack getRecipeOutputs() {
            NonNullList<ItemStack> outputs = NonNullList.create();
            for (FacadeBlockStateInfo info : getVisibleFacadeInfos()) {
                outputs.add(FacadeAssemblyRecipes.createFacadeStack(info, false));
                outputs.add(FacadeAssemblyRecipes.createFacadeStack(info, true));
            }
            if (outputs.isEmpty()) {
                return super.getRecipeOutputs();
            }
            ChangingItemStack changing = new ChangingItemStack(outputs);
            changing.setTimeGap(500);
            return changing;
        }

        @Override
        public ItemStack getResultItem() {
            List<FacadeBlockStateInfo> infos = getVisibleFacadeInfos();
            if (infos.isEmpty()) {
                return super.getResultItem();
            }
            return FacadeAssemblyRecipes.createFacadeStack(infos.get(0), false);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return BCSiliconRecipes.FACADE_SERIALIZER.get();
        }
    }

    private static class AssemblyCategory implements IRecipeCategory<AssemblyRecipeBasic> {
        private static final ResourceLocation SLOT_TEXTURE = new ResourceLocation("buildcraftsilicon", "textures/gui/programming_table.png");
        private final IDrawable background;
        private final IDrawable icon;
        private final IDrawable slotBackground;

        AssemblyCategory(IGuiHelper guiHelper) {
            background = guiHelper.createBlankDrawable(150, 64);
            icon = guiHelper.createDrawableItemStack(new ItemStack(BCSiliconItems.ASSEMBLY_TABLE_ITEM.get()));
            slotBackground = guiHelper.createDrawable(SLOT_TEXTURE, 7, 35, 18, 18);
        }

        @Override
        public RecipeType<AssemblyRecipeBasic> getRecipeType() {
            return ASSEMBLY;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("block.buildcraftsilicon.assembly_table");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, AssemblyRecipeBasic recipe, IFocusGroup focuses) {
            if (recipe instanceof FacadeAssemblyRecipes facadeRecipe) {
                setFacadeRecipe(builder, facadeRecipe, focuses);
                return;
            }

            ItemStack result = recipe.getResultItem();
            Set<IngredientStack> inputs = recipe.getInputsFor(result);
            int inputCount = !inputs.isEmpty() ? inputs.size() : recipe.getIngredients().size();
            int index = 0;
            for (IngredientStack input : inputs) {
                int x = 4 + (index % 4) * 18;
                int y = inputCount <= 2 ? 22 : 4 + (index / 4) * 18;
                builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                        .setBackground(slotBackground, -1, -1)
                        .addItemStacks(expandIngredient(input));
                index++;
            }
            if (inputs.isEmpty()) {
                List<Ingredient> vanillaInputs = recipe.getIngredients();
                for (Ingredient input : vanillaInputs) {
                    int x = 4 + (index % 4) * 18;
                    int y = inputCount <= 2 ? 22 : 4 + (index / 4) * 18;
                    builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                            .setBackground(slotBackground, -1, -1)
                            .addItemStacks(expandIngredient(input, 1));
                    index++;
                }
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, 126, 22)
                    .setBackground(slotBackground, -1, -1)
                    .addItemStack(result);
        }

        private void setFacadeRecipe(IRecipeLayoutBuilder builder, FacadeAssemblyRecipes recipe, IFocusGroup focuses) {
            List<FacadeBlockStateInfo> infos = getVisibleFacadeInfos();

            FacadeBlockStateInfo focusedOutput = getFocusedFacadeInfo(focuses);
            if (focusedOutput != null) {
                infos = List.of(focusedOutput);
            } else {
                List<FacadeBlockStateInfo> focusedInputs = getFocusedFacadeInputInfos(focuses);
                if (!focusedInputs.isEmpty()) {
                    infos = focusedInputs;
                }
            }

            if (infos.isEmpty()) {
                ChangingItemStack[] inputs = recipe.getRecipeInputs();
                if (inputs.length > 0) {
                    builder.addSlot(RecipeIngredientRole.INPUT, 4, 22)
                            .setBackground(slotBackground, -1, -1)
                            .addItemStacks(expandChangingStack(inputs[0]));
                }
                if (inputs.length > 1) {
                    builder.addSlot(RecipeIngredientRole.INPUT, 22, 22)
                            .setBackground(slotBackground, -1, -1)
                            .addItemStacks(expandChangingStack(inputs[1]));
                }
                builder.addSlot(RecipeIngredientRole.OUTPUT, 126, 22)
                        .setBackground(slotBackground, -1, -1)
                        .addItemStacks(expandChangingStack(recipe.getRecipeOutputs()));
                return;
            }

            builder.addSlot(RecipeIngredientRole.INPUT, 4, 22)
                    .setBackground(slotBackground, -1, -1)
                    .addItemStack(createFacadeBaseRequirementStack());

            IRecipeSlotBuilder facadeInputSlot = builder.addSlot(RecipeIngredientRole.INPUT, 22, 22)
                    .setBackground(slotBackground, -1, -1)
                    .addItemStacks(createFacadeRequirementStacks(infos));

            IRecipeSlotBuilder solidOutputSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, 108, 22)
                    .setBackground(slotBackground, -1, -1)
                    .addItemStacks(createFacadeOutputStacks(infos, false));

            IRecipeSlotBuilder hollowOutputSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, 126, 22)
                    .setBackground(slotBackground, -1, -1)
                    .addItemStacks(createFacadeOutputStacks(infos, true));

            builder.createFocusLink(facadeInputSlot, solidOutputSlot, hollowOutputSlot);
        }

        @Override
        public void draw(AssemblyRecipeBasic recipe, IRecipeSlotsView recipeSlotsView, PoseStack stack, double mouseX, double mouseY) {
            Minecraft.getInstance().font.draw(stack, formatMj(recipe.getRequiredMicroJoulesFor(recipe.getResultItem())), 4, 52, 0xFF404040);
        }
    }

    private static class ProgrammingCategory implements IRecipeCategory<ProgrammingRecipeView> {
        private static final ResourceLocation TEXTURE = new ResourceLocation("buildcraftsilicon", "textures/gui/programming_table.png");
        private final IDrawable background;
        private final IDrawable icon;
        private final IDrawable slotBackground;
        private final IDrawable arrow;

        ProgrammingCategory(IGuiHelper guiHelper) {
            background = guiHelper.createBlankDrawable(150, 54);
            icon = guiHelper.createDrawableItemStack(new ItemStack(BCSiliconItems.PROGRAMMING_TABLE_ITEM.get()));
            slotBackground = guiHelper.createDrawable(TEXTURE, 7, 35, 18, 18);
            arrow = guiHelper.createDrawable(TEXTURE, 28, 40, 12, 10);
        }

        @Override
        public RecipeType<ProgrammingRecipeView> getRecipeType() {
            return PROGRAMMING;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("block.buildcraftsilicon.programming_table");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, ProgrammingRecipeView recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 21, 18)
                    .setBackground(slotBackground, -1, -1)
                    .addItemStack(recipe.input());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 109, 18)
                    .setBackground(slotBackground, -1, -1)
                    .addItemStack(recipe.output());
        }

        @Override
        public void draw(ProgrammingRecipeView recipe, IRecipeSlotsView recipeSlotsView, PoseStack stack, double mouseX, double mouseY) {
            arrow.draw(stack, 68, 22);
            Minecraft.getInstance().font.draw(stack, formatMj(recipe.requiredMicroJoules()), 4, 42, 0xFF404040);
        }
    }

    private static class IntegrationCategory implements IRecipeCategory<IntegrationRecipeView> {
        private static final ResourceLocation TEXTURE = new ResourceLocation("buildcraftsilicon", "textures/gui/integration_table.png");
        private static final ResourceLocation SLOT_TEXTURE = new ResourceLocation("buildcraftsilicon", "textures/gui/programming_table.png");
        private final IDrawable background;
        private final IDrawable icon;
        private final IDrawable slotBackground;
        private final IDrawable arrow;

        IntegrationCategory(IGuiHelper guiHelper) {
            background = guiHelper.createBlankDrawable(150, 54);
            icon = guiHelper.createDrawableItemStack(new ItemStack(BCSiliconItems.INTERGRATION_TABLE_ITEM.get()));
            slotBackground = guiHelper.createDrawable(SLOT_TEXTURE, 7, 35, 18, 18);
            arrow = guiHelper.createDrawable(SLOT_TEXTURE, 28, 40, 12, 10);
        }

        @Override
        public RecipeType<IntegrationRecipeView> getRecipeType() {
            return INTEGRATION;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("block.buildcraftsilicon.integration_table");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, IntegrationRecipeView recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 15, 18)
                    .setBackground(slotBackground, -1, -1)
                    .addItemStack(recipe.robotInput());
            builder.addSlot(RecipeIngredientRole.INPUT, 41, 18)
                    .setBackground(slotBackground, -1, -1)
                    .addItemStack(recipe.boardInput());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 109, 18)
                    .setBackground(slotBackground, -1, -1)
                    .addItemStack(recipe.output());
        }

        @Override
        public void draw(IntegrationRecipeView recipe, IRecipeSlotsView recipeSlotsView, PoseStack stack, double mouseX, double mouseY) {
            arrow.draw(stack, 68, 22);
            Minecraft.getInstance().font.draw(stack, formatMj(recipe.requiredMicroJoules()), 4, 42, 0xFF404040);
        }
    }

    private static class DistillationCategory implements IRecipeCategory<IRefineryRecipeManager.IDistillationRecipe> {
        private static final ResourceLocation SLOT_TEXTURE = new ResourceLocation("buildcraftsilicon", "textures/gui/programming_table.png");
        private final IDrawable background;
        private final IDrawable icon;
        private final IDrawable slotBackground;
        private final IDrawable arrow;

        DistillationCategory(IGuiHelper guiHelper) {
            background = guiHelper.createBlankDrawable(150, 58);
            icon = guiHelper.createDrawableItemStack(new ItemStack(BCFactoryItems.DISTILLER_BLOCK_ITEM.get()));
            slotBackground = guiHelper.createDrawable(SLOT_TEXTURE, 7, 35, 18, 18);
            arrow = guiHelper.createDrawable(SLOT_TEXTURE, 28, 40, 12, 10);
        }

        @Override
        public RecipeType<IRefineryRecipeManager.IDistillationRecipe> getRecipeType() {
            return DISTILLATION;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("block.buildcraftfactory.distiller");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, IRefineryRecipeManager.IDistillationRecipe recipe, IFocusGroup focuses) {
            FluidStack in = recipe.in().copy();
            FluidStack outGas = recipe.outGas().copy();
            FluidStack outLiquid = recipe.outLiquid().copy();

            builder.addSlot(RecipeIngredientRole.INPUT, 15, 18)
                    .setBackground(slotBackground, -1, -1)
                    .setFluidRenderer(Math.max(1, in.getAmount()), false, 16, 16)
                    .addIngredient(ForgeTypes.FLUID_STACK, in);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 109, 8)
                    .setBackground(slotBackground, -1, -1)
                    .setFluidRenderer(Math.max(1, outGas.getAmount()), false, 16, 16)
                    .addIngredient(ForgeTypes.FLUID_STACK, outGas);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 109, 30)
                    .setBackground(slotBackground, -1, -1)
                    .setFluidRenderer(Math.max(1, outLiquid.getAmount()), false, 16, 16)
                    .addIngredient(ForgeTypes.FLUID_STACK, outLiquid);
        }

        @Override
        public void draw(IRefineryRecipeManager.IDistillationRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack stack, double mouseX, double mouseY) {
            arrow.draw(stack, 68, 22);
            Minecraft.getInstance().font.draw(stack, formatMj(recipe.powerRequired()), 4, 48, 0xFF404040);
        }
    }

    private static class HeatExchangeCategory implements IRecipeCategory<HeatExchangeRecipeView> {
        private final IDrawable background;
        private final IDrawable icon;

        HeatExchangeCategory(IGuiHelper guiHelper) {
            background = guiHelper.createBlankDrawable(150, 54);
            icon = guiHelper.createDrawableItemStack(new ItemStack(BCFactoryItems.HEAT_EXCHANGE_BLOCK_ITEM.get()));
        }

        @Override
        public RecipeType<HeatExchangeRecipeView> getRecipeType() {
            return HEAT_EXCHANGE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("block.buildcraftfactory.heat_exchange");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, HeatExchangeRecipeView view, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 18, 18).addIngredient(ForgeTypes.FLUID_STACK, view.recipe().in().copy());
            FluidStack out = view.recipe().out();
            if (out != null && !out.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 108, 18).addIngredient(ForgeTypes.FLUID_STACK, out.copy());
            }
        }

        @Override
        public void draw(HeatExchangeRecipeView recipe, IRecipeSlotsView recipeSlotsView, PoseStack stack, double mouseX, double mouseY) {
            String mode = recipe.heating() ? "Heat " : "Cool ";
            String text = mode + recipe.recipe().heatFrom() + " -> " + recipe.recipe().heatTo();
            Minecraft.getInstance().font.draw(stack, text, 4, 42, 0xFF404040);
        }
    }
}
