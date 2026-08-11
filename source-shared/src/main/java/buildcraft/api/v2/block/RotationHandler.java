package buildcraft.api.v2.block;

@FunctionalInterface
public interface RotationHandler {
    RotationResult rotate(RotationContext context);
}
