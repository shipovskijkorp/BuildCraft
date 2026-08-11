package buildcraft.api.v2.block;

public interface BlockInteractionService {
    RotationResult rotate(RotationContext context);
    PaintResult paint(PaintContext context);
}
