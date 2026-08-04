package com.wind.payment.alipay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.wind.common.exception.BaseException;
import com.wind.payment.alipay.webhook.AlipayAsyncNotificationRequest;
import com.wind.payment.core.PaymentTransactionException;
import com.wind.payment.core.enums.PaymentTransactionState;
import com.wind.payment.core.request.PaymentTransactionEventRequest;
import com.wind.payment.core.request.PaymentTransactionRefundEventRequest;
import com.wind.payment.core.response.QueryTransactionOrderResponse;
import com.wind.payment.core.response.TransactionOrderRefundResponse;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlipayNotificationContractTests {

    private static final String CHARSET = "UTF-8";

    private static String privateKey;

    private static String publicKey;

    private static AppAlipayPaymentPlugin plugin;

    @BeforeAll
    static void setUpKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        AliPayPartnerConfig config = new AliPayPartnerConfig();
        config.setAppId("app-id");
        config.setPartner("partner-id");
        config.setRsaPrivateKey(privateKey);
        config.setRsaPublicKey(publicKey);
        plugin = new AppAlipayPaymentPlugin(config);
    }

    @Test
    void testAcceptsSignedPaymentNotificationAsOriginalStringMap() throws Exception {
        Map<String, String> params = signedParams(Map.of(
                "out_trade_no", "payment-001",
                "trade_no", "alipay-001",
                "trade_status", "TRADE_SUCCESS",
                "total_amount", "100.00",
                "buyer_pay_amount", "100.00",
                "receipt_amount", "99.50",
                "buyer_logon_id", "buyer@example.com",
                "notify_time", "2023-11-29 13:09:27"));
        PaymentTransactionEventRequest request = new PaymentTransactionEventRequest();
        request.setTransactionSn("payment-001");
        request.setOrderAmount(CurrencyIsoCode.CNY.of(10_000));
        request.setRawRequest(params);

        QueryTransactionOrderResponse response = plugin.onPaymentEvent(request);

        assertEquals("payment-001", response.getTransactionSn());
        assertEquals("alipay-001", response.getOutTransactionSn());
        assertEquals(PaymentTransactionState.COMPLETED, response.getTransactionState());
        assertEquals(CurrencyIsoCode.CNY.of(10_000), response.getOrderAmount());
        assertEquals(CurrencyIsoCode.CNY.of(9_950), response.getReceiptAmount());
        assertEquals(params, response.<Map<String, String>>getRawResponse());
        assertNotNull(params.get("sign"));
        assertEquals("RSA2", params.get("sign_type"));
        Map<String, String> rawResponse = response.getRawResponse();
        assertNotNull(rawResponse.get("sign"));
        assertEquals("RSA2", rawResponse.get("sign_type"));
    }

    @Test
    void testRejectsNotificationWhenSignedFieldIsTampered() throws Exception {
        Map<String, String> params = signedParams(Map.of(
                "out_trade_no", "payment-001",
                "trade_no", "alipay-001",
                "trade_status", "TRADE_SUCCESS",
                "total_amount", "100.00",
                "notify_time", "2023-11-29 13:09:27"));
        params.put("notify_time", "2023-11-29 13:09:28");
        PaymentTransactionEventRequest request = new PaymentTransactionEventRequest();
        request.setTransactionSn("payment-001");
        request.setOrderAmount(CurrencyIsoCode.CNY.of(10_000));
        request.setRawRequest(params);

        assertThrows(BaseException.class, () -> plugin.onPaymentEvent(request));
    }

    @Test
    void testValidatesRefundAgainstRefundBusinessNumberAndRefundAmount() throws Exception {
        Map<String, String> params = signedParams(Map.of(
                "out_trade_no", "payment-001",
                "out_biz_no", "refund-001",
                "total_amount", "100.00",
                "refund_fee", "20.00",
                "notify_time", "2023-11-29 13:09:27"));
        PaymentTransactionRefundEventRequest request = new PaymentTransactionRefundEventRequest();
        request.setTransactionRefundSn("refund-001");
        request.setOrderAmount(CurrencyIsoCode.CNY.of(10_000));
        request.setRefundAmount(CurrencyIsoCode.CNY.of(2_000));
        request.setRawRequest(params);

        TransactionOrderRefundResponse response = plugin.onRefundEvent(request);

        assertEquals("refund-001", response.getTransactionRefundSn());
        assertEquals("refund-001", response.getOutTransactionRefundSn());
        assertEquals(CurrencyIsoCode.CNY.of(10_000), response.getOrderAmount());
        assertEquals(CurrencyIsoCode.CNY.of(2_000), response.getRefundAmount());
        assertEquals(params, response.<Map<String, String>>getRawResponse());
    }

    @Test
    void testRejectsTypedDtoInsteadOfTreatingItAsSignedRawParameters() {
        PaymentTransactionEventRequest request = new PaymentTransactionEventRequest();
        request.setTransactionSn("payment-001");
        request.setOrderAmount(CurrencyIsoCode.CNY.of(10_000));
        request.setRawRequest(new AlipayAsyncNotificationRequest());

        assertThrows(PaymentTransactionException.class, () -> plugin.onPaymentEvent(request));
    }

    private static Map<String, String> signedParams(Map<String, String> values) throws AlipayApiException {
        Map<String, String> params = new HashMap<>(values);
        params.put("sign_type", AliPayPartnerConfig.EncryptType.RSA2.name());
        Map<String, String> unsignedParams = new HashMap<>(params);
        String content = AlipaySignature.getSignCheckContentV1(unsignedParams);
        params.put("sign", AlipaySignature.sign(content, privateKey, CHARSET, "RSA2"));
        return params;
    }
}
