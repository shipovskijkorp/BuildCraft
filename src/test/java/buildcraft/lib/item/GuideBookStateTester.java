package buildcraft.lib.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GuideBookStateTester {
    @Test
    void stateBelongsToTheIndividualGuideStack() {
        ItemGuide guide = new ItemGuide(new Item.Properties().stacksTo(1));
        ItemStack first = new ItemStack(guide);
        ItemStack second = new ItemStack(guide);

        ItemGuide.writeGuideState(first, new ItemGuide.GuideState(
            false, true, "MODULE", true, new ResourceLocation("buildcraftbuilders", "block/quarry"), 3
        ));
        ItemGuide.writeGuideState(second, new ItemGuide.GuideState(
            true, false, "ALPHABETICAL", false, null, 5
        ));

        ItemGuide.GuideState firstState = ItemGuide.readGuideState(first);
        ItemGuide.GuideState secondState = ItemGuide.readGuideState(second);

        Assertions.assertFalse(firstState.showLore);
        Assertions.assertTrue(firstState.showHints);
        Assertions.assertEquals("MODULE", firstState.sortMode);
        Assertions.assertTrue(firstState.document);
        Assertions.assertEquals(new ResourceLocation("buildcraftbuilders", "block/quarry"), firstState.entry);
        Assertions.assertEquals(3, firstState.spread);

        Assertions.assertTrue(secondState.showLore);
        Assertions.assertFalse(secondState.showHints);
        Assertions.assertEquals("ALPHABETICAL", secondState.sortMode);
        Assertions.assertFalse(secondState.document);
        Assertions.assertNull(secondState.entry);
        Assertions.assertEquals(5, secondState.spread);
    }
}
