package ro.unibuc.hello.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.unibuc.hello.dto.IncidentReportRequestDTO;
import ro.unibuc.hello.dto.IncidentReportResponseDTO;
import ro.unibuc.hello.entity.security.UserEntity;
import ro.unibuc.hello.enums.SeverityEnum;
import ro.unibuc.hello.enums.StatusEnum;
import ro.unibuc.hello.repository.IncidentReportsRepository;
import ro.unibuc.hello.service.IncidentReportsService;
import ro.unibuc.hello.service.mappers.IncidentReportEntityToIncidentReportResponseDTOMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IncidentReportsControllerTest {
    @Mock
    private final IncidentReportEntityToIncidentReportResponseDTOMapper mapper = new IncidentReportEntityToIncidentReportResponseDTOMapper();

    @Mock
    private IncidentReportsService incidentReportsService;

    @Mock
    private IncidentReportsRepository incidentReportsRepository;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private Authentication authentication;

    @Mock
    private UserEntity userEntity;

    @InjectMocks
    private IncidentReportsController incidentReportsController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(incidentReportsController).build();
    }

    @Test
    void testGetAllIncidentReports() throws Exception {
        List<IncidentReportResponseDTO> incidentReports = Arrays.asList(new IncidentReportResponseDTO(
                1L,
                "Server Outage",
                "The main database server is down, affecting all users.",
                SeverityEnum.CRITICAL,
                StatusEnum.OPEN,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "testUser",
                ""
        ),  new IncidentReportResponseDTO(
                2L,
                "Minor UI Bug",
                "A button on the dashboard is misaligned on mobile devices.",
                SeverityEnum.LOW,
                StatusEnum.IN_PROGRESS,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "testUser",
                ""
        ));

        when(incidentReportsService.getAllIncidentReports()).thenReturn(incidentReports);

        mockMvc.perform(get("/incidents"))
                .andDo(print()) // This prints the actual JSON output
                .andExpect(status().isOk());

        mockMvc.perform(get("/incidents")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Server Outage"))
                .andExpect(jsonPath("$[0].description").value("The main database server is down, affecting all users."))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[0].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[0].assignedUsername").value(""))
                .andExpect(jsonPath("$[1].id").value("2"))
                .andExpect(jsonPath("$[1].title").value("Minor UI Bug"))
                .andExpect(jsonPath("$[1].description").value("A button on the dashboard is misaligned on mobile devices."))
                .andExpect(jsonPath("$[1].severity").value("LOW"))
                .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].createdAt").exists())
                .andExpect(jsonPath("$[1].updatedAt").exists())
                .andExpect(jsonPath("$[1].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[1].assignedUsername").value(""));
    }

    @Test
    void testGetIncidentReportById() throws Exception {
        List<IncidentReportResponseDTO> incidentReports = Arrays.asList(new IncidentReportResponseDTO(
                1L,
                "Server Outage",
                "The main database server is down, affecting all users.",
                SeverityEnum.CRITICAL,
                StatusEnum.OPEN,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "testUser",
                ""
        ),  new IncidentReportResponseDTO(
                2L,
                "Minor UI Bug",
                "A button on the dashboard is misaligned on mobile devices.",
                SeverityEnum.LOW,
                StatusEnum.IN_PROGRESS,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "testUser",
                ""
        ));

        when(incidentReportsService.getIncidentReportById(1L)).thenAnswer(invocation -> {
            Long requestedId = invocation.getArgument(0);
            return incidentReports.stream()
                    .filter(report -> report.id().equals(requestedId))
                    .findFirst()
                    .orElse(null);
        });

        when(incidentReportsService.getIncidentReportById(2L)).thenAnswer(invocation -> {
            Long requestedId = invocation.getArgument(0);
            return incidentReports.stream()
                    .filter(report -> report.id().equals(requestedId))
                    .findFirst()
                    .orElse(null);
        });

        mockMvc.perform(get("/incidents/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.title").value("Server Outage"))
                .andExpect(jsonPath("$.description").value("The main database server is down, affecting all users."))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.reporterUsername").value("testUser"))
                .andExpect(jsonPath("$.assignedUsername").value(""));

        mockMvc.perform(get("/incidents/2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("2"))
                .andExpect(jsonPath("$.title").value("Minor UI Bug"))
                .andExpect(jsonPath("$.description").value("A button on the dashboard is misaligned on mobile devices."))
                .andExpect(jsonPath("$.severity").value("LOW"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.reporterUsername").value("testUser"))
                .andExpect(jsonPath("$.assignedUsername").value(""));
    }

    @Test
    void testGetMyReportedIncidentReports() {

    }

    @Test
    void testGetMyAssignedIncidentReports() {

    }

    @Test
    void testAddIncidentReport() throws Exception {
        IncidentReportResponseDTO incidentReportResponse = new IncidentReportResponseDTO(
                1L,
                "Server Outage",
                "The main database server is down, affecting all users.",
                SeverityEnum.CRITICAL,
                StatusEnum.OPEN,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "testUser",
                "");
        when(incidentReportsService.createIncidentReport(any(IncidentReportRequestDTO.class))).thenReturn(incidentReportResponse);

        mockMvc.perform(post("/incidents")
                .content("{\"title\":\"Server Outage\","
                        + "\"description\":\"The main database server is down, affecting all users.\","
                        + "\"severity\":\"CRITICAL\","
                        + "\"status\":\"OPEN\"}")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.title").value("Server Outage"))
                .andExpect(jsonPath("$.description").value("The main database server is down, affecting all users."))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.reporterUsername").value("testUser"))
                .andExpect(jsonPath("$.assignedUsername").value(""));
    }

    @Test
    void testUpdateIncidentReport() throws Exception {
        IncidentReportResponseDTO incidentReportResponse = new IncidentReportResponseDTO(
                1L,
                "Server Outage - Resolved",
                "The issue has been resolved, all services restored.",
                SeverityEnum.CRITICAL,
                StatusEnum.RESOLVED,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "testUser",
                ""
        );
        when(incidentReportsService.updateIncidentReport(eq(1L), any(IncidentReportRequestDTO.class))).thenReturn(incidentReportResponse);

        mockMvc.perform(put("/incidents/1")
                .content("{\"title\":\"Server Outage - Resolved\","
                        + "\"description\":\"The issue has been resolved, all services restored.\","
                        + "\"severity\":\"CRITICAL\","
                        + "\"status\":\"RESOLVED\"}")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Server Outage - Resolved"))
                .andExpect(jsonPath("$.description").value("The issue has been resolved, all services restored."))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.reporterUsername").value("testUser"))
                .andExpect(jsonPath("$.assignedUsername").value(""));
    }

    @Test
    void assignIncidentReport() {

    }

    @Test
    void testDeleteIncidentReport() throws Exception {
        // first we mock posting an incident
        Long id = 1L;
        IncidentReportResponseDTO incidentReportResponse = new IncidentReportResponseDTO(
                id,
                "Server Outage",
                "The main database server is down, affecting all users.",
                SeverityEnum.CRITICAL,
                StatusEnum.OPEN,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "testUser",
                "");
        when(incidentReportsService.createIncidentReport(any(IncidentReportRequestDTO.class))).thenReturn(incidentReportResponse);

        mockMvc.perform(post("/incidents")
                        .content("{\"title\":\"Server Outage\","
                                + "\"description\":\"The main database server is down, affecting all users.\","
                                + "\"severity\":\"CRITICAL\","
                                + "\"status\":\"OPEN\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.title").value("Server Outage"))
                .andExpect(jsonPath("$.description").value("The main database server is down, affecting all users."))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.reporterUsername").value("testUser"))
                .andExpect(jsonPath("$.assignedUsername").value(""));

        // then we delete it
        mockMvc.perform(delete("/incidents/{id}", id))
                .andExpect(status().isNoContent());

        verify(incidentReportsService, times(1)).deleteIncidentReport(id);

        mockMvc.perform(get("/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}