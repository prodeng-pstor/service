package ro.unibuc.hello.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.unibuc.hello.dto.CommentEntryResponseDTO;
import ro.unibuc.hello.exception.EntityNotFoundException;
import ro.unibuc.hello.middleware.GlobalExceptionHandler;
import ro.unibuc.hello.service.CommentsService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentsControllerTest {
    @Mock
    private CommentsService commentsService;

    @InjectMocks
    private CommentsController commentsController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(commentsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetAllCommentsForIncident_ShouldReturnComments_WhenIncidentExists() throws Exception {
        List<CommentEntryResponseDTO> comments = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        LocalDateTime now = LocalDateTime.now();
        String formattedNow = now.format(formatter);

        comments.add(new CommentEntryResponseDTO(1L, 1L, "zaha", now, now, "comment 1"));

        when(commentsService.getCommentsByIncidentId(1L)).thenReturn(comments);

        mockMvc.perform(get("/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].incidentId").value(1))
                .andExpect(jsonPath("$[0].authorUsername").value("zaha"))
                .andExpect(jsonPath("$[0].createdDate").value(formattedNow))
                .andExpect(jsonPath("$[0].updatedDate").value(formattedNow))
                .andExpect(jsonPath("$[0].content").value("comment 1"));
    }

    @Test
    void testGetAllCommentsForIncident_ShouldReturnNotFound_WhenIncidentsDoNotExist() throws Exception {
        when(commentsService.getCommentsByIncidentId(1L)).thenThrow(new EntityNotFoundException("Incident not found"));

        mockMvc.perform(get("/comments/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateComment_ShouldReturnCreatedComment_WhenIncidentExists() throws Exception {
        String content = "New comment content";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        LocalDateTime now = LocalDateTime.now();
        String formattedNow = now.format(formatter);

        CommentEntryResponseDTO createdComment = new CommentEntryResponseDTO(1L, 1L, "zaha", now, now, content);

        when(commentsService.addCommentToIncident(1L, content)).thenReturn(createdComment);

        mockMvc.perform(post("/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.incidentId").value(1))
                .andExpect(jsonPath("$.authorUsername").value("zaha"))
                .andExpect(jsonPath("$.createdDate").value(formattedNow))
                .andExpect(jsonPath("$.updatedDate").value(formattedNow))
                .andExpect(jsonPath("$.content").value(content));
    }

    @Test
    void testUpdateComment_ShouldReturnUpdatedComment_WhenCommentExists() throws Exception {
        String updatedContent = "Updated comment content";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        LocalDateTime now = LocalDateTime.now();
        String formattedNow = now.format(formatter);

        CommentEntryResponseDTO updatedComment = new CommentEntryResponseDTO(1L, 1L, "zaha", now, now, updatedContent);

        when(commentsService.editComment(1L, updatedContent)).thenReturn(updatedComment);

        mockMvc.perform(put("/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.incidentId").value(1))
                .andExpect(jsonPath("$.authorUsername").value("zaha"))
                .andExpect(jsonPath("$.createdDate").value(formattedNow))
                .andExpect(jsonPath("$.updatedDate").value(formattedNow))
                .andExpect(jsonPath("$.content").value(updatedContent));
    }

    @Test
    void testDeleteComment_ShouldReturnNoContent_WhenCommentExists() throws Exception {
        doNothing().when(commentsService).deleteComment(1L);

        mockMvc.perform(delete("/comments/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testCreateComment_ShouldReturnBadRequest_WhenContentIsInvalid() throws Exception {
        String invalidContent = "";

        mockMvc.perform(post("/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidContent))
                .andExpect(status().isBadRequest());
    }

}
