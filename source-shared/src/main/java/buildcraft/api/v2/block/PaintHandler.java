package buildcraft.api.v2.block;

@FunctionalInterface
public interface PaintHandler {
    PaintResult paint(PaintContext context);
}
