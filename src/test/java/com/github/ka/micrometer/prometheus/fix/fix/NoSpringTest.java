package com.github.ka.micrometer.prometheus.fix.fix;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.MultiTagPrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NoSpringTest {

    @Test
    void updateCounterAndGetValue() {
        var expected = 1.0d;
        var registry = newRegistry();
        registry.counter("counter", "version", "1", "type", "success").increment();

        var actual = registry.counter("counter", "version", "1", "type", "success").count();
        assertEquals(expected, actual);
    }

    @Test
    void updateTwoCountersWithDifferentNames() {
        var registry = newRegistry();

        registry.counter("counter1", "version", "1").increment();
        registry.counter("counter2", "version", "1").increment();

        assertEquals(1.0, registry.counter("counter1", "version", "1").count());
        assertEquals(1.0, registry.counter("counter2", "version", "1").count());
    }

    @Test
    void updateCounterWithTheSameNameAndTags() {
        var registry = newRegistry();

        registry.counter("counter", "version", "1").increment();
        registry.counter("counter", "version", "1").increment();
        registry.counter("counter", "version", "1").increment();

        Assertions.assertEquals(1, new ArrayList<>(registry.get("counter").meters()).size());
        assertEquals(3.0, registry.counter("counter", "version", "1").count());
    }

    @Test
    void updateTwoCountersWithSameNameAndDifferentTags() {
        var registry = newRegistry();

        registry.counter("counter", "version", "1").increment();
        registry.counter("counter", "error", "401").increment();
        registry.counter("counter", "version", "1").increment();
        registry.counter("counter", "error", "500").increment();

        Assertions.assertEquals(3, new ArrayList<>(registry.get("counter").meters()).size());

        assertEquals(2.0, registry.counter("counter", "version", "1").count());
        assertEquals(1.0, registry.counter("counter", "error", "401").count());
        assertEquals(1.0, registry.counter("counter", "error", "500").count());
    }

    @Test
    void printPrometheusMetricsIn004FormatText() throws IOException {
        var registry = newRegistry();

        registry.counter("counter", "version", "1", "type", "success").increment();
        registry.counter("counter", "version", "1", "type", "error", "code", "500").increment();

        var writer = new StringWriter();
        TextFormat.write004(writer, registry.getPrometheusRegistry().metricFamilySamples());
        System.out.println(writer.toString());
    }

    static MultiTagPrometheusMeterRegistry newRegistry() {
        var prometheusRegistry = new CollectorRegistry();
        return new MultiTagPrometheusMeterRegistry(PrometheusConfig.DEFAULT, prometheusRegistry, Clock.SYSTEM);
    }
}
