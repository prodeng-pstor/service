package ro.unibuc.hello.controller;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
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
    @Timed(value = "getAll.comments.time", description = "Time taken to return all comments")
    @Counted(value = "getAll.comments.count", description = "Times all comments were returned")
    ResponseEntity<List<CommentEntryResponseDTO>> getAllCommentsForIncident(@PathVariable Long incidentId) {
        return ResponseEntity.ok(commentsService.getCommentsByIncidentId(incidentId));
    }

    @PostMapping("/{incidentId}")
    @Timed(value = "create.comments.time", description = "Time taken to create comment", histogram = true, percentiles = {0.5, 0.8, 0.95})
    @Counted(value = "create.comments.count", description = "Times comment was created")
    ResponseEntity<CommentEntryResponseDTO> createComment(@PathVariable Long incidentId, @RequestBody String content) {
        return ResponseEntity.ok(commentsService.addCommentToIncident(incidentId, content));
    }

    @PutMapping("/{commentId}")
    @Timed(value = "update.comments.time", description = "Time taken to update comment")
    @Counted(value = "update.comments.count", description = "Times comment was updated")
    ResponseEntity<CommentEntryResponseDTO> updateComment(@PathVariable Long commentId, @RequestBody String content) {
        return ResponseEntity.ok(commentsService.editComment(commentId, content));
    }

    @DeleteMapping("/{commentId}")
    @Timed(value = "delete.comments.time", description = "Time taken to delete comment")
    @Counted(value = "delete.comments.count", description = "Times comments was deleted")
    ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentsService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
