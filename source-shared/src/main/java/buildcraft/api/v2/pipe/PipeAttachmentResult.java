package buildcraft.api.v2.pipe;

import java.util.Objects;
import java.util.Optional;

public record PipeAttachmentResult(boolean success, Optional<PipeAttachment> attachment, String reason) {
    public PipeAttachmentResult {
        attachment = Objects.requireNonNull(attachment, "attachment");
        reason = reason == null ? "" : reason;
        if (!success && attachment.isPresent()) throw new IllegalArgumentException("failed result cannot contain an attachment");
    }

    public static PipeAttachmentResult success(PipeAttachment attachment) {
        return new PipeAttachmentResult(true, Optional.of(Objects.requireNonNull(attachment, "attachment")), "");
    }

    public static PipeAttachmentResult failure(String reason) {
        return new PipeAttachmentResult(false, Optional.empty(), reason);
    }
}
