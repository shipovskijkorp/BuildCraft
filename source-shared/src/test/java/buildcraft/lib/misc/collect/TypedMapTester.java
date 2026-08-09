package buildcraft.lib.misc.collect;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TypedMapTester {
    interface RandomValue {
        int getRandom();
    }

    interface NamedValue {
        String getName();
    }

    static final class Independent {
    }

    enum RandomEntry implements RandomValue {
        A, B;

        @Override
        public int getRandom() {
            return ordinal();
        }
    }

    enum BothEntry implements RandomValue, NamedValue {
        A, B;

        @Override
        public int getRandom() {
            return 15 + ordinal();
        }

        @Override
        public String getName() {
            return name();
        }
    }

    @Test
    void directMapIndexesOnlyConcreteClasses() {
        TypedMap<Object> map = new TypedMapDirect<>();
        Independent independent = new Independent();
        map.put(RandomEntry.A);
        map.put(BothEntry.A);
        map.put(independent);

        Assertions.assertNull(map.get(RandomValue.class));
        Assertions.assertNull(map.get(NamedValue.class));
        Assertions.assertSame(independent, map.get(Independent.class));
        Assertions.assertEquals(RandomEntry.A, map.get(RandomEntry.class));
        Assertions.assertEquals(BothEntry.A, map.get(BothEntry.class));

        map.put(RandomEntry.B);
        Assertions.assertEquals(RandomEntry.B, map.get(RandomEntry.class));
        map.remove(RandomEntry.B);
        Assertions.assertNull(map.get(RandomEntry.class));
    }

    @Test
    void hierarchyMapIndexesImplementedInterfaces() {
        TypedMap<Object> map = new TypedMapHierarchy<>();
        Independent independent = new Independent();
        map.put(RandomEntry.A);
        map.put(BothEntry.A);
        map.put(independent);

        Assertions.assertNotNull(map.get(RandomValue.class));
        Assertions.assertEquals(BothEntry.A, map.get(NamedValue.class));
        Assertions.assertSame(independent, map.get(Independent.class));
        Assertions.assertEquals(RandomEntry.A, map.get(RandomEntry.class));
        Assertions.assertEquals(BothEntry.A, map.get(BothEntry.class));
    }
}
