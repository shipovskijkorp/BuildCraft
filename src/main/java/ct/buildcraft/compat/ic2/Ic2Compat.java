package ct.buildcraft.compat.ic2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;

import ct.buildcraft.api.core.BCLog;
import ct.buildcraft.compat.CompatCapTransfromer;
import ct.buildcraft.energy.BCEnergyFluids;
import ct.buildcraft.lib.fluid.BCFluid;
import net.minecraft.world.level.material.Fluid;

public class Ic2Compat {

    private static final ConcurrentHashMap<Class<?>, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static boolean capCompatFailed = false;
    private static boolean recipeCompatFailed = false;

    private static final String[] DEFAULT_FLUID_TILE_CLASSES = {
        "ic2.core.block.generator.tileentity.TileEntityGeoGenerator",
        "ic2.core.block.machine.tileentity.TileEntityOreWashing",
        "ic2.core.block.generator.tileentity.TileEntitySemifluidGenerator",
        "ic2.core.block.heatgenerator.tileentity.TileEntityFluidHeatGenerator",
        "ic2.core.block.kineticgenerator.tileentity.TileEntitySteamKineticGenerator",
        "ic2.core.block.kineticgenerator.tileentity.TileEntityStirlingKineticGenerator",
        "ic2.core.block.machine.tileentity.TileEntityBlastFurnace",
        "ic2.core.block.machine.tileentity.TileEntityCanner",
        "ic2.core.block.machine.tileentity.TileEntityCondenser",
        "ic2.core.block.machine.tileentity.TileEntityCropmatron",
        "ic2.core.block.machine.tileentity.TileEntityElectrolyzer",
        "ic2.core.block.machine.tileentity.TileEntityFermenter",
        "ic2.core.block.machine.tileentity.TileEntityFluidBottler",
        "ic2.core.block.machine.tileentity.TileEntityFluidDistributor",
        "ic2.core.block.machine.tileentity.TileEntityFluidRegulator",
        "ic2.core.block.machine.tileentity.TileEntityLiquidHeatExchanger",
        "ic2.core.block.machine.tileentity.TileEntityMatter",
        "ic2.core.block.machine.tileentity.TileEntityPump",
        "ic2.core.block.machine.tileentity.TileEntityReplicator",
        "ic2.core.block.machine.tileentity.TileEntitySolarDestiller",
        "ic2.core.block.machine.tileentity.TileEntitySteamGenerator",
        "ic2.core.block.machine.tileentity.TileEntitySteamRepressurizer",
        "ic2.core.block.machine.tileentity.TileEntityTank",
        "ic2.core.block.reactor.tileentity.TileEntityReactorFluidPort",
        "ic2.core.block.reactor.tileentity.TileEntityReactorChamberElectric",
        "ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric"
    };

