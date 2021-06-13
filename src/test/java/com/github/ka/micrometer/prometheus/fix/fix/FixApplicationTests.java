package com.github.ka.micrometer.prometheus.fix.fix;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {MetricsConfiguration.class,
		MetricsAutoConfiguration.class,
		PrometheusMetricsExportAutoConfiguration.class})
class FixApplicationTests {

	@Autowired
	PrometheusMeterRegistry registry;

//	@Test
	void contextLoads() {
		System.out.println(registry);
	}

}
