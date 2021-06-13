### what's this?

An attempt to solve this issue https://github.com/micrometer-metrics/micrometer/issues/877


### How does it work?

Changes in `MicrometerCollector` to make label keys dynamic.


### What's the prometheus output looks like after the change?

```
# HELP counter_total  
# TYPE counter_total counter
counter_total{type="success",version="1",} 1.0
counter_total{code="500",type="error",version="1",} 1.0
```
