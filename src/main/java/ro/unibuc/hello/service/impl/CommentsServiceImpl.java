package ro.unibuc.hello.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import ro.unibuc.hello.dto.CommentEntryResponseDTO;
import ro.unibuc.hello.entity.CommentEntity;
import ro.unibuc.hello.entity.security.UserEntity;
import ro.unibuc.hello.exception.AccessViolationException;
import ro.unibuc.hello.exception.EntityNotFoundException;
import ro.unibuc.hello.repository.CommentsRepository;
import ro.unibuc.hello.repository.IncidentReportsRepository;
import ro.unibuc.hello.service.AuthService;
import ro.unibuc.hello.service.CommentsService;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CommentsServiceImpl implements CommentsService {
    private final CommentsRepository commentsRepository;
    private final UserDetailsService userDetailsService;
    private final AuthService authService;
    private final IncidentReportsRepository incidentReportsRepository;
    private final MeterRegistry metricsRegistry;
    private final AtomicLong counterAvailablility = new AtomicLong();
    private final AtomicLong counterQuality = new AtomicLong();


    public CommentsServiceImpl(CommentsRepository commentsRepository, UserDetailsService userDetailsService, AuthService authService, IncidentReportsRepository incidentReportsRepository, MeterRegistry metricsRegistry) {
        this.commentsRepository = commentsRepository;
        this.userDetailsService = userDetailsService;
        this.authService = authService;
        this.incidentReportsRepository = incidentReportsRepository;
        this.metricsRegistry = metricsRegistry;
    }

    @Override
    public List<CommentEntryResponseDTO> getCommentsByIncidentId(Long incidentId) {
        try{
            if (!incidentReportsRepository.existsById(incidentId)) {
                throw new EntityNotFoundException("Incident with id " + incidentId + " not found");
            }

            List<CommentEntryResponseDTO> result = commentsRepository.findAllByIncidentId(incidentId)
                    .stream()
                    .map(this::mapToDTO)
                    .toList();
            metricsRegistry.counter("availability.success", "endpoint", "getAllComments").increment(counterAvailablility.incrementAndGet());
            return result;
        } catch (EntityNotFoundException e) {
            metricsRegistry.counter("availability.success", "endpoint", "getAllComments").increment(counterAvailablility.incrementAndGet());
            throw e;
        } catch (Exception e) {
            metricsRegistry.counter("availability.failure", "endpoint", "getAllComments").increment(counterAvailablility.incrementAndGet());
            throw e;
        }

    }

    @Override
    public CommentEntryResponseDTO addCommentToIncident(Long incidentId, String content) {
        metricsRegistry.counter("my_non_aop_metric", "endpoint", "create").increment();

        if (content == null || content.trim().isEmpty()) {
            metricsRegistry.counter("quality.bad.input", "endpoint", "createComment").increment(counterQuality.incrementAndGet());
            throw new IllegalArgumentException("Content is empty");
        }

        if (!incidentReportsRepository.existsById(incidentId)) {
            throw new EntityNotFoundException("Incident with id " + incidentId + " not found");
        }

        CommentEntity commentEntity = CommentEntity.builder()
                .author((UserEntity) userDetailsService.loadUserByUsername(authService.getUsernameForLoggedInUser()))
                .incidentReport(incidentReportsRepository.findById(incidentId).orElseThrow(() -> new EntityNotFoundException("Incident not found")))
                .content(content)
                .build();

        return mapToDTO(commentsRepository.save(commentEntity));
    }

    @Override
    public CommentEntryResponseDTO editComment(Long commentId, String newContent) {
        metricsRegistry.counter("my_non_aop_metric", "endpoint", "update").increment();

        CommentEntity commentEntity = commentsRepository.findById(commentId).orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        if (!Objects.equals(commentEntity.getAuthor().getUsername(), authService.getUsernameForLoggedInUser())) {
            throw new AccessViolationException("You do not have permission to edit this comment");
        }

        commentEntity.setContent(newContent);
        return mapToDTO(commentsRepository.save(commentEntity));
    }

    @Override
    public void deleteComment(Long commentId) {
        metricsRegistry.counter("my_non_aop_metric", "endpoint", "delete").increment();

        CommentEntity commentEntity = commentsRepository.findById(commentId).orElseThrow(() -> new EntityNotFoundException("Comment not found"));
        commentsRepository.delete(commentEntity);
    }

    private CommentEntryResponseDTO mapToDTO(CommentEntity commentEntity) {
        return new CommentEntryResponseDTO(
                commentEntity.getId(),
                commentEntity.getIncidentReport().getId(),
                commentEntity.getAuthor().getUsername(),
                commentEntity.getCreatedAt(),
                commentEntity.getUpdatedAt(),
                commentEntity.getContent()
        );
    }
}
