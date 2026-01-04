package su.kidoz.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for the Kaillux Admin REST API.
 */
@Configuration
public class OpenApiConfig {

    @Value("${su.kidoz.kaillera.release.version:1.0.0}")
    private String version;

    @Bean
    public OpenAPI kailluxOpenAPI() {
        return new OpenAPI().info(new Info().title("Kaillux Admin API")
                .description("REST API for administering the Kaillux Kaillera Server. "
                        + "Provides endpoints for monitoring server status, managing users, "
                        + "viewing active games, and performing administrative actions.")
                .version(version)
                .contact(new Contact().name("Kaillux Project")
                        .url("https://github.com/kidoz/kaillux"))
                .license(new License().name("GPL-2.0")
                        .url("https://opensource.org/licenses/GPL-2.0")))
                .servers(List.of(new Server().url("/").description("Current server")))
                .tags(List.of(
                        new Tag().name("Server")
                                .description("Server status and configuration information"),
                        new Tag().name("Users").description("User management operations"),
                        new Tag().name("Games").description("Game monitoring operations"),
                        new Tag().name("Controllers")
                                .description("Protocol controller information"),
                        new Tag().name("Health").description("Kubernetes health probe endpoints")))
                .components(new Components().addSecuritySchemes("basicAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")
                                .description("HTTP Basic Authentication for admin endpoints")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}
