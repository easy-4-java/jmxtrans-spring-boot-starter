package org.jmxtrans.embedded.util.time;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link SystemClock}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
class SystemClockTest {

    @Test
    void now_should_return_value_close_to_system_current_time() {
        long before = System.currentTimeMillis();
        long clock = SystemClock.now();
        long after = System.currentTimeMillis();
        // SystemClock caches the value and refreshes every 1ms, so the cached value
        // may lag the freshly-read wall clock by a few millis. Be tolerant.
        assertThat(clock).isBetween(before - 5000L, after + 5000L);
    }

    @Test
    void now_should_be_monotonic_non_decreasing_across_calls() {
        long a = SystemClock.now();
        long b = SystemClock.now();
        assertThat(b).isGreaterThanOrEqualTo(a);
    }

    @Test
    void nowDate_should_return_non_null_string_close_to_now() {
        String date = SystemClock.nowDate();
        assertThat(date).isNotNull().isNotEmpty();
        // java.sql.Timestamp string starts with the date portion "yyyy-mm-dd"
        assertThat(date).matches("\\d{4}-\\d{2}-\\d{2}.*");
    }

    @Test
    void now_should_stay_reasonably_close_to_real_clock() {
        long clock = SystemClock.now();
        long real = System.currentTimeMillis();
        // SystemClock updates every 1ms, so this should be within a small window
        assertThat(Math.abs(real - clock)).isCloseTo(0L, within(5000L));
    }

}
