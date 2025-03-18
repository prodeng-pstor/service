package ro.unibuc.hello.service;

import ro.unibuc.hello.dto.IncidentReportRequestDTO;
import ro.unibuc.hello.dto.IncidentReportResponseDTO;

import java.util.List;


public interface IncidentReportsService {
     List<IncidentReportResponseDTO> getAllIncidentReports();
     IncidentReportResponseDTO getIncidentReportById(Long id);
     List<IncidentReportResponseDTO> getIncidentReportsReportedByLoggedInUser();
     List<IncidentReportResponseDTO> getIncidentReportsAssignedToLoggedInUser();
     IncidentReportResponseDTO createIncidentReport(IncidentReportRequestDTO incidentReport);
     IncidentReportResponseDTO updateIncidentReport(Long id, IncidentReportRequestDTO incidentReport);
     IncidentReportResponseDTO assignIncidentReport(Long id, String username);
     void deleteIncidentReport(Long id);
}
