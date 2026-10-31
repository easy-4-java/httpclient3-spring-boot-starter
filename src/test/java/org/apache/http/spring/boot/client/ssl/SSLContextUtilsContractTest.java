package org.apache.http.spring.boot.client.ssl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import org.junit.jupiter.api.Test;

class SSLContextUtilsContractTest {
    @Test
    void wrapsUnknownProtocolErrorsForBothOverloads() {
        assertThatThrownBy(() -> SSLContextUtils.createSSLContext("missing-tls-protocol",
                (KeyManager[]) null, (TrustManager[]) null))
                .isInstanceOf(IOException.class).hasCauseInstanceOf(NoSuchAlgorithmException.class);
        assertThatThrownBy(() -> SSLContextUtils.createSSLContext("missing-tls-protocol",
                null, null, new SecureRandom()))
                .isInstanceOf(IOException.class).hasCauseInstanceOf(NoSuchAlgorithmException.class);
    }
}
