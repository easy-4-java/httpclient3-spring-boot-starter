package org.apache.http.spring.boot.client.ssl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.Test;

class SSLSocketUtilsContractTest {
    @Test
    void enablesHttpsEndpointIdentificationOnSocket() {
        SSLSocket socket = mock(SSLSocket.class);
        when(socket.getSSLParameters()).thenReturn(new SSLParameters());

        assertThat(SSLSocketUtils.enableEndpointNameVerification(socket)).isTrue();
        verify(socket).setSSLParameters(argThat(params ->
                "HTTPS".equals(params.getEndpointIdentificationAlgorithm())));
    }

    @Test
    void reportsFailureWhenSocketHasNoSslParameters() {
        SSLSocket socket = mock(SSLSocket.class);
        when(socket.getSSLParameters()).thenReturn(null);
        assertThat(SSLSocketUtils.enableEndpointNameVerification(socket)).isFalse();
    }
}
