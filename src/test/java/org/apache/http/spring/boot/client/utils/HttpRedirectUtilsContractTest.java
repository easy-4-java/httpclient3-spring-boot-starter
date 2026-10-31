package org.apache.http.spring.boot.client.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.jupiter.api.Test;

class HttpRedirectUtilsContractTest {
    private final HttpClient client = mock(HttpClient.class);

    @Test
    void getFollowsSingleRedirectToLocation() throws IOException {
        GetMethod original = mock(GetMethod.class);
        when(original.getResponseHeader("location"))
                .thenReturn(new Header("Location", "http://localhost/target"));
        when(client.executeMethod(any(HttpMethod.class))).thenReturn(200);

        GetMethod result = HttpRedirectUtils.stripRedirect(client, original, 302,
                "UTF-8", Map.of("X-Trace", "redirect"), new NameValuePair[] {
                    new NameValuePair("name", "alice")});
        assertThat(result.getURI().getPath()).isEqualTo("/target");
        assertThat(result.getRequestHeader("X-Trace").getValue()).isEqualTo("redirect");
    }

    @Test
    void postWithEntityFollowsSingleRedirect() throws IOException {
        PostMethod original = mock(PostMethod.class);
        when(original.getResponseHeader("location"))
                .thenReturn(new Header("Location", "http://localhost/target"));
        when(client.executeMethod(any(HttpMethod.class))).thenReturn(200);
        var entity = new StringRequestEntity("body", "text/plain", "UTF-8");
        PostMethod result = HttpRedirectUtils.stripRedirect(client, original, 307,
                "UTF-8", null, entity);
        assertThat(result.getURI().getPath()).isEqualTo("/target");
        assertThat(result.getRequestEntity()).isSameAs(entity);
    }

    @Test
    void postWithFormFollowsSingleRedirect() throws IOException {
        PostMethod original = mock(PostMethod.class);
        when(original.getResponseHeader("location"))
                .thenReturn(new Header("Location", "http://localhost/target"));
        when(client.executeMethod(any(HttpMethod.class))).thenReturn(200);
        PostMethod result = HttpRedirectUtils.stripRedirect("http://localhost/source",
                client, original, 302, "UTF-8", "application/x-www-form-urlencoded",
                null, Map.of("name", "alice"));
        assertThat(result.getURI().getPath()).isEqualTo("/target");
    }

    @Test
    void noRedirectRetainsOriginalMethods() throws IOException {
        GetMethod get = new GetMethod("http://localhost/source");
        PostMethod post = new PostMethod("http://localhost/source");
        assertThat(HttpRedirectUtils.stripRedirect(client, get, 200, "UTF-8", null,
                new NameValuePair[0])).isSameAs(get);
        assertThat(HttpRedirectUtils.stripRedirect(client, post, 200, "UTF-8", null,
                new StringRequestEntity("body", "text/plain", "UTF-8"))).isSameAs(post);
        assertThat(HttpRedirectUtils.stripRedirect("http://localhost/source", client, post,
                200, "UTF-8", "application/x-www-form-urlencoded", null,
                Map.of())).isSameAs(post);
    }
}
