package org.jmxtrans.embedded.output.influxdb;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit tests for {@link InfluxTag}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
class InfluxTagTest {

    @Test
    void should_reject_null_name() {
        assertThatNullPointerException().isThrownBy(() -> new InfluxTag(null, "value"));
    }

    @Test
    void should_reject_null_value() {
        assertThatNullPointerException().isThrownBy(() -> new InfluxTag("name", null));
    }

    @Test
    void should_expose_name_and_value() {
        InfluxTag tag = new InfluxTag("host", "127.0.0.1");
        assertThat(tag.getName()).isEqualTo("host");
        assertThat(tag.getValue()).isEqualTo("127.0.0.1");
    }

    @Test
    void toInfluxFormat_should_concatenate_with_equals() {
        assertThat(new InfluxTag("region", "us-west").toInfluxFormat()).isEqualTo("region=us-west");
    }

    @Test
    void toString_should_match_influx_format() {
        InfluxTag tag = new InfluxTag("env", "prod");
        assertThat(tag.toString()).isEqualTo("env=prod");
    }

    @Test
    void equals_and_hashCode_should_be_consistent() {
        InfluxTag a = new InfluxTag("k", "v");
        InfluxTag b = new InfluxTag("k", "v");
        InfluxTag c = new InfluxTag("k", "x");
        InfluxTag d = new InfluxTag("y", "v");

        assertThat(a).isEqualTo(a);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo(d);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("k=v");
    }

}
