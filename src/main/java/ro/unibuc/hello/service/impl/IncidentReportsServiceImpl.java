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
import ro.unibuc.hello.service.mappers.IncidentReportEntityToIncidentReportResponseDTOMapper;

import java.util.List;

@Service
public class IncidentReportsServiceImpl implements IncidentReportsService {
    private final IncidentReportsRepository incidentReportsRepository;
    private final UserDetailsService userDetailsService;
    private final IncidentReportEntityToIncidentReportResponseDTOMapper incidentReportEntityToIncidentReportResponseDTOMapper;

    public IncidentReportsServiceImpl(IncidentReportsRepository incidentReportsRepository, UserDetailsService userDetailsService, IncidentReportEntityToIncidentReportResponseDTOMapper incidentReportEntityToIncidentReportResponseDTOMapper) {
        this.incidentReportsRepository = incidentReportsRepository;
        this.userDetailsService = userDetailsService;
        this.incidentReportEntityToIncidentReportResponseDTOMapper = incidentReportEntityToIncidentReportResponseDTOMapper;
    }

    @Override
    public List<IncidentReportResponseDTO> getAllIncidentReports() {
        return incidentReportsRepository.findAll().stream()
                .map(incidentReportEntityToIncidentReportResponseDTOMapper)
                .toList();
    }

    @Override
    public IncidentReportResponseDTO getIncidentReportById(Long id) {
        return incidentReportEntityToIncidentReportResponseDTOMapper.apply(incidentReportsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Incident report couldn't be found.")));
    }

    @Override
    public List<IncidentReportResponseDTO> getIncidentReportsReportedByLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserEntity userEntity = (UserEntity) userDetailsService.loadUserByUsername(authentication.getName());
        return incidentReportsRepository.findByIncidentReporter(userEntity).stream()
                .map(incidentReportEntityToIncidentReportResponseDTOMapper)
                .toList();
    }

    @Override
    public List<IncidentReportResponseDTO> getIncidentReportsAssignedToLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserEntity userEntity = (UserEntity) userDetailsService.loadUserByUsername(authentication.getName());
        return incidentReportsRepository.findByAssignedUser(userEntity).stream()
                .map(incidentReportEntityToIncidentReportResponseDTOMapper)
                .toList();
    }

    @Override
    public IncidentReportResponseDTO createIncidentReport(IncidentReportRequestDTO incidentReport) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserEntity userEntity = (UserEntity) userDetailsService.loadUserByUsername(authentication.getName());
        IncidentReportEntity incidentReportEntity = makeEntity(incidentReport);
        incidentReportEntity.setIncidentReporter(userEntity);
        incidentReportsRepository.save(incidentReportEntity);
        return incidentReportEntityToIncidentReportResponseDTOMapper.apply(incidentReportEntity);
    }

    @Override
    public IncidentReportResponseDTO updateIncidentReport(Long id, IncidentReportRequestDTO incidentReport) {
        IncidentReportEntity entity = incidentReportsRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Incident report couldn't be found."));
        entity.setTitle(incidentReport.title());
        entity.setDescription(incidentReport.description());
        entity.setSeverity(incidentReport.severity());
        entity.setStatus(incidentReport.status());
        return incidentReportEntityToIncidentReportResponseDTOMapper.apply(incidentReportsRepository.save(entity));
    }

    @Override
    public IncidentReportResponseDTO assignIncidentReport(Long id, String username) {
        IncidentReportEntity entity = incidentReportsRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Incident report couldn't be found."));
        UserEntity userEntity = (UserEntity) userDetailsService.loadUserByUsername(username);
        entity.setAssignedUser(userEntity);
        return incidentReportEntityToIncidentReportResponseDTOMapper.apply(incidentReportsRepository.save(entity));
    }

    @Override
    public void deleteIncidentReport(Long id) {
        if (incidentReportsRepository.existsById(id)) {
            incidentReportsRepository.deleteById(id);
        } else {
            throw new EntityNotFoundException("Incident report couldn't be found.");
        }
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
