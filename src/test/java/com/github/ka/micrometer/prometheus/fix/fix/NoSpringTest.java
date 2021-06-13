package com.github.ka.micrometer.prometheus.fix.fix;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.prometheus.client.FixedCollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

public class NoSpringTest {

    @Test
    void test() throws IOException {
        var prometheusRegistry = new FixedCollectorRegistry();
        var registry = new MetricsConfiguration()
                .prometheusMeterRegistry(PrometheusConfig.DEFAULT, prometheusRegistry, Clock.SYSTEM);
        registry.counter("counter", "version", "1", "type", "success").increment();
        registry.counter("counter", "version", "1", "type", "error", "code", "500").increment();

        var writer = new StringWriter();
        TextFormat.write004(writer, registry.getPrometheusRegistry().metricFamilySamples());

        System.out.println(writer.toString());
    }
}
