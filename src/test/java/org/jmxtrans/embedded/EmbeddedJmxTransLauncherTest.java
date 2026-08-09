package org.jmxtrans.embedded;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EmbeddedJmxTransLauncher}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
class EmbeddedJmxTransLauncherTest {

    @Test
    void setter_and_getter_should_round_trip_jmxtrans() {
        EmbeddedJmxTransLauncher launcher = new EmbeddedJmxTransLauncher();
        EmbeddedJmxTrans jmxtrans = Mockito.mock(EmbeddedJmxTrans.class);
        launcher.setJmxtrans(jmxtrans);
        assertThat(launcher.getJmxtrans()).isSameAs(jmxtrans);
    }

    @Test
    void getJmxtrans_should_return_null_when_not_set() {
        EmbeddedJmxTransLauncher launcher = new EmbeddedJmxTransLauncher();
        assertThat(launcher.getJmxtrans()).isNull();
    }

}
