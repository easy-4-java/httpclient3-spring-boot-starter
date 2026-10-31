package org.apache.http.spring.boot.client.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.StatusLine;
import org.apache.http.spring.boot.client.exception.HttpResponseException;
import org.junit.jupiter.api.Test;

class ResponseHandlersContractTest {
    @Test
    void redirectHandlerReturnsLocationAndRejectsMissingLocation() throws Exception {
        RedirectResponseHandler handler = new RedirectResponseHandler();
        for (int status : new int[] {301, 302, 303, 307}) {
            HttpMethodBase method = response(status, null, null);
            when(method.getResponseHeader("location"))
                    .thenReturn(new Header("Location", "https://example.test/next"));
            assertThat(handler.handleResponse(method)).isEqualTo("https://example.test/next");
        }
        HttpMethodBase empty = response(302, null, null);
        when(empty.getResponseHeader("location")).thenReturn(new Header("Location", ""));
        assertThat(handler.handleResponse(empty)).isEqualTo("/");
        assertThatThrownBy(() -> handler.handleResponse(response(302, null, null)))
                .isInstanceOf(HttpResponseException.class);
        assertThatThrownBy(() -> handler.handleResponse(response(200, null, null)))
                .isInstanceOf(HttpResponseException.class);
    }

    @Test
    void jsonHandlerParsesJsonAndNestedXmlAndRejectsBadMediaType() throws Exception {
        JSONResponseHandler handler = new JSONResponseHandler();
        var json = handler.handleResponse(response(200, "application/json", "{\"answer\":42}"));
        assertThat(json.getInteger("answer")).isEqualTo(42);
        var xml = handler.handleResponse(response(200, "application/xml",
                "<root><name>Alice</name><address><city>Paris</city></address></root>"));
        assertThat(xml.getString("name")).isEqualTo("Alice");
        assertThat(xml.getJSONObject("address").getString("city")).isEqualTo("Paris");
        assertThatThrownBy(() -> handler.handleResponse(response(200, "text/plain", "wrong")))
                .isInstanceOf(HttpResponseException.class);
        assertThatThrownBy(() -> handler.handleResponse(response(200, "application/xml", "<broken")))
                .isInstanceOf(HttpResponseException.class);
    }

    @Test
    void xmlHandlerParsesResponseBodyAndRejectsMalformedXml() throws Exception {
        XMLResponseHandler handler = new XMLResponseHandler();
        assertThat(handler.handleResponse(response(200, "application/xml", "<root><name>Alice</name></root>"))
                .getDocumentElement().getTagName()).isEqualTo("root");
        assertThatThrownBy(() -> handler.handleResponse(response(200, "application/xml", "<broken")))
                .isInstanceOf(HttpResponseException.class);
    }

    private static HttpMethodBase response(int code, String contentType, String body) throws Exception {
        HttpMethodBase method = mock(HttpMethodBase.class);
        StatusLine status = mock(StatusLine.class);
        when(method.getStatusLine()).thenReturn(status);
        when(status.getStatusCode()).thenReturn(code);
        if (contentType != null) {
            when(method.getResponseHeader("Content-Type"))
                    .thenReturn(new Header("Content-Type", contentType));
        }
        if (body != null) {
            when(method.getResponseBodyAsString()).thenReturn(body);
            when(method.getResponseBodyAsStream()).thenReturn(
                    new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        }
        return method;
    }
}
