package ro.unibuc.hello.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ro.unibuc.hello.dto.IncidentReportRequestDTO;
import ro.unibuc.hello.enums.SeverityEnum;
import ro.unibuc.hello.enums.StatusEnum;
import ro.unibuc.hello.service.IncidentReportsService;

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
        final String MONGO_URL = "mongodb://localhost:";
        final String PORT = String.valueOf(mongoDBContainer.getMappedPort(27017));

        //final String POSTGRESQL_URL = "jdbc:postgresql://localhost:5432/incidents";
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

    @BeforeEach
    @WithMockUser(username = "testUser")
    public void cleanUpAndAddTestData() {
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
    void getAllIncidentReports() throws Exception {
        mockMvc.perform(get("/incidents"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
    void getIncidentReportById() throws Exception {
        mockMvc.perform(get("/incidents/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
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
    @WithMockUser(username = "testUser")
    void getMyReportedIncidentReports() throws Exception {
        mockMvc.perform(get("reportedByMe"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Server Outage"))
                .andExpect(jsonPath("$[0].description").value("The main database server is down, affecting all users."))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[0].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[0].assignedUsername").value(""))
                .andExpect(jsonPath("$[1].id").value(2L))
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
    @WithMockUser(username = "testUser")
    void getMyAssignedIncidentReports() throws Exception {
        mockMvc.perform(get("assignedToMe"))
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
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.title").value("App bug"))
                .andExpect(jsonPath("$.description").value("App doesn't display incident reports."))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.reporterUsername").value("testUser"))
                .andExpect(jsonPath("$.assignedUsername").value(""));

        mockMvc.perform(get("/incidents"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void updateIncidentReport() throws Exception {
        IncidentReportRequestDTO incident = new IncidentReportRequestDTO(
                "Minor UI Bug",
                "A button on the dashboard is misaligned on mobile devices.",
                SeverityEnum.LOW,
                StatusEnum.RESOLVED
        );

        mockMvc.perform(put("/incidents/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(incident)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].title").value("Minor UI Bug"))
                .andExpect(jsonPath("$[1].description").value("A button on the dashboard is misaligned on mobile devices."))
                .andExpect(jsonPath("$[1].severity").value("LOW"))
                .andExpect(jsonPath("$[1].status").value("RESOLVED"))
                .andExpect(jsonPath("$[1].createdAt").exists())
                .andExpect(jsonPath("$[1].updatedAt").exists())
                .andExpect(jsonPath("$[1].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[1].assignedUsername").value(""));

        mockMvc.perform(get("/incidents/2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Server Outage"))
                .andExpect(jsonPath("$[0].description").value("The main database server is down, affecting all users."))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[0].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[0].assignedUsername").value(""))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].title").value("Minor UI Bug"))
                .andExpect(jsonPath("$[1].description").value("A button on the dashboard is misaligned on mobile devices."))
                .andExpect(jsonPath("$[1].severity").value("LOW"))
                .andExpect(jsonPath("$[1].status").value("RESOLVED"))
                .andExpect(jsonPath("$[1].createdAt").exists())
                .andExpect(jsonPath("$[1].updatedAt").exists())
                .andExpect(jsonPath("$[1].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[1].assignedUsername").value(""));
    }

    @Test
    @WithMockUser(username = "testUser")
    void assignIncidentReport() throws Exception {
        mockMvc.perform(put("/incidents/assign/2/testUser"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Server Outage"))
                .andExpect(jsonPath("$[0].description").value("The main database server is down, affecting all users."))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[0].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[0].assignedUsername").value(""))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].title").value("Minor UI Bug"))
                .andExpect(jsonPath("$[1].description").value("A button on the dashboard is misaligned on mobile devices."))
                .andExpect(jsonPath("$[1].severity").value("LOW"))
                .andExpect(jsonPath("$[1].status").value("RESOLVED"))
                .andExpect(jsonPath("$[1].createdAt").exists())
                .andExpect(jsonPath("$[1].updatedAt").exists())
                .andExpect(jsonPath("$[1].reporterUsername").value("testUser"))
                .andExpect(jsonPath("$[1].assignedUsername").value("testUser"));
    }

    @Test
    void deleteIncidentReport() throws Exception {
        mockMvc.perform(delete("/incidents/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/incidents"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.title").value("Minor UI Bug"))
                .andExpect(jsonPath("$.description").value("A button on the dashboard is misaligned on mobile devices."))
                .andExpect(jsonPath("$.severity").value("LOW"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.reporterUsername").value("testUser"))
                .andExpect(jsonPath("$.assignedUsername").value(""));
    }
}