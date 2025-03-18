package ro.unibuc.hello.dto;

import java.time.LocalDateTime;

public record CommentEntryResponseDTO(
        Long id,
        Long incidentId,
        String authorUsername,
        LocalDateTime createdDate,
        LocalDateTime updatedDate,
        String content
) {
}
