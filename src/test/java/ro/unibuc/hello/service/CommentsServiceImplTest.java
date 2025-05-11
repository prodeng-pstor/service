package ro.unibuc.hello.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetailsService;
import ro.unibuc.hello.dto.CommentEntryResponseDTO;
import ro.unibuc.hello.entity.CommentEntity;
import ro.unibuc.hello.entity.IncidentReportEntity;
import ro.unibuc.hello.entity.security.UserEntity;
import ro.unibuc.hello.exception.AccessViolationException;
import ro.unibuc.hello.exception.EntityNotFoundException;
import ro.unibuc.hello.repository.CommentsRepository;
import ro.unibuc.hello.repository.IncidentReportsRepository;
import ro.unibuc.hello.service.impl.CommentsServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommentsServiceImplTest {
    @Mock
    private CommentsRepository commentsRepository;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private AuthService authService;

    @Mock
    private IncidentReportsRepository incidentReportsRepository;

    @Mock
    private MeterRegistry metricsRegistry;

    @InjectMocks
    private CommentsServiceImpl commentsService;

    private CommentEntity commentEntity;
    private CommentEntryResponseDTO commentEntryResponseDTO;
    private IncidentReportEntity incidentReportEntity;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userEntity = UserEntity.builder()
                .username("testUser")
                .build();

        incidentReportEntity = IncidentReportEntity.builder()
                .title("test report")
                .build();
        incidentReportEntity.setId(1L);

        commentEntity = CommentEntity.builder()
                .incidentReport(incidentReportEntity)
                .author(userEntity)
                .content("This is a comment.")
                .build();

        commentEntryResponseDTO = new CommentEntryResponseDTO(
                commentEntity.getId(),
                1L,
                commentEntity.getAuthor().getUsername(),
                commentEntity.getCreatedAt(),
                commentEntity.getUpdatedAt(),
                commentEntity.getContent()
        );
    }

    @Test
    void testGetCommentsByIncidentId_ShouldReturnComments_WhenIncidentExists() {
        Counter successCounter = mock(Counter.class);
        when(metricsRegistry.counter(eq("availability.success"), eq("endpoint"), eq("getAllComments"))).thenReturn(successCounter);
        doNothing().when(successCounter).increment();

        Counter failureCounter = mock(Counter.class);
        when(metricsRegistry.counter(eq("availability.failure"), eq("endpoint"), eq("getAllComments"))).thenReturn(failureCounter);
        doNothing().when(failureCounter).increment();

        Long incidentId = 1L;
        when(incidentReportsRepository.existsById(incidentId)).thenReturn(true);
        when(commentsRepository.findAllByIncidentId(incidentId)).thenReturn(List.of(commentEntity));

        List<CommentEntryResponseDTO> result = commentsService.getCommentsByIncidentId(incidentId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(commentEntryResponseDTO, result.getFirst());
    }

    @Test
    void testGetCommentsByIncidentId_ShouldThrowEntityNotFound_WhenIncidentNotFound() {
        Counter successCounter = mock(Counter.class);
        when(metricsRegistry.counter(eq("availability.success"), eq("endpoint"), eq("getAllComments"))).thenReturn(successCounter);
        doNothing().when(successCounter).increment();

        Long incidentId = 1L;
        when(incidentReportsRepository.existsById(incidentId)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> commentsService.getCommentsByIncidentId(incidentId));
    }

    @Test
    void testAddCommentToIncident_ShouldAddComment_WhenIncidentExists() {
        Counter qualityCounter = mock(Counter.class);
        when(metricsRegistry.counter("quality.bad.input", "endpoint", "createComment")).thenReturn(qualityCounter);
        doNothing().when(qualityCounter).increment();

        Counter counterMock = Mockito.mock(Counter.class);
        when(metricsRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counterMock);
        doNothing().when(counterMock).increment();

        Long incidentId = 1L;
        String content = "New comment content.";
        when(incidentReportsRepository.existsById(incidentId)).thenReturn(true);
        when(incidentReportsRepository.findById(incidentId)).thenReturn(Optional.of(incidentReportEntity));
        when(authService.getUsernameForLoggedInUser()).thenReturn("testUser");
        when(userDetailsService.loadUserByUsername("testUser")).thenReturn(userEntity);
        when(commentsRepository.save(any(CommentEntity.class))).thenReturn(commentEntity);

        CommentEntryResponseDTO result = commentsService.addCommentToIncident(incidentId, content);

        assertNotNull(result);
        assertEquals(commentEntryResponseDTO, result);
    }

    @Test
    void testAddCommentToIncident_ShouldThrowEntityNotFound_WhenIncidentNotFound() {
        Counter qualityCounter = mock(Counter.class);
        when(metricsRegistry.counter("quality.bad.input", "endpoint", "createComment")).thenReturn(qualityCounter);
        doNothing().when(qualityCounter).increment();

        Counter counterMock = Mockito.mock(Counter.class);
        when(metricsRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counterMock);
        doNothing().when(counterMock).increment();

        Long incidentId = 1L;
        String content = "New comment content";
        when(incidentReportsRepository.existsById(incidentId)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> commentsService.addCommentToIncident(incidentId, content));
    }

    @Test
    void testEditComment_ShouldEditComment_WhenCommentExistsAndUserHasPermission() {
        Counter counterMock = Mockito.mock(Counter.class);
        when(metricsRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counterMock);
        doNothing().when(counterMock).increment();

        Long commentId = 1L;
        String newContent = "Updated content";
        when(commentsRepository.findById(commentId)).thenReturn(Optional.of(commentEntity));
        when(authService.getUsernameForLoggedInUser()).thenReturn("testUser");
        when(commentsRepository.save(any(CommentEntity.class))).thenReturn(commentEntity);

        CommentEntryResponseDTO result = commentsService.editComment(commentId, newContent);

        assertNotNull(result);
        assertEquals(newContent, result.content());
    }

    @Test
    void testEditComment_ShouldThrowEntityNotFound_WhenCommentNotFound() {
        Counter counterMock = Mockito.mock(Counter.class);
        when(metricsRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counterMock);
        doNothing().when(counterMock).increment();

        Long commentId = 1L;
        String newContent = "Updated content";
        when(commentsRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> commentsService.editComment(commentId, newContent));
    }

    @Test
    void testEditComment_ShouldThrowAccessViolation_WhenUserHasNoPermission() {
        Counter counterMock = Mockito.mock(Counter.class);
        when(metricsRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counterMock);
        doNothing().when(counterMock).increment();

        Long commentId = 1L;
        String newContent = "Updated content";

        UserEntity otherUserEntity = UserEntity.builder()
                .username("testUser12")
                .build();

        CommentEntity otherUserComment = CommentEntity.builder()
                .author(otherUserEntity)
                .content("This is a different comment")
                .build();

        when(commentsRepository.findById(commentId)).thenReturn(Optional.of(otherUserComment));
        when(authService.getUsernameForLoggedInUser()).thenReturn("testUser");

        assertThrows(AccessViolationException.class, () -> commentsService.editComment(commentId, newContent));
    }

    @Test
    void testDeleteComment_ShouldDeleteComment_WhenCommentExists() {
        Counter counterMock = Mockito.mock(Counter.class);
        when(metricsRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counterMock);
        doNothing().when(counterMock).increment();

        Long commentId = 1L;
        when(commentsRepository.findById(commentId)).thenReturn(Optional.of(commentEntity));

        commentsService.deleteComment(commentId);

        verify(commentsRepository, times(1)).delete(commentEntity);
    }

    @Test
    void testDeleteComment_ShouldThrowEntityNotFound_WhenCommentNotFound() {
        Counter counterMock = Mockito.mock(Counter.class);
        when(metricsRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counterMock);
        doNothing().when(counterMock).increment();

        Long commentId = 1L;
        when(commentsRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> commentsService.deleteComment(commentId));
    }
}
