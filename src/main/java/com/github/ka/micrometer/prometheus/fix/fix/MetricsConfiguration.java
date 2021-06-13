package com.github.ka.micrometer.prometheus.fix.fix;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.MultiTagPrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfiguration {

    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry(PrometheusConfig config,
                                                           CollectorRegistry collectorRegistry, Clock clock) {
        return new MultiTagPrometheusMeterRegistry(config, collectorRegistry, clock);
    }
}
