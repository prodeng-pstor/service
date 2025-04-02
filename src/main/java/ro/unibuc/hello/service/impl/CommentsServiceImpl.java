package ro.unibuc.hello.service.impl;

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

@Service
public class CommentsServiceImpl implements CommentsService {
    private final CommentsRepository commentsRepository;
    private final UserDetailsService userDetailsService;
    private final AuthService authService;
    private final IncidentReportsRepository incidentReportsRepository;

    public CommentsServiceImpl(CommentsRepository commentsRepository, UserDetailsService userDetailsService, AuthService authService, IncidentReportsRepository incidentReportsRepository) {
        this.commentsRepository = commentsRepository;
        this.userDetailsService = userDetailsService;
        this.authService = authService;
        this.incidentReportsRepository = incidentReportsRepository;
    }

    @Override
    public List<CommentEntryResponseDTO> getCommentsByIncidentId(Long incidentId) {
        if (!incidentReportsRepository.existsById(incidentId)) {
            throw new EntityNotFoundException("Incident with id " + incidentId + " not found");
        }

        return commentsRepository.findAllByIncidentId(incidentId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public CommentEntryResponseDTO addCommentToIncident(Long incidentId, String content) {
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
        CommentEntity commentEntity = commentsRepository.findById(commentId).orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        if (!Objects.equals(commentEntity.getAuthor().getUsername(), authService.getUsernameForLoggedInUser())) {
            throw new AccessViolationException("You do not have permission to edit this comment");
        }

        commentEntity.setContent(newContent);
        return mapToDTO(commentsRepository.save(commentEntity));
    }

    @Override
    public void deleteComment(Long commentId) {
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
