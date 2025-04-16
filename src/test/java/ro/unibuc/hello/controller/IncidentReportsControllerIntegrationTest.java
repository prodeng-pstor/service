package ro.unibuc.hello.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ro.unibuc.hello.dto.IncidentReportRequestDTO;
import ro.unibuc.hello.dto.IncidentReportResponseDTO;
import ro.unibuc.hello.entity.security.UserEntity;
import ro.unibuc.hello.enums.SeverityEnum;
import ro.unibuc.hello.enums.StatusEnum;
import ro.unibuc.hello.repository.IncidentReportsRepository;
import ro.unibuc.hello.repository.security.UsersRepository;
import ro.unibuc.hello.service.IncidentReportsService;

import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("IntegrationTest")
class IncidentReportsControllerIntegrationTest {
    @Container
    public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0.20")
            .withExposedPorts(27017)
            .withSharding();

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("incidents")
            .withUsername("incidents")
            .withPassword("incidents");

    @BeforeAll
    public static void setUp() {
        mongoDBContainer.start();
        postgreSQLContainer.start();
    }

    @AfterAll
    public static void tearDown() {
        mongoDBContainer.stop();
        postgreSQLContainer.stop();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        final String MONGO_URL = "mongodb://host.docker.internal";
        final String PORT = String.valueOf(mongoDBContainer.getMappedPort(27017));

        final String USER = "incidents";
        final String PASSWORD = "incidents";

        registry.add("mongodb.connection.url", () -> MONGO_URL + PORT);
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncidentReportsService incidentReportsService;

    @Autowired
    private IncidentReportsRepository incidentReportsRepository;

    @Autowired
    private UsersRepository usersRepository;

    @BeforeEach
    @WithMockUser(username = "testUser")
    public void cleanUpAndAddTestData() {
        UserEntity testUser;

        if (usersRepository.findByUsername("testUser").isEmpty()) {
            testUser = new UserEntity("testUser", "password", Collections.emptySet());
            usersRepository.save(testUser);
        }

        incidentReportsRepository.deleteAll();
        incidentReportsRepository.flush();

        // Creating test Incident Report objects.
        IncidentReportRequestDTO incident1 = new IncidentReportRequestDTO(
                "Server Outage",
                "The main database server is down, affecting all users.",
                SeverityEnum.CRITICAL,
                StatusEnum.OPEN
        );
        IncidentReportRequestDTO incident2 = new IncidentReportRequestDTO(
                "Minor UI Bug",
                "A button on the dashboard is misaligned on mobile devices.",
                SeverityEnum.LOW,
                StatusEnum.IN_PROGRESS
        );

        // Saving test Incident Reports to the database via service.
        incidentReportsService.createIncidentReport(incident1);
        incidentReportsService.createIncidentReport(incident2);
    }

    @Test
    @WithMockUser(username = "testUser")
    void getAllIncidentReports() throws Exception {
        mockMvc.perform(get("/incidents"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].title").value("Server Outage"))
                .andExpect(jsonPath("$[0].description").value("The main database server is down, affecting all users."))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[0].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[0].assignedUsername").value("Unassigned"))
                .andExpect(jsonPath("$[1].title").value("Minor UI Bug"))
                .andExpect(jsonPath("$[1].description").value("A button on the dashboard is misaligned on mobile devices."))
                .andExpect(jsonPath("$[1].severity").value("LOW"))
                .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].createdAt").exists())
                .andExpect(jsonPath("$[1].updatedAt").exists())
                .andExpect(jsonPath("$[1].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[1].assignedUsername").value("Unassigned"));
    }

    @Test
    @WithMockUser(username = "testUser")
    void getIncidentReportById() throws Exception {
        IncidentReportRequestDTO incident3 = new IncidentReportRequestDTO(
                "App bug",
                "App doesn't display incident reports.",
                SeverityEnum.CRITICAL,
                StatusEnum.OPEN
        );

        ResultActions postResponse = mockMvc.perform(post("/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(incident3)))
                .andExpect(status().isOk());

        String incidentId = postResponse.andReturn().getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        IncidentReportResponseDTO responseDTO = objectMapper.readValue(incidentId, IncidentReportResponseDTO.class);

        mockMvc.perform(get("/incidents/{id}", responseDTO.id()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(responseDTO.id()))
                .andExpect(jsonPath("$.title").value("App bug"))
                .andExpect(jsonPath("$.description").value("App doesn't display incident reports."))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.reporterUsername").value("testUser"))
                .andExpect(jsonPath("$.assignedUsername").value("Unassigned"));
    }

    @Test
    @WithMockUser(username = "testUser")
    void getMyReportedIncidentReports() throws Exception {
        mockMvc.perform(get("/incidents/reportedByMe"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Server Outage"))
                .andExpect(jsonPath("$[0].description").value("The main database server is down, affecting all users."))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[0].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[0].assignedUsername").value("Unassigned"))
                .andExpect(jsonPath("$[1].title").value("Minor UI Bug"))
                .andExpect(jsonPath("$[1].description").value("A button on the dashboard is misaligned on mobile devices."))
                .andExpect(jsonPath("$[1].severity").value("LOW"))
                .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].createdAt").exists())
                .andExpect(jsonPath("$[1].updatedAt").exists())
                .andExpect(jsonPath("$[1].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[1].assignedUsername").value("Unassigned"));
    }

    @Test
    @WithMockUser(username = "testUser")
    void getMyAssignedIncidentReports() throws Exception {
        mockMvc.perform(get("/incidents/assignedToMe"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(username = "testUser")
    void addIncidentReport() throws Exception {
        IncidentReportRequestDTO incident3 = new IncidentReportRequestDTO(
                "App bug",
                "App doesn't display incident reports.",
                SeverityEnum.CRITICAL,
                StatusEnum.OPEN
        );

        mockMvc.perform(post("/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(incident3)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("App bug"))
                .andExpect(jsonPath("$.description").value("App doesn't display incident reports."))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.reporterUsername").value("testUser"))
                .andExpect(jsonPath("$.assignedUsername").value("Unassigned"));

        mockMvc.perform(get("/incidents"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @WithMockUser(username = "testUser")
    void updateIncidentReport() throws Exception {

        MvcResult getResult = mockMvc.perform(get("/incidents"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String jsonResponse = getResult.getResponse().getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        List<IncidentReportResponseDTO> incidents = objectMapper.readValue(
                jsonResponse,
                new TypeReference<List<IncidentReportResponseDTO>>() {}
        );
        IncidentReportResponseDTO secondIncident = incidents.get(1);

        IncidentReportRequestDTO incident = new IncidentReportRequestDTO(
                "Minor UI Bug",
                "A button on the dashboard is misaligned on mobile devices.",
                SeverityEnum.LOW,
                StatusEnum.RESOLVED
        );

        mockMvc.perform(put("/incidents/{id}", secondIncident.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(incident)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Minor UI Bug"))
                .andExpect(jsonPath("$.description").value("A button on the dashboard is misaligned on mobile devices."))
                .andExpect(jsonPath("$.severity").value("LOW"))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.reporterUsername").value("testUser"))
                .andExpect(jsonPath("$.assignedUsername").value("Unassigned"));

        mockMvc.perform(get("/incidents"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Server Outage"))
                .andExpect(jsonPath("$[0].description").value("The main database server is down, affecting all users."))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[0].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[0].assignedUsername").value("Unassigned"))
                .andExpect(jsonPath("$[1].title").value("Minor UI Bug"))
                .andExpect(jsonPath("$[1].description").value("A button on the dashboard is misaligned on mobile devices."))
                .andExpect(jsonPath("$[1].severity").value("LOW"))
                .andExpect(jsonPath("$[1].status").value("RESOLVED"))
                .andExpect(jsonPath("$[1].createdAt").exists())
                .andExpect(jsonPath("$[1].updatedAt").exists())
                .andExpect(jsonPath("$[1].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[1].assignedUsername").value("Unassigned"));
    }

    @Test
    @WithMockUser(username = "testUser")
    void assignIncidentReport() throws Exception {
        MvcResult getResult = mockMvc.perform(get("/incidents"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String jsonResponse = getResult.getResponse().getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        List<IncidentReportResponseDTO> incidents = objectMapper.readValue(
                jsonResponse,
                new TypeReference<List<IncidentReportResponseDTO>>() {}
        );
        IncidentReportResponseDTO secondIncident = incidents.get(1);

        mockMvc.perform(put("/incidents/assign/{id}/testUser", secondIncident.id()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Minor UI Bug"))
                .andExpect(jsonPath("$.description").value("A button on the dashboard is misaligned on mobile devices."))
                .andExpect(jsonPath("$.severity").value("LOW"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.reporterUsername").value("testUser"))
                .andExpect(jsonPath("$.assignedUsername").value("testUser"));

        mockMvc.perform(get("/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Server Outage"))
                .andExpect(jsonPath("$[0].description").value("The main database server is down, affecting all users."))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[0].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[0].assignedUsername").value("Unassigned"))
                .andExpect(jsonPath("$[1].title").value("Minor UI Bug"))
                .andExpect(jsonPath("$[1].description").value("A button on the dashboard is misaligned on mobile devices."))
                .andExpect(jsonPath("$[1].severity").value("LOW"))
                .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].createdAt").exists())
                .andExpect(jsonPath("$[1].updatedAt").exists())
                .andExpect(jsonPath("$[1].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[1].assignedUsername").value("testUser"));
    }

    @Test
    @WithMockUser(username = "testUser")
    void deleteIncidentReport() throws Exception {

        MvcResult getResult = mockMvc.perform(get("/incidents"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String jsonResponse = getResult.getResponse().getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        List<IncidentReportResponseDTO> incidents = objectMapper.readValue(
                jsonResponse,
                new TypeReference<List<IncidentReportResponseDTO>>() {}
        );
        IncidentReportResponseDTO secondIncident = incidents.getFirst();

        mockMvc.perform(delete("/incidents/{id}", secondIncident.id()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/incidents"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Minor UI Bug"))
                .andExpect(jsonPath("$[0].description").value("A button on the dashboard is misaligned on mobile devices."))
                .andExpect(jsonPath("$[0].severity").value("LOW"))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[0].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[0].assignedUsername").value("Unassigned"));
    }
}