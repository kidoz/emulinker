package su.kidoz.kaillera.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kubernetes health probe endpoints.
 *
 * <p>
 * Provides standard Kubernetes liveness and readiness probes for container
 * orchestration.
 */
@RestController
@RequestMapping("/healthz")
@Tag(name = "Health", description = "Kubernetes health probe endpoints")
public class HealthProbeController {

    private final HealthEndpoint healthEndpoint;

    public HealthProbeController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @Operation(summary = "Liveness probe", description = "Indicates whether the application is running. "
            + "Used by Kubernetes to determine if the container should be restarted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application is alive", content = @Content(schema = @Schema(implementation = Map.class), examples = @ExampleObject(value = "{\"status\": \"ok\"}"))),
            @ApiResponse(responseCode = "503", description = "Application is not healthy", content = @Content(schema = @Schema(implementation = Map.class), examples = @ExampleObject(value = "{\"status\": \"down\"}")))})
    @GetMapping(value = "/liveness", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> liveness() {
        HealthDescriptor descriptor = healthEndpoint.healthForPath("liveness");
        return buildResponse(descriptor);
    }

    @Operation(summary = "Readiness probe", description = "Indicates whether the application is ready to accept traffic. "
            + "Used by Kubernetes to determine if traffic should be routed to this pod.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application is ready", content = @Content(schema = @Schema(implementation = Map.class), examples = @ExampleObject(value = "{\"status\": \"ok\"}"))),
            @ApiResponse(responseCode = "503", description = "Application is not ready", content = @Content(schema = @Schema(implementation = Map.class), examples = @ExampleObject(value = "{\"status\": \"down\"}")))})
    @GetMapping(value = "/readiness", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> readiness() {
        HealthDescriptor descriptor = healthEndpoint.healthForPath("readiness");
        return buildResponse(descriptor);
    }

    private ResponseEntity<Map<String, String>> buildResponse(HealthDescriptor descriptor) {
        if (descriptor != null && Status.UP.equals(descriptor.getStatus())) {
            return ResponseEntity.ok(Map.of("status", "ok"));
        }
        String status = descriptor != null ? descriptor.getStatus().getCode() : "unknown";
        return ResponseEntity.status(503).body(Map.of("status", status));
    }
}
