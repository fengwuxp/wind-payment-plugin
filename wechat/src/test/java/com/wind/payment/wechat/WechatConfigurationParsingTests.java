package com.wind.payment.wechat;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WechatConfigurationParsingTests {

    private static final String CONFIG = "{\"appId\":\"app-id\",\"partner\":\"partner-id\",\"partnerSecret\":\"secret\"}";

    @Test
    void testAllStringConstructorsAcceptStandardConfiguration() {
        assertDoesNotThrow(() -> new AppWechatPaymentPlugin(CONFIG));
        assertDoesNotThrow(() -> new JsApiWechatPaymentPlugin(CONFIG));
        assertDoesNotThrow(() -> new ScanWechatPaymentPlugin(CONFIG));
        assertDoesNotThrow(() -> new WebPageWechatPaymentPlugin(CONFIG));
    }

    @Test
    void testStringConstructorRejectsDuplicateConfigurationFields() {
        String config = CONFIG.replace("\"appId\":\"app-id\"",
                "\"appId\":\"first-id\",\"appId\":\"second-id\"");

        assertThrows(JacksonException.class, () -> new AppWechatPaymentPlugin(config));
    }
}
