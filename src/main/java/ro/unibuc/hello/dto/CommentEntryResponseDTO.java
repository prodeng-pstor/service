package ro.unibuc.hello.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record CommentEntryResponseDTO(
        Long id,
        Long incidentId,
        String authorUsername,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime createdDate,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime updatedDate,
        String content
) {
}
