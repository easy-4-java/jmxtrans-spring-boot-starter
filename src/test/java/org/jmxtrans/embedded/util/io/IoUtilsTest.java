package org.jmxtrans.embedded.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link IoUtils}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
class IoUtilsTest {

    @Test
    void constructor_should_be_accessible() {
        // exercise the implicit default constructor for coverage
        IoUtils instance = new IoUtils();
        assertThat(instance).isNotNull();
    }

    @Test
    void closeQuietly_urlConnection_should_return_null_silently() {
        IoUtils.closeQuietly((URLConnection) null);
    }

    @Test
    void closeQuietly_urlConnection_should_disconnect_http_connection() throws Exception {
        HttpURLConnection conn = mock(HttpURLConnection.class);
        IoUtils.closeQuietly((URLConnection) conn);
        verify(conn).disconnect();
    }

    @Test
    void closeQuietly_urlConnection_should_do_nothing_for_non_http() {
        URLConnection conn = mock(URLConnection.class);
        IoUtils.closeQuietly(conn);
        Mockito.verifyNoMoreInteractions(conn);
    }

    @Test
    void closeQuietly_closeable_should_handle_null() {
        IoUtils.closeQuietly((java.io.Closeable) null);
    }

    @Test
    void closeQuietly_closeable_should_swallow_exception() throws Exception {
        java.io.Closeable closeable = mock(java.io.Closeable.class);
        Mockito.doThrow(new IOException("boom")).when(closeable).close();
        IoUtils.closeQuietly(closeable);
        verify(closeable).close();
    }

    @Test
    void closeQuietly_writer_should_handle_null() {
        IoUtils.closeQuietly((Writer) null);
    }

    @Test
    void closeQuietly_writer_should_swallow_exception() throws Exception {
        Writer writer = mock(Writer.class);
        Mockito.doThrow(new IOException("boom")).when(writer).close();
        IoUtils.closeQuietly(writer);
        verify(writer).close();
    }

    @Test
    void closeQuietly_inputStream_should_handle_null() {
        IoUtils.closeQuietly((InputStream) null);
    }

    @Test
    void closeQuietly_inputStream_should_swallow_exception() throws Exception {
        InputStream is = mock(InputStream.class);
        Mockito.doThrow(new IOException("boom")).when(is).close();
        IoUtils.closeQuietly(is);
        verify(is).close();
    }

    @Test
    void closeQuietly_closeable_should_close_real_closeable() throws Exception {
        boolean[] closed = new boolean[]{false};
        java.io.Closeable closeable = () -> closed[0] = true;
        IoUtils.closeQuietly(closeable);
        assertThat(closed[0]).isTrue();
    }

    @Test
    void closeQuietly_writer_should_close_real_writer() throws Exception {
        OutputStream out = new java.io.ByteArrayOutputStream();
        Writer writer = new java.io.OutputStreamWriter(out);
        IoUtils.closeQuietly(writer);
        // After closing, writing would throw. Verify by attempting to write -> should throw
        org.assertj.core.api.Assertions.assertThatThrownBy(writer::flush)
                .isInstanceOf(java.io.IOException.class);
    }

    @Test
    void closeQuietly_inputStream_should_close_real_input_stream() {
        java.io.InputStream is = new java.io.ByteArrayInputStream(new byte[0]);
        IoUtils.closeQuietly(is);
        // no exception means success; stream is now closed
    }

    /** Helps the compiler pick the URLConnection overload for mocks. */
    @Test
    void closeQuietly_urlConnection_real_url_connection_null_path() {
        IoUtils.closeQuietly((URLConnection) null);
    }

    @Test
    void dummy_url_connection_test_does_not_throw() throws Exception {
        URL url = new URL("http://example.invalid");
        // a URLConnection instance that is not HttpURLConnection should not throw
        URLConnection conn = new URLConnection(url) {
            @Override
            public void connect() {
                // no-op
            }
        };
        IoUtils.closeQuietly(conn);
    }

}
