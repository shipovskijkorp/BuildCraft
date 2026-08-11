package buildcraft.api.v2.pipe;

@FunctionalInterface
public interface PipeAttachmentFactory<A extends PipeAttachment> {
    A create(PipeAttachmentPlacementContext context);
}
