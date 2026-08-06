package buildcraft.transport.pipe;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;

public class PipeEventBusTester {
    @Test
    void registeringAndUnregisteringAHandlerChangesDispatch() {
        PipeEventBus bus = new PipeEventBus();
        Assertions.assertEquals(0, fire(bus).targetSpeed, 0.00001);

        bus.registerHandler(this);
        Assertions.assertEquals(1, fire(bus).targetSpeed, 0.00001);

        bus.unregisterHandler(this);
        Assertions.assertEquals(0, fire(bus).targetSpeed, 0.00001);
    }

    @PipeEventHandler
    public void modifySpeed(PipeEventItem.ModifySpeed event) {
        event.targetSpeed = 1;
    }

    @Test
    void inheritedHandlersDispatchToOverrides() {
        PipeEventBus baseBus = new PipeEventBus();
        baseBus.registerHandler(new Base());
        Assertions.assertEquals(2, fire(baseBus).targetSpeed, 0.00001);

        PipeEventBus subBus = new PipeEventBus();
        subBus.registerHandler(new Sub());
        Assertions.assertEquals(3, fire(subBus).targetSpeed, 0.00001);
    }

    private static PipeEventItem.ModifySpeed fire(PipeEventBus bus) {
        PipeEventItem.ModifySpeed event = new PipeEventItem.ModifySpeed(null, null, null, 1);
        bus.fireEvent(event);
        return event;
    }

    public static class Base {
        @PipeEventHandler
        public void modifySpeed(PipeEventItem.ModifySpeed event) {
            event.targetSpeed = 2;
        }
    }

    public static class Sub extends Base {
        @Override
        public void modifySpeed(PipeEventItem.ModifySpeed event) {
            event.targetSpeed = 3;
        }
    }
}
