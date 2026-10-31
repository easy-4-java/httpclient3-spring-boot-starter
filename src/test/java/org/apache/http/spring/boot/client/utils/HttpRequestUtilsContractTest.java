package org.apache.http.spring.boot.client.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.http.spring.boot.client.exception.HttpResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HttpRequestUtilsContractTest {
    @TempDir Path tempDir;

    @Test
    void multipartContainsTextFileStreamAndBytes() throws Exception {
        Path file = tempDir.resolve("note.txt");
        Files.writeString(file, "file-body");
        Map<String, Object> parts = new LinkedHashMap<>();
        parts.put("text", "plain");
        parts.put("file", file.toFile());
        parts.put("stream", new ByteArrayInputStream("stream-body".getBytes(StandardCharsets.UTF_8)));
        parts.put("bytes", "byte-body".getBytes(StandardCharsets.UTF_8));

        PostMethod method = new PostMethod("http://localhost/upload");
        MultipartRequestEntity entity = HttpRequestUtils.getHttpEntity(method,
                "http://localhost/upload?source=local", parts, "UTF-8");
        assertThat(entity).isNotNull();
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        entity.writeRequest(encoded);
        String body = encoded.toString(StandardCharsets.UTF_8);
        assertThat(body).contains("name=\"source\"", "local", "name=\"text\"", "plain",
                "file-body", "stream-body", "byte-body", "note.txt");

        HttpRequestUtils.setHttpMethod(method, "http://localhost/upload", parts, "UTF-8", null, null);
        assertThat(method.getRequestEntity()).isInstanceOf(MultipartRequestEntity.class);
        assertThat(method.getRequestHeader("Content-Type").getValue()).contains("multipart/form-data");
    }

    @Test
    void requestEntitiesPreserveEachSupportedBodyType() throws Exception {
        Path file = tempDir.resolve("note.txt");
        Files.writeString(file, "file-body");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("file", file.toFile());
        values.put("stream", new ByteArrayInputStream("stream-body".getBytes(StandardCharsets.UTF_8)));
        values.put("bytes", "byte-body".getBytes(StandardCharsets.UTF_8));
        values.put("text", "plain");
        var entities = HttpRequestUtils.buildRequestEntity(values);
        assertThat(entities).hasSize(4);
        for (var entry : entities.entrySet()) {
            ByteArrayOutputStream encoded = new ByteArrayOutputStream();
            entry.getValue().writeRequest(encoded);
            assertThat(encoded.toByteArray()).isNotEmpty();
        }
    }

    @Test
    void redirectUsesLocationForBothMethodsAndRejectsMissingLocation() throws Exception {
        GetMethod get = mock(GetMethod.class);
        when(get.getResponseHeader("location")).thenReturn(new Header("Location", "http://localhost/next"));
        assertThat(HttpRequestUtils.getHttpRedirect(get, 302, "UTF-8", null).getURI().getPath())
                .isEqualTo("/next");

        PostMethod post = mock(PostMethod.class);
        when(post.getResponseHeader("location")).thenReturn(new Header("Location", "http://localhost/next"));
        assertThat(HttpRequestUtils.getHttpRedirect(post, 307, "UTF-8", null).getURI().getPath())
                .isEqualTo("/next");
        assertThatThrownBy(() -> HttpRequestUtils.getHttpRedirect(new GetMethod(), 301, "UTF-8", null))
                .isInstanceOf(HttpResponseException.class);
    }

    @Test
    void gzipDetectionChecksRequestHeaderElements() {
        GetMethod method = new GetMethod("http://localhost/");
        method.addRequestHeader("Accept-Encoding", "br, gzip");
        assertThat(HttpRequestUtils.isGzip(method)).isTrue();
        method.removeRequestHeader("Accept-Encoding");
        method.addRequestHeader("Accept-Encoding", "br");
        assertThat(HttpRequestUtils.isGzip(method)).isFalse();
    }
}
