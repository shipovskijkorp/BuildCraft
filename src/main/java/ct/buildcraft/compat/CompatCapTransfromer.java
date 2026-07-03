package ct.buildcraft.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import ct.buildcraft.lib.misc.CapUtil;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;

public enum CompatCapTransfromer {
    INSTANCE;

    private final Map<Class<?>, BiFunction<Object, Direction, IFluidHandler>> fluidCapRegistry = new HashMap<>();

    public <T extends CapabilityProvider<?>> IFluidHandler transfromFluidCap(T provider, Direction face) {
        BiFunction<Object, Direction, IFluidHandler> function = fluidCapRegistry.get(provider.getClass());
        return function == null ? null : function.apply(provider, face);
    }

    public void registryFluidCapTransform(Class<?> clazz, BiFunction<Object, Direction, IFluidHandler> function) {
        fluidCapRegistry.put(clazz, function);
    }

    public <T extends CapabilityProvider<?>, E> LazyOptional<E> getCap(T provider, Capability<E> capability, Direction face) {
        LazyOptional<E> orginCap = provider.getCapability(capability, face);
        if (orginCap.isPresent()) {
            return orginCap;
        }
        if (capability == CapUtil.CAP_FLUIDS) {
            IFluidHandler transfromFluidCap = transfromFluidCap(provider, face);
            return transfromFluidCap == null ? LazyOptional.empty() : LazyOptional.of(() -> transfromFluidCap).cast();
        }
        return LazyOptional.empty();
    }
}
