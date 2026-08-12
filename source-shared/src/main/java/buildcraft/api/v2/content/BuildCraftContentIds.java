package buildcraft.api.v2.content;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Stable ids for built-in content intended to be reused as addon archetypes/profiles. */
public final class BuildCraftContentIds {
    public static final class Machines {
        public static final ResourceLocation QUARRY = id("buildcraftbuilders:quarry");
        public static final ResourceLocation DISTILLER = id("buildcraftfactory:distiller");
        public static final ResourceLocation MINING_WELL = id("buildcraftfactory:mining_well");
        public static final ResourceLocation PUMP = id("buildcraftfactory:pump");

        private Machines() {}
    }

    public static final class Engines {
        public static final ResourceLocation REDSTONE = id("buildcraftcore:engine_redstone");
        public static final ResourceLocation STONE = id("buildcraftenergy:engine_stone");
        public static final ResourceLocation IRON = id("buildcraftenergy:engine_iron");
        public static final ResourceLocation CREATIVE = id("buildcraftcore:engine_creative");
        public static final ResourceLocation FE = id("buildcraftenergy:engine_fe");
        /** MJ -> external-energy converter implemented with the engine chassis. */
        public static final ResourceLocation MJ_DYNAMO = id("buildcraftenergy:mj_dynamo");

        private Engines() {}
    }

    public static final class EngineStages {
        public static final ResourceLocation BLUE = id("buildcraft:engine_stage/blue");
        public static final ResourceLocation GREEN = id("buildcraft:engine_stage/green");
        public static final ResourceLocation YELLOW = id("buildcraft:engine_stage/yellow");
        public static final ResourceLocation RED = id("buildcraft:engine_stage/red");
        public static final ResourceLocation OVERHEAT = id("buildcraft:engine_stage/overheat");
        public static final ResourceLocation BLACK = id("buildcraft:engine_stage/black");

        private EngineStages() {}
    }

    public static final class MachineComponents {
        public static final ResourceLocation ENERGY = id("buildcraft:energy");
        public static final ResourceLocation AREA = id("buildcraft:area");
        public static final ResourceLocation MINING = id("buildcraft:mining");
        public static final ResourceLocation PUMPING = id("buildcraft:pumping");
        public static final ResourceLocation DISTILLATION = id("buildcraft:distillation");
        public static final ResourceLocation INVENTORY_OUTPUT = id("buildcraft:inventory_output");
        public static final ResourceLocation FLUID_INPUT = id("buildcraft:fluid_input");
        public static final ResourceLocation FLUID_OUTPUT = id("buildcraft:fluid_output");
        public static final ResourceLocation CHUNK_LOADING = id("buildcraft:chunk_loading");

        private MachineComponents() {}
    }

    public static final class LaserTables {
        public static final ResourceLocation ASSEMBLY = id("buildcraftsilicon:assembly_table");
        public static final ResourceLocation ADVANCED_CRAFTING = id("buildcraftsilicon:advanced_crafting_table");
        public static final ResourceLocation INTEGRATION = id("buildcraftsilicon:integration_table");
        public static final ResourceLocation CHARGING = id("buildcraftsilicon:charging_table");
        public static final ResourceLocation PROGRAMMING = id("buildcraftsilicon:programming_table");

        private LaserTables() {}
    }

    public static final class Pipes {
        public static final ResourceLocation STRUCTURE = id("buildcrafttransport:structure");

        public static final ResourceLocation WOOD_ITEM = id("buildcrafttransport:wood_item");
        public static final ResourceLocation WOOD_FLUID = id("buildcrafttransport:wood_fluid");
        public static final ResourceLocation WOOD_POWER = id("buildcrafttransport:wood_power");
        public static final ResourceLocation WOOD_FE = id("buildcrafttransport:wood_fe");

        public static final ResourceLocation COBBLESTONE_ITEM = id("buildcrafttransport:cobblestone_item");
        public static final ResourceLocation COBBLESTONE_FLUID = id("buildcrafttransport:cobblestone_fluid");
        public static final ResourceLocation COBBLESTONE_POWER = id("buildcrafttransport:cobblestone_power");
        public static final ResourceLocation COBBLESTONE_FE = id("buildcrafttransport:cobblestone_fe");

