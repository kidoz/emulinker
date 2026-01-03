package su.kidoz.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Game configuration properties.
 *
 * <p>
 * Maps to properties with prefix "game." in application.properties.
 */
@ConfigurationProperties(prefix = "game")
@Validated
public class GameConfig {

    @Min(1)
    private int bufferSize = 1024;

    @Min(1)
    private int timeoutMillis = 1250;

    @Min(0)
    private int desynchTimeouts = 4;

    @Min(0)
    @Max(5)
    private int defaultAutoFireSensitivity = 0;

    // Getters and setters

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public int getDesynchTimeouts() {
        return desynchTimeouts;
    }

    public void setDesynchTimeouts(int desynchTimeouts) {
        this.desynchTimeouts = desynchTimeouts;
    }

    public int getDefaultAutoFireSensitivity() {
        return defaultAutoFireSensitivity;
    }

    public void setDefaultAutoFireSensitivity(int defaultAutoFireSensitivity) {
        this.defaultAutoFireSensitivity = defaultAutoFireSensitivity;
    }
}
