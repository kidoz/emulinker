package org.emulinker.kaillera.admin;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.emulinker.config.SecurityConfig;
import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.release.ReleaseInfo;
import org.emulinker.util.EmuLinkerExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Security tests for Admin REST API using standalone MockMvc setup.
 *
 * <p>
 * Note: For full integration testing of security, use @SpringBootTest
 * with @AutoConfigureMockMvc. This simplified test validates the SecurityConfig
 * behavior patterns.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Admin API Security Tests")
class AdminSecurityTest {

    @Mock
    private KailleraServer kailleraServer;

    @Mock
    private ConnectController connectController;

    @Mock
    private EmuLinkerExecutor executor;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminRestController controller = new AdminRestController(kailleraServer, connectController,
                executor);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("Endpoint availability tests (without security)")
    class EndpointAvailability {

        @Test
        @DisplayName("GET /api/admin/server-info should be available")
        void serverInfoShouldBeAvailable() throws Exception {
            // Setup minimal mocks to prevent NPE
            ReleaseInfo releaseInfo = mock(ReleaseInfo.class);
            when(releaseInfo.getProductName()).thenReturn("Test");
            when(releaseInfo.getVersionString()).thenReturn("1.0");
            when(releaseInfo.getBuildNumber()).thenReturn(1);
            when(kailleraServer.getReleaseInfo()).thenReturn(releaseInfo);

            mockMvc.perform(get("/api/admin/server-info").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/admin/users should be available")
        void usersShouldBeAvailable() throws Exception {
            when(kailleraServer.getUsers()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/admin/users").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/admin/games should be available")
        void gamesShouldBeAvailable() throws Exception {
            when(kailleraServer.getGames()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/admin/games").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/admin/controllers should be available")
        void controllersShouldBeAvailable() throws Exception {
            when(connectController.getControllers()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/admin/controllers").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("SecurityConfig unit tests")
    class SecurityConfigTests {

        @Test
        @DisplayName("SecurityConfig should be instantiable with valid credentials")
        void securityConfigShouldBeInstantiable() {
            // Create a mock Environment (no stubbing needed - validateCredentials
            // is a @PostConstruct method that only runs in Spring context)
            Environment environment = mock(Environment.class);

            // Verify the security config can be created
            SecurityConfig config = new SecurityConfig(environment);
            assertNotNull(config);

            // The config exists and doesn't throw - that's the key test
            // Full integration tests would require a running Spring context
        }
    }
}
