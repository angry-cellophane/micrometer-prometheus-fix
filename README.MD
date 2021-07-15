### what's this?

An attempt to solve this issue https://github.com/micrometer-metrics/micrometer/issues/877


### How does it work?

Changes in `MicrometerCollector` to make label keys dynamic.
Check this PR for more details https://github.com/micrometer-metrics/micrometer/pull/2653


### What's the prometheus output looks like after the change?

```
# HELP counter_total  
# TYPE counter_total counter
counter_total{type="success",version="1",} 1.0
counter_total{code="500",type="error",version="1",} 1.0
```

### Is it safe to use?
Yes, absolutely. No metric gets dropped quietly, output format is valid and parsable by prometheus.


### What if I want to use it now?

Copy the classes under the [`io.micrometer.prometheus`](./src/main/java/io/micrometer/prometheus) package into your project.

If you use spring there's an example of a configuration in [`com.github.ka.micrometer.prometheus.fix.fix.MetricsConfiguration`](./src/main/java/com/github/ka/micrometer/prometheus/fix/fix/MetricsConfiguration.java)
