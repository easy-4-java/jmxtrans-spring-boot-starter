package org.jmxtrans.embedded.output.influxdb;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit tests for {@link InfluxMetric}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
class InfluxMetricTest {

    private static final long TS = 1700000000000L;

    @Test
    void constructor_should_reject_null_measurement() {
        assertThatNullPointerException()
                .isThrownBy(() -> new InfluxMetric(null, Collections.emptyList(), 1, TS));
    }

    @Test
    void constructor_should_reject_null_tags() {
        assertThatNullPointerException()
                .isThrownBy(() -> new InfluxMetric("m", null, 1, TS));
    }

    @Test
    void constructor_should_reject_null_value() {
        assertThatNullPointerException()
                .isThrownBy(() -> new InfluxMetric("m", Collections.emptyList(), null, TS));
    }

    @Test
    void getters_should_return_constructor_values() {
        List<InfluxTag> tags = Collections.singletonList(new InfluxTag("h", "host1"));
        InfluxMetric metric = new InfluxMetric("cpu", tags, 42L, TS);
        assertThat(metric.getMeasurement()).isEqualTo("cpu");
        assertThat(metric.getTags()).isEqualTo(tags);
        assertThat(metric.getTimestampMillis()).isEqualTo(TS);
        assertThat(metric.getValue()).isEqualTo("42i");
    }

    @Test
    void toInfluxFormat_should_render_integer_value() {
        InfluxMetric metric = new InfluxMetric("cpu", Collections.emptyList(), 5, TS);
        assertThat(metric.toInfluxFormat()).isEqualTo("cpu value=5i " + TS);
    }

    @Test
    void toInfluxFormat_should_render_long_value() {
        InfluxMetric metric = new InfluxMetric("cpu", Collections.emptyList(), 5L, TS);
        assertThat(metric.toInfluxFormat()).isEqualTo("cpu value=5i " + TS);
    }

    @Test
    void toInfluxFormat_should_render_double_value() {
        InfluxMetric metric = new InfluxMetric("cpu", Collections.emptyList(), 1.5d, TS);
        assertThat(metric.toInfluxFormat()).isEqualTo("cpu value=1.5 " + TS);
    }

    @Test
    void toInfluxFormat_should_render_float_value() {
        InfluxMetric metric = new InfluxMetric("cpu", Collections.emptyList(), 2.25f, TS);
        assertThat(metric.toInfluxFormat()).isEqualTo("cpu value=2.25 " + TS);
    }

    @Test
    void toInfluxFormat_should_render_bigdecimal_value() {
        InfluxMetric metric = new InfluxMetric("cpu", Collections.emptyList(), new BigDecimal("3.14"), TS);
        assertThat(metric.toInfluxFormat()).isEqualTo("cpu value=3.14 " + TS);
    }

    @Test
    void toInfluxFormat_should_render_string_value() {
        InfluxMetric metric = new InfluxMetric("cpu", Collections.emptyList(), "abc", TS);
        assertThat(metric.toInfluxFormat()).isEqualTo("cpu value=abc " + TS);
    }

    @Test
    void toInfluxFormat_should_render_with_tags() {
        List<InfluxTag> tags = Arrays.asList(new InfluxTag("host", "h1"), new InfluxTag("region", "r1"));
        InfluxMetric metric = new InfluxMetric("cpu", tags, 7, TS);
        assertThat(metric.toInfluxFormat()).isEqualTo("cpu,host=h1,region=r1 value=7i " + TS);
    }

    @Test
    void equals_hashCode_toString_should_behave_consistently() {
        List<InfluxTag> tags = Collections.singletonList(new InfluxTag("k", "v"));
        InfluxMetric a = new InfluxMetric("m", tags, 1, TS);
        InfluxMetric b = new InfluxMetric("m", tags, 1, TS);
        InfluxMetric otherMeasurement = new InfluxMetric("x", tags, 1, TS);
        InfluxMetric otherValue = new InfluxMetric("m", tags, 2, TS);
        InfluxMetric otherTs = new InfluxMetric("m", tags, 1, TS + 1);

        assertThat(a).isEqualTo(a);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(otherMeasurement);
        assertThat(a).isNotEqualTo(otherValue);
        assertThat(a).isNotEqualTo(otherTs);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("m");
        assertThat(a.toString()).contains("InfluxMetric");
    }

}
