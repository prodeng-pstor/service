package ro.unibuc.hello.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import ro.unibuc.hello.dto.IncidentReportRequestDTO;
import ro.unibuc.hello.dto.IncidentReportResponseDTO;
import ro.unibuc.hello.entity.IncidentReportEntity;
import ro.unibuc.hello.entity.security.UserEntity;
import ro.unibuc.hello.exception.EntityNotFoundException;
import ro.unibuc.hello.repository.IncidentReportsRepository;
import ro.unibuc.hello.service.IncidentReportsService;

import java.util.List;

@Service
public class IncidentReportsServiceImpl implements IncidentReportsService {
    private final IncidentReportsRepository incidentReportsRepository;
    private final UserDetailsService userDetailsService;


    public IncidentReportsServiceImpl(IncidentReportsRepository incidentReportsRepository, UserDetailsService userDetailsService) {
        this.incidentReportsRepository = incidentReportsRepository;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public List<IncidentReportResponseDTO> getAllIncidentReports() {
        return incidentReportsRepository.findAll().stream()
                .map(this::makeDTO)
                .toList();
    }

    @Override
    public IncidentReportResponseDTO getIncidentReportById(Long id) {
        return makeDTO(incidentReportsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Incident report couldn't be found.")));
    }

    @Override
    public List<IncidentReportResponseDTO> getUserIncidentReports() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserEntity userEntity = (UserEntity) userDetailsService.loadUserByUsername(authentication.getName());
        return incidentReportsRepository.findByUser(userEntity).stream()
                .map(this::makeDTO)
                .toList();
    }

    @Override
    public IncidentReportResponseDTO createIncidentReport(IncidentReportRequestDTO incidentReport) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserEntity userEntity = (UserEntity) userDetailsService.loadUserByUsername(authentication.getName());
        IncidentReportEntity incidentReportEntity = makeEntity(incidentReport);
        incidentReportEntity.setUser(userEntity);
        incidentReportsRepository.save(incidentReportEntity);
        return makeDTO(incidentReportEntity);
    }

    @Override
    public IncidentReportResponseDTO updateIncidentReport(Long id, IncidentReportRequestDTO incidentReport) throws EntityNotFoundException {
        IncidentReportEntity entity = incidentReportsRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Incident report couldn't be found."));
        entity.setTitle(incidentReport.title());
        entity.setDescription(incidentReport.description());
        entity.setSeverity(incidentReport.severity());
        entity.setStatus(incidentReport.status());
        return makeDTO(incidentReportsRepository.save(entity));
    }

    @Override
    public void deleteIncidentReport(Long id) throws EntityNotFoundException {
        if (incidentReportsRepository.existsById(id)) {
            incidentReportsRepository.deleteById(id);
        } else {
            throw new EntityNotFoundException("Incident report couldn't be found.");
        }
    }

    private IncidentReportResponseDTO makeDTO(IncidentReportEntity incidentReportEntity) {
        return new IncidentReportResponseDTO(incidentReportEntity.getId(),
                incidentReportEntity.getTitle(),
                incidentReportEntity.getDescription(),
                incidentReportEntity.getSeverity(),
                incidentReportEntity.getStatus(),
                incidentReportEntity.getCreatedAt(),
                incidentReportEntity.getUpdatedAt(),
                incidentReportEntity.getUser().getUsername());
    }

    private IncidentReportEntity makeEntity(IncidentReportRequestDTO incidentReportRequestDTO) {
        return IncidentReportEntity.builder()
                .title(incidentReportRequestDTO.title())
                .description(incidentReportRequestDTO.description())
                .status(incidentReportRequestDTO.status())
                .severity(incidentReportRequestDTO.severity())
                .build();
    }
}
