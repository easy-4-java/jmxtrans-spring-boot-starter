package org.jmxtrans.embedded.output.influxdb;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jmxtrans.embedded.ResultNameStrategy;
import org.jmxtrans.embedded.output.influxdb.InfluxMetricConverter.FailedToConvertToInfluxMetricException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link InfluxMetricConverter}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
class InfluxMetricConverterTest {

    private final ResultNameStrategy strategy = new ResultNameStrategy();

    @Test
    void constructor_should_be_accessible() {
        // exercise the implicit default constructor for coverage
        InfluxMetricConverter instance = new InfluxMetricConverter();
        assertThat(instance).isNotNull();
    }

    @Test
    void tagsFromCommaSeparatedString_should_return_empty_list_for_blank_input() {
        assertThat(InfluxMetricConverter.tagsFromCommaSeparatedString(strategy, "")).isEmpty();
        assertThat(InfluxMetricConverter.tagsFromCommaSeparatedString(strategy, "   ")).isEmpty();
    }

    @Test
    void tagsFromCommaSeparatedString_should_parse_single_tag() {
        List<InfluxTag> tags = InfluxMetricConverter.tagsFromCommaSeparatedString(strategy, "host=127.0.0.1");
        assertThat(tags).containsExactly(new InfluxTag("host", "127.0.0.1"));
    }

    @Test
    void tagsFromCommaSeparatedString_should_parse_multiple_tags() {
        List<InfluxTag> tags = InfluxMetricConverter.tagsFromCommaSeparatedString(strategy, "host=h1, region = r1 ");
        assertThat(tags).containsExactly(new InfluxTag("host", "h1"), new InfluxTag("region", "r1"));
    }

    @Test
    void tagsFromCommaSeparatedString_should_resolve_env_value() {
        // PATH is set on every reasonable environment
        List<InfluxTag> tags = InfluxMetricConverter.tagsFromCommaSeparatedString(strategy, "env=PATH");
        assertThat(tags.get(0).getName()).isEqualTo("env");
        assertThat(tags.get(0).getValue()).isEqualTo(System.getenv("PATH"));
    }

    @Test
    void tagsFromCommaSeparatedString_should_resolve_system_property_value() {
        System.setProperty("conv.test.prop", "conv-value");
        try {
            List<InfluxTag> tags = InfluxMetricConverter.tagsFromCommaSeparatedString(strategy, "p=conv.test.prop");
            assertThat(tags).containsExactly(new InfluxTag("p", "conv-value"));
        } finally {
            System.clearProperty("conv.test.prop");
        }
    }

    @Test
    void tagsFromCommaSeparatedString_should_throw_when_tag_format_invalid() {
        assertThatThrownBy(() -> InfluxMetricConverter.tagsFromCommaSeparatedString(strategy, "noequalshere"))
                .isInstanceOf(FailedToConvertToInfluxMetricException.class)
                .hasMessageContaining("must be on format");
    }

    @Test
    void convertToInfluxMetric_should_combine_additional_tags_and_parsed_tags() {
        List<InfluxTag> additional = Collections.singletonList(new InfluxTag("app", "demo"));
        InfluxMetric metric = InfluxMetricConverter.convertToInfluxMetric(
                strategy, "cpu.load,host=server1", 3, additional, 1L);
        assertThat(metric.getMeasurement()).isEqualTo("cpu.load");
        assertThat(metric.getValue()).isEqualTo("3i");
        assertThat(metric.getTags()).containsExactly(
                new InfluxTag("app", "demo"),
                new InfluxTag("host", "server1"));
    }

    @Test
    void convertToInfluxMetric_should_handle_name_without_tags() {
        InfluxMetric metric = InfluxMetricConverter.convertToInfluxMetric(
                strategy, "mem", 9, Collections.emptyList(), 2L);
        assertThat(metric.getMeasurement()).isEqualTo("mem");
        assertThat(metric.getTags()).isEmpty();
    }

    @Test
    void convertToInfluxMetric_should_handle_multiple_parsed_tags() {
        InfluxMetric metric = InfluxMetricConverter.convertToInfluxMetric(
                strategy, "m,a=1,b=2", 0, Collections.emptyList(), 0L);
        assertThat(metric.getTags()).containsExactly(new InfluxTag("a", "1"), new InfluxTag("b", "2"));
    }

    @Test
    void convertToInfluxMetric_should_trim_measurement() {
        InfluxMetric metric = InfluxMetricConverter.convertToInfluxMetric(
                strategy, "  m  ,a=1", 0, Collections.emptyList(), 0L);
        assertThat(metric.getMeasurement()).isEqualTo("m");
    }

    @Test
    void failedToConvertToInfluxMetricException_should_carry_message() {
        FailedToConvertToInfluxMetricException ex = new FailedToConvertToInfluxMetricException("boom");
        assertThat(ex).hasMessage("boom");
    }

    @Test
    void convertToInfluxMetric_should_keep_additional_tags_order_with_multiple() {
        List<InfluxTag> additional = Arrays.asList(new InfluxTag("a", "1"), new InfluxTag("b", "2"));
        InfluxMetric metric = InfluxMetricConverter.convertToInfluxMetric(
                strategy, "metric,c=3,d=4", 1, additional, 10L);
        assertThat(metric.getTags()).containsExactly(
                new InfluxTag("a", "1"),
                new InfluxTag("b", "2"),
                new InfluxTag("c", "3"),
                new InfluxTag("d", "4"));
    }

}
