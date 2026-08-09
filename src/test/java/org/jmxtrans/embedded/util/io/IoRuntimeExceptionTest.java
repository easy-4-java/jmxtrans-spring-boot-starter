package org.jmxtrans.embedded.util.io;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link IoRuntimeException} and {@link FileNotFoundRuntimeException}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
class IoRuntimeExceptionTest {

    @Test
    void propagate_should_return_file_not_found_runtime_for_FileNotFoundException() {
        IoRuntimeException ex = IoRuntimeException.propagate(new FileNotFoundException("/nope"));
        assertThat(ex).isInstanceOf(FileNotFoundRuntimeException.class);
        assertThat(ex.getCause()).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void propagate_should_return_io_runtime_for_generic_IOException() {
        IoRuntimeException ex = IoRuntimeException.propagate(new IOException("boom"));
        assertThat(ex).isInstanceOf(IoRuntimeException.class)
                .isNotInstanceOf(FileNotFoundRuntimeException.class);
        assertThat(ex.getCause()).isInstanceOf(IOException.class);
    }

    @Test
    void default_constructor_should_work() {
        assertThat(new IoRuntimeException().getMessage()).isNull();
        assertThat(new FileNotFoundRuntimeException().getMessage()).isNull();
    }

    @Test
    void message_constructor_should_work() {
        assertThat(new IoRuntimeException("msg")).hasMessage("msg");
        assertThat(new FileNotFoundRuntimeException("msg")).hasMessage("msg");
    }

    @Test
    void cause_constructor_should_work() {
        Throwable cause = new Exception("c");
        assertThat(new IoRuntimeException(cause)).hasCause(cause);
        assertThat(new FileNotFoundRuntimeException(cause)).hasCause(cause);
    }

    @Test
    void message_and_cause_constructor_should_work() {
        Throwable cause = new Exception("c");
        assertThat(new IoRuntimeException("m", cause)).hasMessage("m").hasCause(cause);
        assertThat(new FileNotFoundRuntimeException("m", cause)).hasMessage("m").hasCause(cause);
    }

    @Test
    void full_constructor_should_work() {
        Throwable cause = new Exception("c");
        IoRuntimeException io = new IoRuntimeException("m", cause, true, false);
        assertThat(io).hasMessage("m").hasCause(cause);

        FileNotFoundRuntimeException fnf = new FileNotFoundRuntimeException("m", cause, true, false);
        assertThat(fnf).hasMessage("m").hasCause(cause);
    }

}
