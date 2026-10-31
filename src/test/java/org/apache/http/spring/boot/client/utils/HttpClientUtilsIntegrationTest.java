package org.apache.http.spring.boot.client.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.spring.boot.client.handler.PlainTextResponseHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpClientUtilsIntegrationTest {
    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> request = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/inspect", this::inspect);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/inspect";
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void inspect(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        request.set(exchange.getRequestMethod() + " " + exchange.getRequestURI()
                + " body=" + body + " header=" + exchange.getRequestHeaders().getFirst("X-Trace"));
        byte[] response = "accepted".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    @Test
    void getSendsQueryParametersAndCustomHeader() throws IOException {
        String response = HttpClientUtils.httpRequestWithGet(baseUrl,
                Map.of("name", "alice"), "UTF-8", Map.of("X-Trace", "get"),
                new PlainTextResponseHandler());
        assertThat(response).isEqualTo("accepted");
        assertThat(request.get()).contains("GET /inspect?name=alice", "header=get");
    }

    @Test
    void convenienceGetOverloadsReturnTheSameResponse() throws IOException {
        assertThat(HttpClientUtils.httpRequestWithGet(baseUrl, new PlainTextResponseHandler()))
                .isEqualTo("accepted");
        assertThat(request.get()).startsWith("GET /inspect");
        assertThat(HttpClientUtils.httpRequestWithGet(baseUrl, Map.of("name", "bob"),
                new PlainTextResponseHandler())).isEqualTo("accepted");
        assertThat(request.get()).contains("name=bob");
        assertThat(HttpClientUtils.httpRequestWithGet(baseUrl, Map.of("name", "carol"),
                "UTF-8", new PlainTextResponseHandler())).isEqualTo("accepted");
        assertThat(request.get()).contains("name=carol");
    }

    @Test
    void postSendsFormParametersAndCustomHeader() throws IOException {
        String response = HttpClientUtils.httpRequestWithPost(baseUrl,
                Map.of("name", "alice"), "UTF-8", "application/x-www-form-urlencoded",
                Map.of("X-Trace", "form"), new PlainTextResponseHandler());
        assertThat(response).isEqualTo("accepted");
        assertThat(request.get()).contains("POST /inspect", "name=alice", "header=form");
    }

    @Test
    void conveniencePostOverloadsReturnTheSameResponse() throws IOException {
        assertThat(HttpClientUtils.httpRequestWithPost(baseUrl, Map.of("name", "bob"),
                new PlainTextResponseHandler())).isEqualTo("accepted");
        assertThat(request.get()).contains("name=bob");
        assertThat(HttpClientUtils.httpRequestWithPost(baseUrl, Map.of("name", "carol"),
                "UTF-8", new PlainTextResponseHandler())).isEqualTo("accepted");
        assertThat(request.get()).contains("name=carol");
        assertThat(HttpClientUtils.httpRequestWithPost(baseUrl, "{\"name\":\"dave\"}",
                new PlainTextResponseHandler())).isEqualTo("accepted");
        assertThat(request.get()).startsWith("POST /inspect");
    }

    @Test
    void postSendsJsonBodyAndCustomHeader() throws IOException {
        String response = HttpClientUtils.httpRequestWithPost(baseUrl,
                "{\"name\":\"alice\"}", "UTF-8", Map.of("X-Trace", "json"),
                new PlainTextResponseHandler());
        assertThat(response).isEqualTo("accepted");
        assertThat(request.get()).contains("POST /inspect", "header=json");
    }

    @Test
    void requestFailureIsReturnedAsIoException() {
        assertThatThrownBy(() -> HttpClientUtils.httpRequestWithGet("http://127.0.0.1:0/absent",
                new PlainTextResponseHandler())).isInstanceOf(IOException.class);
    }
}
