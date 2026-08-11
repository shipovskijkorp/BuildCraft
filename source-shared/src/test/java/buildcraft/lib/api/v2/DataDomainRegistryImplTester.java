package buildcraft.lib.api.v2;

import buildcraft.api.v2.crops.CropAdapter;
import buildcraft.api.v2.template.TemplateHandler;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataDomainRegistryImplTester {
    @Test
    public void cropAndTemplateRegistrationsArePriorityOrderedAndImmutable() {
        CropServiceImpl crops = new CropServiceImpl();
        CropAdapter noOp = new NoOpCrop();
        crops.register(id("crop_low"), 0, noOp);
        crops.register(id("crop_high"), 10, noOp);
        assertEquals(id("crop_high"), crops.adapters().get(0).id());
        assertThrows(UnsupportedOperationException.class, () -> crops.adapters().clear());
        assertThrows(IllegalStateException.class, () -> crops.register(id("crop_low"), 20, noOp));

        TemplateServiceImpl templates = new TemplateServiceImpl();
        TemplateHandler handler = (level, pos, actor, stack) -> false;
        templates.register(id("template_b"), 5, handler);
        templates.register(id("template_a"), 5, handler);
        assertEquals(id("template_a"), templates.handlers().get(0).id());
    }

    // Facade rules operate on vanilla Block/BlockState/ItemStack registries and are
    // therefore verified by launched GameTests rather than an unbootstrapped JUnit JVM.

    private static final class NoOpCrop implements CropAdapter {
        @Override public boolean isSeed(ItemStack stack) { return false; }
        @Override public boolean canSustainPlant(Level level, ItemStack seed, BlockPos pos) { return false; }
        @Override public boolean plant(Level level, Player actor, ItemStack seed, BlockPos pos) { return false; }
        @Override public boolean isMature(BlockGetter level, BlockState state, BlockPos pos) { return false; }
        @Override public boolean harvest(Level level, BlockPos pos, NonNullList<ItemStack> drops, Player actor) { return false; }
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("test:" + path));
    }
}
