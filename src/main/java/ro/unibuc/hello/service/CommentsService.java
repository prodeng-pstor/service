package ro.unibuc.hello.service;

import ro.unibuc.hello.dto.CommentEntryResponseDTO;

import java.util.List;

public interface CommentsService {
    List<CommentEntryResponseDTO> getCommentsByIncidentId(Long incidentId);

    CommentEntryResponseDTO addCommentToIncident(Long incidentId, String content);

    CommentEntryResponseDTO editComment(Long commentId, String newContent);

    void deleteComment(Long commentId);
}
