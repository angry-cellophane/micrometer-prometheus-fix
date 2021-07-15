package com.github.ka.micrometer.prometheus.fix.fix;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.MultiTagPrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.prometheus.client.CollectorRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureBefore(CompositeMeterRegistryAutoConfiguration.class)
public class MetricsConfiguration {

    @Bean
    public MultiTagPrometheusMeterRegistry prometheusMeterRegistry(PrometheusConfig config,
                                                                   CollectorRegistry collectorRegistry, Clock clock) {
        return new MultiTagPrometheusMeterRegistry(config, collectorRegistry, clock);
    }
}
