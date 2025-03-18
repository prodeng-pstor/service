package ro.unibuc.hello.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.unibuc.hello.dto.CommentEntryResponseDTO;
import ro.unibuc.hello.service.CommentsService;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentsController {
    private final CommentsService commentsService;

    public CommentsController(CommentsService commentsService) {
        this.commentsService = commentsService;
    }

    @GetMapping("/{incidentId}")
    ResponseEntity<List<CommentEntryResponseDTO>> getAllCommentsForIncident(@PathVariable Long incidentId) {
        return ResponseEntity.ok(commentsService.getCommentsByIncidentId(incidentId));
    }

    @PostMapping("/{incidentId}")
    ResponseEntity<CommentEntryResponseDTO> createComment(@PathVariable Long incidentId, @RequestBody String content) {
        return ResponseEntity.ok(commentsService.addCommentToIncident(incidentId, content));
    }

    @PutMapping("/{commentId}")
    ResponseEntity<CommentEntryResponseDTO> updateComment(@PathVariable Long commentId, @RequestBody String content) {
        return ResponseEntity.ok(commentsService.editComment(commentId, content));
    }

    @DeleteMapping("/{commentId}")
    ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentsService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
