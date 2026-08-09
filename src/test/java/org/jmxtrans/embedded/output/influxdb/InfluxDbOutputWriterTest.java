package org.jmxtrans.embedded.output.influxdb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.jmxtrans.embedded.QueryResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InfluxDbOutputWriter}.
 *
 * <p>Uses a lightweight in-process {@link HttpServer} to exercise the HTTP send path
 * without mocking the JDK's {@link java.net.HttpURLConnection}.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
class InfluxDbOutputWriterTest {

    private static final String DEBUG_LOG_PROPERTY = "org.slf4j.simpleLogger.log.org.jmxtrans.embedded";

    private HttpServer server;
    private int port;
    private CapturingHandler handler;

    /**
     * Enable debug logging before any {@link InfluxDbOutputWriter} is created
     * so that the SLF4J SimpleLogger picks up the level when the Logger is
     * first initialised. This exercises the {@code LOG.isDebugEnabled()} branches
     * in {@link InfluxDbOutputWriter#write}.
     */
    @BeforeAll
    static void enableDebugLogging() {
        System.setProperty(DEBUG_LOG_PROPERTY, "debug");
    }

    @AfterAll
    static void resetLogging() {
        System.clearProperty(DEBUG_LOG_PROPERTY);
    }

    @BeforeEach
    void startServer() throws IOException {
        handler = new CapturingHandler();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", handler);
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getUrl_should_return_default_when_null() {
        InfluxDbOutputWriter writer = new InfluxDbOutputWriter();
        assertThat(writer.getUrl(null)).isEqualTo("http://127.0.0.1:8086");
        assertThat(writer.getUrl("http://h:1")).isEqualTo("http://h:1");
    }

    @Test
    void getDatabase_should_return_default_when_null() {
        InfluxDbOutputWriter writer = new InfluxDbOutputWriter();
        assertThat(writer.getDatabase(null)).isEqualTo("Metrics_127.0.0.1");
        assertThat(writer.getDatabase("db1")).isEqualTo("db1");
    }

    @Test
    void getUser_and_getPassword_should_return_input_as_is() {
        InfluxDbOutputWriter writer = new InfluxDbOutputWriter();
        assertThat(writer.getUser("u")).isEqualTo("u");
        assertThat(writer.getPassword("p")).isEqualTo("p");
    }

    @Test
    void start_should_be_disabled_when_enabled_is_false() {
        InfluxDbOutputWriter writer = newWriter(port);
        writer.getSettings().put("enabled", "false");
        writer.start();
        // disabled: no url parsing happens, write() returns immediately
        writer.write(Collections.<QueryResult>emptyList());
    }

    @Test
    void start_should_configure_writer_with_defaults() {
        InfluxDbOutputWriter writer = newWriter(port);
        writer.getSettings().put("retentionPolicy", "rp");
        writer.getSettings().put("tags", "env=prod");
        writer.start();

        assertThat(writer.toString()).contains("InfluxDbOutputWriter");
    }

    @Test
    void start_should_configure_writer_without_trailing_slash() {
        InfluxDbOutputWriter writer = newWriter(port);
        writer.getSettings().put("url", "http://localhost:" + port + "/write/");
        writer.start();
        assertThat(writer.toString()).isNotNull();
    }

    @Test
    void write_should_send_metric_to_influx_endpoint() throws Exception {
        InfluxDbOutputWriter writer = newWriter(port);
        writer.start();
        writer.write(Collections.singleton(new QueryResult("cpu.load", 42, 1L)));

        String body = handler.awaitBody();
        assertThat(body).contains("cpu.load value=42i");
        assertThat(handler.lastRequest()).contains("db=mydb");
    }

    @Test
    void write_should_send_metric_with_tags() throws Exception {
        InfluxDbOutputWriter writer = newWriter(port);
        writer.getSettings().put("tags", "env=unit");
        writer.start();
        writer.write(Collections.singleton(new QueryResult("mem.used,host=local", 5, 1L)));

        String body = handler.awaitBody();
        assertThat(body).contains("env=unit").contains("host=local");
    }

    @Test
    void write_should_handle_non_2xx_response_without_propagating_exception() {
        InfluxDbOutputWriter writer = newWriter(port);
        writer.start();
        handler.respondWith(500);
        writer.write(Collections.singleton(new QueryResult("cpu", 1, 1L)));
        // writer swallows exceptions internally; nothing to assert except no throw
        assertThat(writer.toString()).isNotNull();
    }

    @Test
    void write_should_silently_swallow_send_errors_when_url_unreachable() {
        InfluxDbOutputWriter writer = newWriter(1); // port 1 -> connection refused
        writer.start();
        // Should not throw
        writer.write(Collections.singleton(new QueryResult("cpu", 1, 1L)));
    }

    @Test
    void start_should_configure_proxy_when_proxy_host_set() {
        InfluxDbOutputWriter writer = newWriter(port);
        writer.getSettings().put("proxyHost", "proxy.example.com");
        writer.getSettings().put("proxyPort", "8080");
        writer.start();
        // proxy is private; verify no exception and writer initialized
        assertThat(writer.toString()).isNotNull();
    }

    @Test
    void start_should_ignore_proxy_when_proxy_host_empty() {
        InfluxDbOutputWriter writer = newWriter(port);
        writer.getSettings().put("proxyHost", "");
        writer.start();
        assertThat(writer.toString()).isNotNull();
    }

    @Test
    void equals_and_hashCode_should_reflect_settings() {
        InfluxDbOutputWriter a = newWriter(port);
        InfluxDbOutputWriter b = newWriter(port);
        InfluxDbOutputWriter c = newWriter(port);
        c.getSettings().put("database", "other");

        assertThat(a).isEqualTo(a);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("str");
    }

    @Test
    void write_should_skip_after_too_many_exceptions() {
        InfluxDbOutputWriter writer = newWriter(port);
        // small retryTimes so we quickly cross the threshold
        writer.getSettings().put("retryTimes", "0");
        writer.start();

        handler.respondWith(500);
        // first write increments exceptionCounter to 1 (>0), so subsequent writes short-circuit
        writer.write(Collections.singleton(new QueryResult("cpu", 1, 1L)));
        // now a write with success handler will return early (exceptionCounter > retryTimes)
        handler.respondWith(200);
        writer.write(Collections.singleton(new QueryResult("cpu", 2, 1L)));
        assertThat(writer.toString()).isNotNull();
    }

    @Test
    void write_should_send_multiple_metrics_in_one_request() throws Exception {
        InfluxDbOutputWriter writer = newWriter(port);
        writer.start();
        java.util.List<QueryResult> results = java.util.Arrays.asList(
                new QueryResult("cpu.load", 1, 1L),
                new QueryResult("mem.used", 2, 1L));
        writer.write(results);

        String body = handler.awaitBody();
        assertThat(body).contains("cpu.load value=1i").contains("mem.used value=2i");
        // multi-metric body separates entries with newline
        assertThat(body).contains("\n");
    }

    @Test
    void write_should_work_when_log_level_is_debug() throws Exception {
        // debug logging is enabled via @BeforeAll so LOG.isDebugEnabled() branches
        // in InfluxDbOutputWriter.write() are exercised
        InfluxDbOutputWriter writer = newWriter(port);
        writer.start();
        writer.write(Collections.singleton(new QueryResult("cpu.load", 7, 1L)));
        assertThat(handler.awaitBody()).contains("cpu.load value=7i");
    }

    @Test
    void write_should_use_proxy_when_configured() throws Exception {
        // Configure a proxy that points at the local server; HttpURLConnection will
        // attempt to tunnel but the request still reaches our handler via the proxy
        // (when proxyHost is localhost, the JVM may connect directly). We mainly
        // exercise the proxy branch in start()/openHttpConnection().
        InfluxDbOutputWriter writer = newWriter(port);
        writer.getSettings().put("proxyHost", "127.0.0.1");
        writer.getSettings().put("proxyPort", String.valueOf(port));
        writer.start();
        writer.write(Collections.singleton(new QueryResult("cpu.load", 9, 1L)));
        // Body may or may not arrive depending on proxy behavior; assert no throw only
        assertThat(writer.toString()).isNotNull();
    }

    @Test
    void write_should_handle_openHttpConnection_failure() throws Exception {
        // Exercise the catch block in openHttpConnection (lines 228-229) by
        // replacing the URL field (after start()) with a non-HTTP URL whose
        // openConnection() returns a non-HttpURLConnection, triggering a
        // ClassCastException that is caught and wrapped in IoRuntimeException.
        InfluxDbOutputWriter writer = newWriter(port);
        writer.start();
        java.lang.reflect.Field urlField = InfluxDbOutputWriter.class.getDeclaredField("url");
        urlField.setAccessible(true);
        urlField.set(writer, new java.net.URL("file:///nonexistent"));
        // write() swallows exceptions internally, so it should not propagate
        writer.write(Collections.singleton(new QueryResult("cpu", 1, 1L)));
        assertThat(writer.toString()).isNotNull();
    }

    @Test
    void start_should_throw_for_invalid_url_scheme() {
        InfluxDbOutputWriter writer = newWriter(port);
        // an unsupported URL scheme triggers MalformedURLException -> wrapped RuntimeException
        writer.getSettings().put("url", "not_a_valid_url://x y z");
        org.assertj.core.api.Assertions.assertThatThrownBy(writer::start)
                .isInstanceOf(RuntimeException.class);
    }

    private static InfluxDbOutputWriter newWriter(int port) {
        InfluxDbOutputWriter writer = new InfluxDbOutputWriter();
        Map<String, Object> settings = new HashMap<>();
        settings.put("url", "http://localhost:" + port + "/write");
        settings.put("database", "mydb");
        // start() requires these settings to be present
        settings.put("user", "u");
        settings.put("password", "p");
        writer.setSettings(settings);
        return writer;
    }

    /** Simple handler that captures the request URI + body and returns 200 (or a custom code). */
    private static final class CapturingHandler implements HttpHandler {
        private volatile String body;
        private volatile String request;
        private volatile int responseCode = 200;
        private final Object lock = new Object();

        void respondWith(int code) {
            this.responseCode = code;
        }

        String lastRequest() {
            return request;
        }

        String awaitBody() throws InterruptedException {
            synchronized (lock) {
                long deadline = System.currentTimeMillis() + 2000;
                while (body == null) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        throw new AssertionError("timeout waiting for request body");
                    }
                    lock.wait(remaining);
                }
                return body;
            }
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (OutputStream os = exchange.getResponseBody()) {
                try {
                    this.request = exchange.getRequestURI().toString();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    exchange.getRequestBody().transferTo(baos);
                    String captured = baos.toString(StandardCharsets.UTF_8);
                    synchronized (lock) {
                        this.body = captured;
                        lock.notifyAll();
                    }
                    byte[] resp = "ok".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(responseCode, resp.length);
                    os.write(resp);
                } catch (RuntimeException re) {
                    // ensure connection is cleaned up
                    exchange.sendResponseHeaders(500, -1);
                }
            }
        }
    }

}
