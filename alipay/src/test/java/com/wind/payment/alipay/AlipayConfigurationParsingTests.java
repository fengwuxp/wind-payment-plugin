package com.wind.payment.alipay;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlipayConfigurationParsingTests {

    @Test
    void testAllStringConstructorsAcceptStandardConfiguration() throws Exception {
        String config = configJson("app-id");

        assertDoesNotThrow(() -> new AppAlipayPaymentPlugin(config));
        assertDoesNotThrow(() -> new AuthCodeAlipayPaymentPlugin(config));
        assertDoesNotThrow(() -> new QrCodeAlipayPaymentPlugin(config));
        assertDoesNotThrow(() -> new WebPageAlipayPaymentPlugin(config));
    }

    @Test
    void testStringConstructorRejectsDuplicateConfigurationFields() throws Exception {
        String config = configJson("first-id").replace("\"appId\":\"first-id\"",
                "\"appId\":\"first-id\",\"appId\":\"second-id\"");

        assertThrows(JacksonException.class, () -> new AppAlipayPaymentPlugin(config));
    }

    private static String configJson(String appId) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        return "{\"appId\":\"%s\",\"partner\":\"partner-id\",\"rsaPrivateKey\":\"%s\",\"rsaPublicKey\":\"%s\"}"
                .formatted(appId, privateKey, publicKey);
    }
}
