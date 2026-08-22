package buildcraft.lib.internal.mj;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import buildcraft.api.v2.energy.MjPortDescriptor;
import buildcraft.api.v2.energy.MjPortRole;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public class MjBridgeMetadataTester {
    @Test
    void feDescriptorDoesNotProbeTransfers() throws Exception {
        IEnergyStorage fe = new ExplodingEnergyStorage();
        MjPortDescriptor descriptor = invokeDescriptor(
            "FeEndpoint",
            new Class<?>[] {IEnergyStorage.class},
            fe
        );

        Assertions.assertTrue(descriptor.has(MjPortRole.CONSUMER));
        Assertions.assertTrue(descriptor.has(MjPortRole.PROVIDER));
        Assertions.assertEquals(Long.MAX_VALUE, descriptor.maxInsertPerTick().microMj());
        Assertions.assertEquals(Long.MAX_VALUE, descriptor.maxExtractPerTick().microMj());
    }

    @Test
    void legacyDescriptorDoesNotProbeTransfers() throws Exception {
        ExplodingMjEndpoint endpoint = new ExplodingMjEndpoint();
        MjPortDescriptor descriptor = invokeDescriptor(
            "Endpoint",
            new Class<?>[] {
                IMjConnector.class,
                IMjReceiver.class,
                IMjRedstoneReceiver.class,
                IMjReadable.class,
                IMjPassiveProvider.class
            },
            endpoint,
            endpoint,
            null,
            null,
            endpoint
        );

        Assertions.assertTrue(descriptor.has(MjPortRole.CONNECTOR));
        Assertions.assertTrue(descriptor.has(MjPortRole.CONSUMER));
        Assertions.assertTrue(descriptor.has(MjPortRole.PASSIVE_PROVIDER));
        Assertions.assertEquals(Long.MAX_VALUE, descriptor.maxInsertPerTick().microMj());
        Assertions.assertEquals(Long.MAX_VALUE, descriptor.maxExtractPerTick().microMj());
    }

    @Test
    void writeOnlyFeViewDoesNotUseCurrentDemandAsCapacity() {
        MjReceiverEnergyStorage storage = new MjReceiverEnergyStorage(new ExplodingMjEndpoint());
        Assertions.assertEquals(Integer.MAX_VALUE, storage.getMaxEnergyStored());
    }

    private static MjPortDescriptor invokeDescriptor(String nestedClass, Class<?>[] parameterTypes, Object... args)
        throws Exception {
        Class<?> endpointClass = Class.forName(MjApi2PlatformBridge.class.getName() + "$" + nestedClass);
        Constructor<?> constructor = endpointClass.getDeclaredConstructor(parameterTypes);
        constructor.setAccessible(true);
        Object endpoint = constructor.newInstance(args);
        Method descriptor = endpointClass.getDeclaredMethod("descriptor");
        descriptor.setAccessible(true);
        return (MjPortDescriptor) descriptor.invoke(endpoint);
    }

    private static final class ExplodingEnergyStorage implements IEnergyStorage {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            throw new AssertionError("descriptor must not simulate FE insertion");
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            throw new AssertionError("descriptor must not simulate FE extraction");
        }
        @Override public int getEnergyStored() {
            throw new AssertionError("descriptor must not query live FE storage");
        }
        @Override public int getMaxEnergyStored() {
            throw new AssertionError("descriptor must not query live FE capacity");
        }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
    }

    private static final class ExplodingMjEndpoint implements IMjReceiver, IMjPassiveProvider {
        @Override public boolean canConnect(IMjConnector other) { return true; }
        @Override public boolean canReceive() {
            throw new AssertionError("descriptor must not query dynamic MJ receive state");
        }
        @Override public long getPowerRequested() {
            throw new AssertionError("metadata must not query current MJ demand");
        }
        @Override public long receivePower(long microJoules, FluidAction action) {
            throw new AssertionError("metadata must not simulate MJ insertion");
        }
        @Override public long extractPower(long min, long max, boolean doExtract) {
            throw new AssertionError("metadata must not simulate MJ extraction");
        }
    }
}