    public static Object getFirstIc2FluidTankField(Object obj) {
        if (obj == null) {
            return null;
        }
        Class<?> clazz = obj.getClass();
        Field field = FIELD_CACHE.get(clazz);
        if (field == null) {
            try {
                field = findIc2FluidTankField(clazz);
                FIELD_CACHE.put(clazz, field);
            } catch (Throwable e) {
                return null;
            }
        }
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            BCLog.logger.warn("Failed to read IC2 fluid tank field", e);
            return null;
        }
    }

    private static Field findIc2FluidTankField(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                Class<?> type = field.getType();
                if (looksLikeIc2FluidsComponent(type)) {
                    field.setAccessible(true);
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchFieldError("Can't find IC2 fluid tank field in " + clazz.getName());
    }

    private static boolean looksLikeIc2FluidsComponent(Class<?> type) {
        String name = type.getName().toLowerCase();
        if (!name.contains("fluid") && !name.endsWith("fluids")) {
            return false;
        }
        return hasMethod(type, "getAllTanks")
            || hasMethod(type, "fillMb")
            || hasMethod(type, "drainMb")
            || hasMethod(type, "getTanks");
    }

    private static boolean hasMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static void registerDefaultHandle(String className) {
        try {
            Class<?> clazz = Class.forName(className, false, Ic2Compat.class.getClassLoader());
            Field field = findIc2FluidTankField(clazz);
            FIELD_CACHE.put(clazz, field);
            CompatCapTransfromer.INSTANCE.registryFluidCapTransform(clazz,
                (tile, d) -> new Ic2TankHandler(getFirstIc2FluidTankField(tile), d));
        } catch (ClassNotFoundException e) {
            // Different IC2Classic versions move or remove internals. Missing optional classes are fine.
        } catch (Throwable e) {
            BCLog.logger.debug("Skipping IC2 fluid compat for {}", className, e);
        }
    }

    public static void preInit() {
        if (capCompatFailed) {
            return;
        }
        try {
            for (String className : DEFAULT_FLUID_TILE_CLASSES) {
                registerDefaultHandle(className);
            }
        } catch (Throwable e) {
            BCLog.logger.error("Cannot make fluid tank compat with IC2 Classic", e);
            capCompatFailed = true;
        }
    }

    public static void init() {
        if (recipeCompatFailed) {
            return;
        }
        try {
            addSemiGenerator(BCEnergyFluids.oilResidue, 3000);
            addSemiGenerator(BCEnergyFluids.crudeOil, 16000);
            addSemiGenerator(BCEnergyFluids.oilDistilled, 10000);
            addSemiGenerator(BCEnergyFluids.oilHeavy, 10000);
            addSemiGenerator(BCEnergyFluids.oilDense, 10000);
            addSemiGenerator(BCEnergyFluids.fuelGaseous, 44992);
            addSemiGenerator(BCEnergyFluids.fuelDense, 128000);
            addSemiGenerator(BCEnergyFluids.fuelMixedLight, 128000);
            addSemiGenerator(BCEnergyFluids.fuelMixedHeavy, 128000);
            addSemiGenerator(BCEnergyFluids.fuelLight, 128000);

            addHeatGenerator(BCEnergyFluids.oilResidue, 6);
            addHeatGenerator(BCEnergyFluids.crudeOil, 32);
            addHeatGenerator(BCEnergyFluids.oilDistilled, 32);
            addHeatGenerator(BCEnergyFluids.oilHeavy, 32);
            addHeatGenerator(BCEnergyFluids.oilDense, 32);
            addHeatGenerator(BCEnergyFluids.fuelGaseous, 90);
            addHeatGenerator(BCEnergyFluids.fuelDense, 768);
            addHeatGenerator(BCEnergyFluids.fuelMixedLight, 768);
            addHeatGenerator(BCEnergyFluids.fuelMixedHeavy, 768);
            addHeatGenerator(BCEnergyFluids.fuelLight, 768);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            BCLog.logger.info("IC2 Classic recipe API was not found; skipping BuildCraft fuel recipe compat");
            recipeCompatFailed = true;
        } catch (Throwable e) {
            BCLog.logger.error("Cannot make recipe compat with IC2 Classic", e);
            recipeCompatFailed = true;
        }
    }

    private static void addSemiGenerator(BCFluid[] fluids, int power) throws ReflectiveOperationException {
        Object manager = getRecipesManager("semiFluidGenerator");
        float value = power / 1000f;
        addFluidRecipe(manager, fluids[0], 1, value);
        addFluidRecipe(manager, fluids[1], 1, value);
        addFluidRecipe(manager, fluids[2], 1, value);
    }

    private static void addHeatGenerator(BCFluid[] fluids, int power) throws ReflectiveOperationException {
        Object manager = getRecipesManager("fluidHeatGenerator");
        addFluidRecipe(manager, fluids[0], 10, power);
        addFluidRecipe(manager, fluids[1], 10, power);
        addFluidRecipe(manager, fluids[2], 10, power);
    }

    private static Object getRecipesManager(String fieldName) throws ReflectiveOperationException {
        Class<?> recipesClass = Class.forName("ic2.api.recipe.Recipes");
        Field field = recipesClass.getField(fieldName);
        return field.get(null);
    }

    private static void addFluidRecipe(Object manager, Fluid fluid, int amount, Number value) throws ReflectiveOperationException {
        if (manager == null || fluid == null) {
            return;
        }
        Method fallback = null;
        for (Method method : manager.getClass().getMethods()) {
            if (!method.getName().equals("addFluid") || method.getParameterCount() != 3) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            if (!types[0].isAssignableFrom(fluid.getClass()) && !types[0].isAssignableFrom(Fluid.class)) {
                continue;
            }
            if (!isInt(types[1])) {
                continue;
            }
            if (isFloat(types[2])) {
                method.invoke(manager, fluid, amount, value.floatValue());
                return;
            }
            if (isInt(types[2])) {
                method.invoke(manager, fluid, amount, value.intValue());
                return;
            }
            fallback = method;
        }
        if (fallback != null) {
            Object third = coerceNumber(value, fallback.getParameterTypes()[2]);
            fallback.invoke(manager, fluid, amount, third);
        }
    }

    private static boolean isInt(Class<?> type) {
        return type == int.class || type == Integer.class;
    }

    private static boolean isFloat(Class<?> type) {
        return type == float.class || type == Float.class || type == double.class || type == Double.class;
    }

    private static Object coerceNumber(Number value, Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return value.intValue();
        }
        if (type == long.class || type == Long.class) {
            return value.longValue();
        }
        if (type == double.class || type == Double.class) {
            return value.doubleValue();
        }
        return value.floatValue();
    }
}
