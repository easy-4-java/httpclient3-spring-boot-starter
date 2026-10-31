package org.apache.http.spring.boot.client.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.StatusLine;
import org.apache.http.spring.boot.client.exception.HttpResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XmlExternalEntitiesContractTest {
    @TempDir Path tempDir;

    @Test
    void domRejectsLocalFileEntityWithoutReadingItsContent() throws Exception {
        Path privateFile = tempDir.resolve("private.txt");
        Files.writeString(privateFile, "fixture-secret-dom");
        String xml = "<!DOCTYPE root [<!ENTITY leaked SYSTEM \"" + privateFile.toUri()
                + "\">]><root>&leaked;</root>";
        assertThatThrownBy(() -> new XMLResponseHandler().handleResponse(response(xml)))
                .isInstanceOf(HttpResponseException.class)
                .hasMessageNotContaining("fixture-secret-dom");
    }

    @Test
    void jsonXmlBranchRejectsLocalFileEntityWithoutReadingItsContent() throws Exception {
        Path privateFile = tempDir.resolve("private.txt");
        Files.writeString(privateFile, "fixture-secret-json");
        String xml = "<!DOCTYPE root [<!ENTITY leaked SYSTEM \"" + privateFile.toUri()
                + "\">]><root>&leaked;</root>";
        assertThatThrownBy(() -> new JSONResponseHandler().handleResponse(response(xml)))
                .isInstanceOf(HttpResponseException.class)
                .hasMessageNotContaining("fixture-secret-json");
    }

    @Test
    void domRejectsExternalDtdWithoutHttpRequest() throws Exception {
        assertExternalDtdRejectedWithoutRequest(false);
    }

    @Test
    void jsonXmlBranchRejectsExternalDtdWithoutHttpRequest() throws Exception {
        assertExternalDtdRejectedWithoutRequest(true);
    }

    private void assertExternalDtdRejectedWithoutRequest(boolean jsonHandler) throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/fixture.dtd", exchange -> {
            requests.incrementAndGet();
            byte[] body = "<!ENTITY leaked 'unexpected'>".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            String xml = "<!DOCTYPE root SYSTEM \"http://127.0.0.1:"
                    + server.getAddress().getPort() + "/fixture.dtd\"><root>safe</root>";
            assertThatThrownBy(() -> {
                if (jsonHandler) {
                    new JSONResponseHandler().handleResponse(response(xml));
                } else {
                    new XMLResponseHandler().handleResponse(response(xml));
                }
            }).isInstanceOf(HttpResponseException.class);
            assertThat(requests.get()).isZero();
        } finally {
            server.stop(0);
        }
    }

    private static HttpMethodBase response(String xml) throws Exception {
        HttpMethodBase method = mock(HttpMethodBase.class);
        StatusLine status = mock(StatusLine.class);
        when(method.getStatusLine()).thenReturn(status);
        when(status.getStatusCode()).thenReturn(200);
        when(method.getResponseHeader("Content-Type"))
                .thenReturn(new Header("Content-Type", "application/xml"));
        when(method.getResponseBodyAsString()).thenReturn(xml);
        when(method.getResponseBodyAsStream()).thenReturn(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return method;
    }
}
