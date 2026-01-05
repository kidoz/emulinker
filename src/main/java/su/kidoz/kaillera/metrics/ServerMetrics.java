package su.kidoz.kaillera.metrics;

import su.kidoz.kaillera.master.StatsCollector;

/**
 * Groups metrics and observability collectors.
 *
 * <p>
 * This record bundles components responsible for collecting server statistics
 * and metrics for monitoring and observability purposes.
 *
 * @param statsCollector
 *            collects statistics for master server list reporting
 * @param gameMetricsCollector
 *            collects game-level metrics for Prometheus/Micrometer
 */
public record ServerMetrics(StatsCollector statsCollector,
        GameMetricsCollector gameMetricsCollector) {
}
