/**
 * Copyright 2017 VMware, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.prometheus;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.NamingConvention;
import io.prometheus.client.Collector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * {@link Collector} for Micrometer.
 *
 * @author Jon Schneider
 * @author Johnny Lim
 */
public class MultiTagMicrometerCollector extends Collector implements Collector.Describable {

    interface Child {
        Stream<MicrometerCollector.Family> samples(String conventionName, TagsHolder tags);
    }

    static class TagsHolder {
        final List<String> keys;
        final List<String> values;

        public TagsHolder(List<String> keys, List<String> values) {
            this.keys = List.copyOf(Objects.requireNonNull(keys, "keys"));
            this.values = List.copyOf(Objects.requireNonNull(values, "values"));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TagsHolder that = (TagsHolder) o;
            return keys.equals(that.keys) && values.equals(that.values);
        }

        @Override
        public int hashCode() {
            return Objects.hash(keys, values);
        }
    }

    private final Meter.Id id;
    private final Map<TagsHolder, Child> children = new ConcurrentHashMap<>();
    private final String conventionName;
    private final String help;

    public MultiTagMicrometerCollector(Meter.Id id, NamingConvention convention, PrometheusConfig config) {
        this.id = id;
        this.conventionName = id.getConventionName(convention);
        this.help = config.descriptions() ? Optional.ofNullable(id.getDescription()).orElse(" ") : " ";
    }

    public void add(List<Tag> tags, Child child) {
        children.put(toHolder(tags), child);
    }

    public void remove(List<Tag> tags) {
        children.remove(toHolder(tags));
    }

    private TagsHolder toHolder(List<Tag> tags) {
        return new TagsHolder(
                tags.stream().map(Tag::getKey).collect(Collectors.toUnmodifiableList()),
                tags.stream().map(Tag::getValue).collect(Collectors.toUnmodifiableList())
        );
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    @Override
    public List<MetricFamilySamples> collect() {
        Map<String, MicrometerCollector.Family> families = new HashMap<>();

        for (Map.Entry<TagsHolder, Child> entry : children.entrySet()) {
            var tags = entry.getKey();
            var child = entry.getValue();

            child.samples(conventionName, tags).forEach(family -> {
                families.compute(family.getConventionName(), (name, matchingFamily) -> matchingFamily != null ?
                        matchingFamily.addSamples(family.samples) : family);
            });
        }

        return families.values().stream()
                .map(family -> new MetricFamilySamples(family.conventionName, family.type, help, family.samples))
                .collect(toList());
    }

    @Override
    public List<MetricFamilySamples> describe() {
        switch (id.getType()) {
            case COUNTER:
                return Collections.singletonList(
                        new MetricFamilySamples(conventionName, Type.COUNTER, help, Collections.emptyList()));

            case GAUGE:
                return Collections.singletonList(
                        new MetricFamilySamples(conventionName, Type.GAUGE, help, Collections.emptyList()));

            case TIMER:
            case DISTRIBUTION_SUMMARY:
                return Arrays.asList(
                        new MetricFamilySamples(conventionName, Type.HISTOGRAM, help, Collections.emptyList()),
                        new MetricFamilySamples(conventionName + "_max", Type.GAUGE, help, Collections.emptyList()));

            case LONG_TASK_TIMER:
                return Arrays.asList(
                        new MetricFamilySamples(conventionName, Type.HISTOGRAM, help, Collections.emptyList()),
                        new MetricFamilySamples(conventionName, Type.UNKNOWN, help, Collections.emptyList()));

            default:
                return Collections.singletonList(
                        new MetricFamilySamples(conventionName, Type.UNKNOWN, help, Collections.emptyList()));
        }
    }
}
