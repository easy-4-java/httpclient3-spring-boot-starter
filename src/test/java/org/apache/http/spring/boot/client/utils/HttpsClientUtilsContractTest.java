package org.apache.http.spring.boot.client.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SSLProtocolSocketFactory;
import org.junit.jupiter.api.Test;

class HttpsClientUtilsContractTest {
    @Test
    void registersHttpsProtocolWithSslSocketFactory() throws Exception {
        Class.forName(HttpsClientUtils.class.getName());
        Protocol https = Protocol.getProtocol("https");
        assertThat(https.getDefaultPort()).isEqualTo(443);
        assertThat(https.getSocketFactory()).isInstanceOf(SSLProtocolSocketFactory.class);
    }
}
