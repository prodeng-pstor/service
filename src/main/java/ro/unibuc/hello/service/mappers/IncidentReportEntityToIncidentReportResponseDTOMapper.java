package ro.unibuc.hello.service.mappers;

import org.springframework.stereotype.Component;
import ro.unibuc.hello.dto.IncidentReportResponseDTO;
import ro.unibuc.hello.entity.IncidentReportEntity;
import ro.unibuc.hello.entity.security.UserEntity;

import java.util.Optional;
import java.util.function.Function;

@Component
public class IncidentReportEntityToIncidentReportResponseDTOMapper implements Function<IncidentReportEntity, IncidentReportResponseDTO> {
    @Override
    public IncidentReportResponseDTO apply(IncidentReportEntity incidentReportEntity) {
        String incidentReporterUsername = Optional.ofNullable(incidentReportEntity)
                .map(IncidentReportEntity::getIncidentReporter)  // Get the incidentReporter UserEntity
                .map(UserEntity::getUsername)                    // Get the username
                .orElse("Unassigned");

        String assignedUsername = Optional.ofNullable(incidentReportEntity)
                .map(IncidentReportEntity::getAssignedUser)
                .map(UserEntity::getUsername)
                .orElse("Unassigned");

        return new IncidentReportResponseDTO(incidentReportEntity.getId(),
                incidentReportEntity.getTitle(),
                incidentReportEntity.getDescription(),
                incidentReportEntity.getSeverity(),
                incidentReportEntity.getStatus(),
                incidentReportEntity.getCreatedAt(),
                incidentReportEntity.getUpdatedAt(),
                incidentReporterUsername,
                assignedUsername);
    }
}
