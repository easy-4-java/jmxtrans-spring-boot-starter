package org.jmxtrans.embedded.util.tag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TagUtil}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
class TagUtilTest {

    @Test
    void getTagValFromEnv_should_return_env_value_for_path() {
        // PATH is available on any reasonable test environment
        assertThat(TagUtil.getTagValFromEnv("PATH")).isEqualTo(System.getenv("PATH"));
    }

    @Test
    void getTagValFromEnv_should_return_null_for_unknown_env() {
        assertThat(TagUtil.getTagValFromEnv("___NOPE_NOT_AN_ENV_VAR___")).isNull();
    }

    @Test
    void getTagValFromEnv_should_return_system_property_when_set() {
        System.setProperty("tagutil.test.prop", "abc");
        try {
            assertThat(TagUtil.getTagValFromEnv("tagutil.test.prop")).isEqualTo("abc");
        } finally {
            System.clearProperty("tagutil.test.prop");
        }
    }

}
