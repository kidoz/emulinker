package org.emulinker.kaillera.admin;

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
 */
@RestController
@RequestMapping("/healthz")
public class HealthProbeController {

    private final HealthEndpoint healthEndpoint;

    public HealthProbeController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping(value = "/liveness", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> liveness() {
        HealthDescriptor descriptor = healthEndpoint.healthForPath("liveness");
        return buildResponse(descriptor);
    }

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
