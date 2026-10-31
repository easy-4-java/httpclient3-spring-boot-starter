package org.apache.http.spring.boot.client.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.StatusLine;
import org.apache.http.spring.boot.client.handler.ObjectResponseHandler;
import org.junit.jupiter.api.Test;

class ObjectResponseAllowlistContractTest {
    @Test
    void externalConsumerCanAllowOnlyItsDto() throws Exception {
        String xml = new com.thoughtworks.xstream.XStream().toXML(new Payload("hello"));
        assertThatThrownBy(() -> new ObjectResponseHandler().handleResponse(response(xml)))
                .isInstanceOf(com.thoughtworks.xstream.security.ForbiddenClassException.class);
        Payload parsed = (Payload) new ConsumerHandler().handleResponse(response(xml));
        assertThat(parsed.value).isEqualTo("hello");
    }

    private static HttpMethodBase response(String xml) throws Exception {
        HttpMethodBase method = mock(HttpMethodBase.class);
        StatusLine status = mock(StatusLine.class);
        when(method.getStatusLine()).thenReturn(status);
        when(status.getStatusCode()).thenReturn(200);
        when(method.getResponseBodyAsStream()).thenReturn(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return method;
    }

    public static class ConsumerHandler extends ObjectResponseHandler {
        public ConsumerHandler() {
            xstream.allowTypes(new Class<?>[] {Payload.class});
        }
    }

    public static class Payload {
        public String value;

        public Payload() {
        }

        public Payload(String value) {
            this.value = value;
        }
    }
}
