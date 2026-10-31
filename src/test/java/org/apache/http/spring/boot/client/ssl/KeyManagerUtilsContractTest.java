package org.apache.http.spring.boot.client.ssl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.security.KeyStore;
import javax.net.ssl.X509ExtendedKeyManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KeyManagerUtilsContractTest {
    @TempDir Path tempDir;

    @Test
    void loadsPrivateKeyAndCertificateChainFromClientStore() throws Exception {
        Path store = tempDir.resolve("client.p12");
        String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        Process process = new ProcessBuilder(keytool, "-genkeypair", "-alias", "client",
                "-keyalg", "RSA", "-keysize", "2048", "-dname", "CN=localhost",
                "-storetype", "PKCS12", "-keystore", store.toString(), "-storepass", "changeit",
                "-keypass", "changeit", "-validity", "1", "-noprompt")
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertThat(process.waitFor()).as(output).isZero();

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (var input = java.nio.file.Files.newInputStream(store)) {
            keyStore.load(input, "changeit".toCharArray());
        }
        X509ExtendedKeyManager manager = (X509ExtendedKeyManager)
                KeyManagerUtils.createClientKeyManager(keyStore, null, "changeit");
        assertThat(manager.chooseClientAlias(new String[] {"RSA"}, null, null)).isEqualTo("client");
        assertThat(manager.getClientAliases("RSA", null)).containsExactly("client");
        assertThat(manager.getCertificateChain("client")).hasSize(1);
        assertThat(manager.getPrivateKey("client")).isNotNull();
        assertThat(manager.getServerAliases("RSA", null)).isNull();
        assertThat(manager.chooseServerAlias("RSA", null, null)).isNull();

        assertThat(KeyManagerUtils.createClientKeyManager("PKCS12", store.toFile(),
                "changeit", "client", "changeit")).isNotNull();
        assertThat(KeyManagerUtils.createClientKeyManager(store.toFile(), "changeit", "client"))
                .isNotNull();
        assertThat(KeyManagerUtils.createClientKeyManager(store.toFile(), "changeit"))
                .isNotNull();
    }

    @Test
    void rejectsStoreWithNoPrivateKey() throws Exception {
        KeyStore empty = KeyStore.getInstance("PKCS12");
        empty.load(null, "changeit".toCharArray());
        assertThatThrownBy(() -> KeyManagerUtils.createClientKeyManager(empty, null, "changeit"))
                .isInstanceOf(java.security.KeyStoreException.class)
                .hasMessageContaining("Cannot find a private key entry");
    }
}