        public static final ResourceLocation STONE_ITEM = id("buildcrafttransport:stone_item");
        public static final ResourceLocation STONE_FLUID = id("buildcrafttransport:stone_fluid");
        public static final ResourceLocation STONE_POWER = id("buildcrafttransport:stone_power");
        public static final ResourceLocation STONE_FE = id("buildcrafttransport:stone_fe");

        public static final ResourceLocation QUARTZ_ITEM = id("buildcrafttransport:quartz_item");
        public static final ResourceLocation QUARTZ_FLUID = id("buildcrafttransport:quartz_fluid");
        public static final ResourceLocation QUARTZ_POWER = id("buildcrafttransport:quartz_power");
        public static final ResourceLocation QUARTZ_FE = id("buildcrafttransport:quartz_fe");

        public static final ResourceLocation GOLD_ITEM = id("buildcrafttransport:gold_item");
        public static final ResourceLocation GOLD_FLUID = id("buildcrafttransport:gold_fluid");
        public static final ResourceLocation GOLD_POWER = id("buildcrafttransport:gold_power");
        public static final ResourceLocation GOLD_FE = id("buildcrafttransport:gold_fe");

        public static final ResourceLocation SANDSTONE_ITEM = id("buildcrafttransport:sandstone_item");
        public static final ResourceLocation SANDSTONE_FLUID = id("buildcrafttransport:sandstone_fluid");
        public static final ResourceLocation SANDSTONE_POWER = id("buildcrafttransport:sandstone_power");
        public static final ResourceLocation SANDSTONE_FE = id("buildcrafttransport:sandstone_fe");

        public static final ResourceLocation IRON_ITEM = id("buildcrafttransport:iron_item");
        public static final ResourceLocation IRON_FLUID = id("buildcrafttransport:iron_fluid");
        public static final ResourceLocation IRON_POWER = id("buildcrafttransport:iron_power");
        public static final ResourceLocation IRON_FE = id("buildcrafttransport:iron_fe");

        public static final ResourceLocation DIAMOND_ITEM = id("buildcrafttransport:diamond_item");
        public static final ResourceLocation DIAMOND_FLUID = id("buildcrafttransport:diamond_fluid");
        public static final ResourceLocation DIAMOND_POWER = id("buildcrafttransport:diamond_power");
        public static final ResourceLocation DIAMOND_FE = id("buildcrafttransport:diamond_fe");

        public static final ResourceLocation DIAMOND_WOOD_ITEM = id("buildcrafttransport:diamond_wood_item");
        public static final ResourceLocation DIAMOND_WOOD_FLUID = id("buildcrafttransport:diamond_wood_fluid");
        public static final ResourceLocation DIAMOND_WOOD_POWER = id("buildcrafttransport:diamond_wood_power");
        public static final ResourceLocation DIAMOND_WOOD_FE = id("buildcrafttransport:diamond_wood_fe");

        public static final ResourceLocation CLAY_ITEM = id("buildcrafttransport:clay_item");
        public static final ResourceLocation CLAY_FLUID = id("buildcrafttransport:clay_fluid");
        public static final ResourceLocation VOID_ITEM = id("buildcrafttransport:void_item");
        public static final ResourceLocation VOID_FLUID = id("buildcrafttransport:void_fluid");
        public static final ResourceLocation OBSIDIAN_ITEM = id("buildcrafttransport:obsidian_item");
        public static final ResourceLocation LAPIS_ITEM = id("buildcrafttransport:lapis_item");
        public static final ResourceLocation DAIZULI_ITEM = id("buildcrafttransport:daizuli_item");
        public static final ResourceLocation EMZULI_ITEM = id("buildcrafttransport:emzuli_item");
        public static final ResourceLocation STRIPES_ITEM = id("buildcrafttransport:stripes_item");

        private Pipes() {}
    }

    public static final class Worldgen {
        /** Standard BuildCraft oil-deposit generator profile. */
        public static final ResourceLocation STANDARD_OIL = id("buildcraftenergy:oil");

        private Worldgen() {}
    }

    private BuildCraftContentIds() {
    }

    private static ResourceLocation id(String value) {
        return Objects.requireNonNull(ResourceLocation.tryParse(value));
    }
}
