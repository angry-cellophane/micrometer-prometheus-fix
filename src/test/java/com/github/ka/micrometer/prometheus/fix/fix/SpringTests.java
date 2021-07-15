package com.github.ka.micrometer.prometheus.fix.fix;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.MultiTagPrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
		properties = "management.metrics.export.prometheus.enabled=true")
class SpringTests {

	@Autowired
	MeterRegistry registry;

	@AfterEach
	void cleanup() {
		if (registry != null) registry.clear();
	}

	@Test
	@DisplayName("can create two counters with the same name but different tag keys without error")
	void canCreateTwoCountersWithDifferentTags() {
		var counter1 = registry.counter("counter", "tag1", "val1", "tag2", "val2");
		var counter2 = registry.counter("counter", "tag2", "val2", "tag3", "val3");

		assertNotNull(counter1);
		assertNotNull(counter2);
	}

	@Test
	@DisplayName("update counters with different tag keys")
	void updateCounters() {
		var counter1 = registry.counter("counter", "tag1", "val1", "tag2", "val2");
		var counter2 = registry.counter("counter", "tag2", "val2", "tag3", "val3");

		counter1.increment();
		counter2.increment();
		counter2.increment();

		assertEquals(1, counter1.count());
		assertEquals(2, counter2.count());
	}

	@Test
	@DisplayName("both counters in prometheus metrics")
	void countersInPrometheusMetrics() throws IOException {
		var counter1 = registry.counter("counter", "tag1", "val1", "tag2", "val2");
		var counter2 = registry.counter("counter", "tag2", "val2", "tag3", "val3");

		counter1.increment();
		counter2.increment();

		assertTrue(registry.getClass() == MultiTagPrometheusMeterRegistry.class);

		var sw = new StringWriter();
		var promRegistry = (MultiTagPrometheusMeterRegistry) registry;
		promRegistry.scrape(sw);
		String output = sw.toString();

		assertTrue(output.contains("counter_total{tag1=\"val1\",tag2=\"val2\",} 1.0"));
		assertTrue(output.contains("counter_total{tag2=\"val2\",tag3=\"val3\",} 1.0"));
	}
}
