package buildcraft.robotics;

import buildcraft.robotics.entity.EntityRobot;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class BCRoboticsEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, BCRobotics.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<EntityRobot>> ROBOT = ENTITIES.register("robot", () ->
            EntityType.Builder.<EntityRobot>of(EntityRobot::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build(BCRobotics.MODID + ":robot"));

    private BCRoboticsEntities() {
    }

    public static void registry(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
