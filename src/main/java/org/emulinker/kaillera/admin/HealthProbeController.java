package org.emulinker.kaillera.admin;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class HealthProbeController {

    private final HealthEndpoint healthEndpoint;

    @Autowired
    public HealthProbeController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping({"/livez", "/healthz"})
    public ResponseEntity<String> liveness() {
        HealthComponent component = healthEndpoint.healthForPath("liveness");
        if (component != null && component.getStatus().equals(Status.UP)) {
            return ResponseEntity.ok("OK");
        }
        return ResponseEntity.status(503).body("Service Unavailable");
    }

    @GetMapping("/readyz")
    public ResponseEntity<String> readiness() {
        HealthComponent component = healthEndpoint.healthForPath("readiness");
        if (component != null && component.getStatus().equals(Status.UP)) {
            return ResponseEntity.ok("OK");
        }
        return ResponseEntity.status(503).body("Service Unavailable");
    }
}
