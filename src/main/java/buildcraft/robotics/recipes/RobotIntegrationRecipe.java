package buildcraft.robotics.recipes;

import com.google.common.collect.ImmutableList;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.recipes.IngredientStack;
import buildcraft.api.recipes.IntegrationRecipe;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.lib.recipe.IntegrationRecipeRegistry;
import buildcraft.robotics.BCRobotics;
import buildcraft.robotics.BCRoboticsBoards;
import buildcraft.robotics.BCRoboticsBoards.BoardEntry;
import buildcraft.robotics.BCRoboticsItems;
import buildcraft.robotics.item.ItemRobot;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class RobotIntegrationRecipe extends IntegrationRecipe {
    public static final ResourceLocation ID = new ResourceLocation(BCRobotics.MODID, "robot_board_integration");
    private static boolean registered;

    public RobotIntegrationRecipe() {
        super(ID);
    }

    public static void register() {
        if (registered || IntegrationRecipeRegistry.INSTANCE.getRecipe(ID) != null) {
            registered = true;
            return;
        }
        IntegrationRecipeRegistry.INSTANCE.addRecipe(new RobotIntegrationRecipe());
        registered = true;
    }

    @Override
    public ItemStack getOutput(ItemStack target, NonNullList<ItemStack> toIntegrate) {
        if (target.isEmpty() || !target.is(BCRoboticsItems.ROBOT.get())) {
            return ItemStack.EMPTY;
        }
        if (BCRoboticsBoards.getRobotBoard(target) != BCRoboticsBoards.EMPTY) {
            return ItemStack.EMPTY;
        }
        BoardEntry board = findBoard(toIntegrate);
        if (board == BCRoboticsBoards.EMPTY) {
            return ItemStack.EMPTY;
        }
        int energy = ItemRobot.getEnergy(target);
        if (energy <= 0) {
            energy = EntityRobotBase.SAFETY_ENERGY;
        }
        return ItemRobot.createRobotStack(board, energy);
    }

    @Override
    public ImmutableList<IngredientStack> getRequirements(ItemStack output) {
        return ImmutableList.of(new IngredientStack(Ingredient.of(BCRoboticsItems.REDSTONE_BOARD.get())));
    }

    @Override
    public long getRequiredMicroJoules(ItemStack output) {
        return 10_000L * MjAPI.MJ;
    }

    @Override
    public IngredientStack getCenterStack() {
        return new IngredientStack(Ingredient.of(BCRoboticsItems.ROBOT.get()));
    }

    private BoardEntry findBoard(NonNullList<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty() || !stack.is(BCRoboticsItems.REDSTONE_BOARD.get())) {
                continue;
            }
            BoardEntry board = BCRoboticsBoards.getBoard(stack);
            if (board != BCRoboticsBoards.EMPTY) {
                return board;
            }
        }
        return BCRoboticsBoards.EMPTY;
    }
}
