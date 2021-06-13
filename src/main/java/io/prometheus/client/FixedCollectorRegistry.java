package io.prometheus.client;

import io.micrometer.prometheus.FixedMicrometerCollector;

import java.util.*;
import java.util.Enumeration;

public class FixedCollectorRegistry extends CollectorRegistry {

    private final Object namesCollectorsLock = new Object();
    private final Map<Collector, List<String>> collectorsToNames = new HashMap<>();
    private final Map<String, Map<List<String>, Collector>> namesToCollectors = new HashMap<>();

    private final boolean autoDescribe;

    public FixedCollectorRegistry() {
        this(false);
    }

    public FixedCollectorRegistry(boolean autoDescribe) {
        this.autoDescribe = autoDescribe;
    }

    /**
     * Register a Collector.
     * <p>
     * A collector can be registered to multiple CollectorRegistries.
     */
    @Override
    public void register(Collector m) {
        var names = collectorNames(m);
        var tags = tagsOf(m);
        synchronized (namesCollectorsLock) {
            for (String name : names) {
                var byTag = namesToCollectors.getOrDefault(name, Collections.emptyMap());
                if (byTag.containsKey(tags)) {
                    throw new IllegalArgumentException("Collector already registered that provides name: " + name);
                }
            }
            for (String name : names) {
                var byTag = namesToCollectors.computeIfAbsent(name, n -> new HashMap<>());
                byTag.put(tags, m);
            }
            collectorsToNames.put(m, names);
        }
    }

    /**
     * Unregister a Collector.
     */
    @Override
    public void unregister(Collector m) {
        var tags = tagsOf(m);
        synchronized (namesCollectorsLock) {
            List<String> names = collectorsToNames.remove(m);
            for (String name : names) {
                var byTags = namesToCollectors.getOrDefault(name, Collections.emptyMap());
                byTags.remove(tags);
                if (byTags.isEmpty()) {
                    namesToCollectors.remove(name);
                }
            }
        }
    }

    /**
     * Unregister all Collectors.
     */
    @Override
    public void clear() {
        synchronized (namesCollectorsLock) {
            collectorsToNames.clear();
            namesToCollectors.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> tagsOf(Collector m) {
        if (m == null) return Collections.emptyList();

        if (m instanceof FixedMicrometerCollector) {
            return ((FixedMicrometerCollector)m).getTagKeys();
        }
        if (m instanceof SimpleCollector) {
            return ((SimpleCollector)m).labelNames;
        }

        return Collections.emptyList();
    }

    private List<String> collectorNames(Collector m) {
        List<Collector.MetricFamilySamples> mfs;
        if (m instanceof Collector.Describable) {
            mfs = ((Collector.Describable) m).describe();
        } else if (autoDescribe) {
            mfs = m.collect();
        } else {
            mfs = Collections.emptyList();
        }

        List<String> names = new ArrayList<String>();
        for (Collector.MetricFamilySamples family : mfs) {
            switch (family.type) {
                case COUNTER:
                    names.add(family.name + "_total");
                    names.add(family.name + "_created");
                    names.add(family.name);
                    break;
                case SUMMARY:
                    names.add(family.name + "_count");
                    names.add(family.name + "_sum");
                    names.add(family.name + "_created");
                    names.add(family.name);
                    break;
                case HISTOGRAM:
                    names.add(family.name + "_count");
                    names.add(family.name + "_sum");
                    names.add(family.name + "_bucket");
                    names.add(family.name + "_created");
                    names.add(family.name);
                    break;
                case GAUGE_HISTOGRAM:
                    names.add(family.name + "_gcount");
                    names.add(family.name + "_gsum");
                    names.add(family.name + "_bucket");
                    names.add(family.name);
                    break;
                case INFO:
                    names.add(family.name + "_info");
                    names.add(family.name);
                    break;
                default:
                    names.add(family.name);
            }
        }
        return names;
    }

    /**
     * Enumeration of metrics of all registered collectors.
     */
    public java.util.Enumeration<Collector.MetricFamilySamples> metricFamilySamples() {
        return new FixedMetricFamilySamplesEnumeration();
    }

    /**
     * Enumeration of metrics matching the specified names.
     * <p>
     * Note that the provided set of names will be matched against the time series
     * name and not the metric name. For instance, to retrieve all samples from a
     * histogram, you must include the '_count', '_sum' and '_bucket' names.
     */
    public java.util.Enumeration<Collector.MetricFamilySamples> filteredMetricFamilySamples(Set<String> includedNames) {
        return new FixedMetricFamilySamplesEnumeration(includedNames);
    }

    private Set<Collector> collectors() {
        synchronized (namesCollectorsLock) {
            return new HashSet<>(collectorsToNames.keySet());
        }
    }

    class FixedMetricFamilySamplesEnumeration extends MetricFamilySamplesEnumeration implements Enumeration<Collector.MetricFamilySamples> {

        private final Iterator<Collector> collectorIter;
        private Iterator<Collector.MetricFamilySamples> metricFamilySamples;
        private Collector.MetricFamilySamples next;
        private Set<String> includedNames;

        FixedMetricFamilySamplesEnumeration(Set<String> includedNames) {
            this.includedNames = includedNames;
            collectorIter = includedCollectorIterator(includedNames);
            findNextElement();
        }

        private Iterator<Collector> includedCollectorIterator(Set<String> includedNames) {
            if (includedNames.isEmpty()) {
                return collectors().iterator();
            } else {
                HashSet<Collector> collectors = new HashSet<Collector>();
                synchronized (namesCollectorsLock) {
                    for (Map.Entry<String, Map<List<String>, Collector>> entry : namesToCollectors.entrySet()) {
                        if (includedNames.contains(entry.getKey())) {
                            collectors.addAll(entry.getValue().values());
                        }
                    }
                }

                return collectors.iterator();
            }
        }

        FixedMetricFamilySamplesEnumeration() {
            this(Collections.emptySet());
        }

        private void findNextElement() {
            next = null;

            while (metricFamilySamples != null && metricFamilySamples.hasNext()) {
                next = filter(metricFamilySamples.next());
                if (next != null) {
                    return;
                }
            }

            if (next == null) {
                while (collectorIter.hasNext()) {
                    metricFamilySamples = collectorIter.next().collect().iterator();
                    while (metricFamilySamples.hasNext()) {
                        next = filter(metricFamilySamples.next());
                        if (next != null) {
                            return;
                        }
                    }
                }
            }
        }

        private Collector.MetricFamilySamples filter(Collector.MetricFamilySamples next) {
            if (includedNames.isEmpty()) {
                return next;
            } else {
                Iterator<Collector.MetricFamilySamples.Sample> it = next.samples.iterator();
                while (it.hasNext()) {
                    if (!includedNames.contains(it.next().name)) {
                        it.remove();
                    }
                }
                if (next.samples.size() == 0) {
                    return null;
                }
                return next;
            }
        }

        public Collector.MetricFamilySamples nextElement() {
            Collector.MetricFamilySamples current = next;
            if (current == null) {
                throw new NoSuchElementException();
            }
            findNextElement();
            return current;
        }

        public boolean hasMoreElements() {
            return next != null;
        }
    }
}
