package ct.buildcraft.compat.ic2;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class Ic2TankHandler implements IFluidHandler {

    protected final Object tankComponent;
    protected final List<Object> tanks;
    private final Direction face;

    public Ic2TankHandler(Object tankComponent, Direction face) {
        this.tankComponent = tankComponent;
        this.tanks = tankComponent == null ? Collections.emptyList() : getAllTanks(tankComponent);
        this.face = face;
    }

    @Override
    public int getTanks() {
        return tanks.size();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tankid) {
        if (tankid < 0 || tankid >= tanks.size()) {
            return FluidStack.EMPTY;
        }
        Object ic2FluidStack = invokeNoArgs(tanks.get(tankid), "getFluidStack");
        return toForgeFluidStack(ic2FluidStack);
    }

    @Override
    public int getTankCapacity(int tankid) {
        if (tankid < 0 || tankid >= tanks.size()) {
            return 0;
        }
        Object result = invokeNoArgs(tanks.get(tankid), "getCapacity");
        return result instanceof Number ? ((Number) result).intValue() : 0;
    }

    @Override
    public boolean isFluidValid(int tankid, @NotNull FluidStack stack) {
        if (tankid < 0 || tankid >= tanks.size() || stack.isEmpty()) {
            return false;
        }
        Object ic2FluidStack = createIc2FluidStack(stack);
        if (ic2FluidStack == null) {
            return false;
        }
        Object result = invoke(tanks.get(tankid), "fillMb", ic2FluidStack, true);
        return result instanceof Number && ((Number) result).intValue() > 0;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (tankComponent == null || resource.isEmpty()) {
            return 0;
        }
        Object ic2FluidStack = createIc2FluidStack(resource);
        if (ic2FluidStack == null) {
            return 0;
        }
        Object result = invoke(tankComponent, "fillMb", face, ic2FluidStack, action.simulate());
        return result instanceof Number ? ((Number) result).intValue() : 0;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (tankComponent == null || resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        Object ic2FluidStack = createIc2FluidStack(resource);
        if (ic2FluidStack == null) {
            return FluidStack.EMPTY;
        }
        Object result = invoke(tankComponent, "drainMb", face, ic2FluidStack, action.simulate());
        if (result instanceof Number) {
            int amount = ((Number) result).intValue();
            return amount > 0 ? new FluidStack(resource.getFluid(), amount) : FluidStack.EMPTY;
        }
        return toForgeFluidStack(result);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (tankComponent == null || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        Object result = invoke(tankComponent, "drainMb", face, maxDrain, action.simulate());
        return toForgeFluidStack(result);
    }

    private static List<Object> getAllTanks(Object tankComponent) {
        Object result = invokeNoArgs(tankComponent, "getAllTanks");
        if (result instanceof Collection<?>) {
            return new ArrayList<>((Collection<?>) result);
        }
        if (result instanceof Object[]) {
            Object[] array = (Object[]) result;
            List<Object> list = new ArrayList<>(array.length);
            Collections.addAll(list, array);
            return list;
        }
        return Collections.emptyList();
    }

    private static Object createIc2FluidStack(FluidStack stack) {
        try {
            Class<?> stackClass = findIc2FluidStackClass();
            for (Method method : stackClass.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) || !method.getName().equals("create") || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if (types[0].isAssignableFrom(stack.getFluid().getClass()) || types[0].isAssignableFrom(Fluid.class)) {
                    if (types[1] == int.class || types[1] == Integer.class) {
                        return method.invoke(null, stack.getFluid(), stack.getAmount());
                    }
                    if (types[1] == long.class || types[1] == Long.class) {
                        return method.invoke(null, stack.getFluid(), (long) stack.getAmount());
                    }
                }
            }
        } catch (Throwable ignored) {
            // Optional compatibility: if IC2 changes internals, expose no converted fluid capability instead of crashing.
        }
        return null;
    }

    private static Class<?> findIc2FluidStackClass() throws ClassNotFoundException {
        try {
            return Class.forName("ic2.core.fluid.Ic2FluidStack");
        } catch (ClassNotFoundException e) {
            return Class.forName("ic2.core.block.comp.Fluids$Ic2FluidStack");
        }
    }

    private static FluidStack toForgeFluidStack(Object ic2FluidStack) {
        if (ic2FluidStack == null || isEmpty(ic2FluidStack)) {
            return FluidStack.EMPTY;
        }
        Object fluidObject = invokeNoArgs(ic2FluidStack, "getFluid");
        Object amountObject = invokeNoArgs(ic2FluidStack, "getAmountMb");
        if (!(fluidObject instanceof Fluid)) {
            return FluidStack.EMPTY;
        }
        int amount = amountObject instanceof Number ? ((Number) amountObject).intValue() : 0;
        return amount > 0 ? new FluidStack((Fluid) fluidObject, amount) : FluidStack.EMPTY;
    }

    private static boolean isEmpty(Object stack) {
        Object result = invokeNoArgs(stack, "isEmpty");
        return result instanceof Boolean && (Boolean) result;
    }

    private static Object invokeNoArgs(Object target, String name) {
        return invoke(target, name);
    }

    private static Object invoke(Object target, String name, Object... args) {
        if (target == null) {
            return null;
        }
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == args.length && canAccept(method.getParameterTypes(), args)) {
                    try {
                        method.setAccessible(true);
                        return method.invoke(target, args);
                    } catch (Throwable ignored) {
                        return null;
                    }
                }
            }
            current = current.getSuperclass();
        }
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == args.length && canAccept(method.getParameterTypes(), args)) {
                try {
                    return method.invoke(target, args);
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static boolean canAccept(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            if (!canAccept(types[i], args[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean canAccept(Class<?> type, Object arg) {
        if (arg == null) {
            return !type.isPrimitive();
        }
        if (type.isPrimitive()) {
            if (type == boolean.class) {
                return arg instanceof Boolean;
            }
            if (type == int.class) {
                return arg instanceof Integer;
            }
            if (type == long.class) {
                return arg instanceof Long || arg instanceof Integer;
            }
            if (type == float.class) {
                return arg instanceof Float || arg instanceof Number;
            }
            if (type == double.class) {
                return arg instanceof Double || arg instanceof Number;
            }
            return false;
        }
        return type.isAssignableFrom(arg.getClass());
    }
}
