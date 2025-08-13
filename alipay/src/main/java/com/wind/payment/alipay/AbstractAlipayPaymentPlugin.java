package com.wind.payment.alipay;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeFastpayRefundQueryModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.payment.alipay.notification.AlipayAsyncNotificationRequest;
import com.wind.payment.core.PaymentTransactionException;
import com.wind.payment.core.PaymentTransactionPlugin;
import com.wind.payment.core.enums.PaymentTransactionState;
import com.wind.payment.core.request.PaymentTransactionEventRequest;
import com.wind.payment.core.request.PaymentTransactionRefundEventRequest;
import com.wind.payment.core.request.QueryTransactionOrderRefundRequest;
import com.wind.payment.core.request.QueryTransactionOrderRequest;
import com.wind.payment.core.request.TransactionOrderRefundRequest;
import com.wind.payment.core.response.QueryTransactionOrderResponse;
import com.wind.payment.core.response.TransactionOrderRefundResponse;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 支付宝支付插件抽象类
 *
 * @author wuxp
 * @date 2023-10-01 08:48
 **/
@Slf4j
public abstract class AbstractAlipayPaymentPlugin implements PaymentTransactionPlugin {

    /**
     * 支付结果处理成功返回码
     */
    private static final String PAYMENT_RESULT_HANDLE_SUCCESS_RETURN_CODE = "success";

    /**
     * 支付结果处理失败返回码
     */
    private static final String PAYMENT_RESULT_HANDLE_FAILURE_RETURN_CODE = "failure";

    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    static final String ERROR_PATTERN = "errorCode：%s，errorMessage：%s";

    private static final String ALI_PAY_DEV = "alipaydev";


    private final AliPayPartnerConfig config;

    @Getter
    private final AlipayClient alipayClient;

    protected AbstractAlipayPaymentPlugin(AliPayPartnerConfig config) {
        AssertUtils.hasLength(config.getAppId(), "alipay AppId must not empty");
        AssertUtils.hasLength(config.getPartner(), "alipay Partner must not empty");
        AssertUtils.hasLength(config.getServiceUrl(), "alipay ServiceUrl must not empty");
        AssertUtils.hasLength(config.getRsaPrivateKey(), "alipay RsaPrivateKey must not empty");
        AssertUtils.hasLength(config.getRsaPublicKey(), "alipay RsaPublicKey must not empty");
        this.config = config;
        this.alipayClient = new DefaultAlipayClient(
                config.getServiceUrl(),
                config.getAppId(),
                config.getRsaPrivateKey(),
                "json",
                config.getCharset(),
                config.getRsaPublicKey(),
                config.getEncryptType().name());
    }

    @Override
    public QueryTransactionOrderResponse queryTransactionOrder(QueryTransactionOrderRequest request) {
        AlipayTradeQueryRequest req = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setTradeNo(request.getOutTransactionSn());
        model.setOutTradeNo(request.getTransactionSn());
        req.setBizModel(model);
        QueryTransactionOrderResponse result = new QueryTransactionOrderResponse();
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(req);
            if (log.isDebugEnabled()) {
                log.debug("查询支付宝支付结果 :{}", response.getBody());
            }
            if (response.isSuccess()) {
                Money buyerPayAmount = CurrencyIsoCode.CNY.ofText(response.getBuyerPayAmount());
                result.setOutTransactionSn(response.getTradeNo())
                        .setOutTransactionSn(response.getOutTradeNo())
                        .setOrderAmount(CurrencyIsoCode.CNY.ofText(response.getTotalAmount()))
                        .setBuyerPayAmount(buyerPayAmount)
                        .setReceiptAmount(CurrencyIsoCode.CNY.ofText(response.getReceiptAmount()))
                        .setUseSandboxEnv(this.isUseSandboxEnv())
                        .setTransactionState(this.transformTradeState(response.getTradeStatus(), buyerPayAmount.getIntAmount()))
                        .setRawResponse(response);
            } else {
                throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR, String.format("查询支付宝交易单失败，transactionNo = %s。" +
                        ERROR_PATTERN, request.getTransactionSn(), response.getCode(), response.getMsg()));
            }
        } catch (AlipayApiException exception) {
            throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR, String.format("查询支付宝交易单异常，transactionNo = %s",
                    request.getTransactionSn()), exception);
        }
        return result;
    }

    @Override
    public TransactionOrderRefundResponse transactionOrderRefund(TransactionOrderRefundRequest request) {
        AlipayTradeRefundRequest req = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(request.getOutTransactionSn());
        model.setTradeNo(request.getTransactionSn());
        model.setOutRequestNo(request.getTransactionRefundSn());
        model.setRefundAmount(request.getRefundAmount().fen2Yuan().toString());
        model.setRefundReason(request.getRefundReason());
        if (log.isDebugEnabled()) {
            log.debug("支付宝退款请求参数 {}", model);
        }
        req.setBizModel(model);
        AssertUtils.hasLength(request.getAsynchronousNotificationUrl(), "refund notify url mist not empty");
        req.setNotifyUrl(request.getAsynchronousNotificationUrl());
        if (log.isDebugEnabled()) {
            log.debug("支付宝退款回调 URL ->[{}]", request.getAsynchronousNotificationUrl());
        }

        TransactionOrderRefundResponse result = new TransactionOrderRefundResponse();
        try {
            AlipayTradeRefundResponse response = alipayClient.execute(req);
            if (log.isDebugEnabled()) {
                log.debug("支付宝退款响应, {}", response);
            }
            if (response.isSuccess()) {
                result.setTransactionSn(request.getTransactionRefundSn())
                        .setTransactionRefundSn(response.getOutTradeNo())
                        .setOutTransactionRefundSn(response.getTradeNo())
                        .setOrderAmount(CurrencyIsoCode.CNY.ofText(response.getRefundFee()))
                        .setOrderAmount(request.getOrderAmount())
                        .setTransactionState(PaymentTransactionState.WAIT_REFUND)
                        .setRawResponse(response);
            } else {
                throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR, String.format("支付宝交易退款失败，transactionNo = %s。" +
                        ERROR_PATTERN, request.getTransactionSn(), response.getCode(), response.getMsg()));
            }
            result.setRawResponse(response);
        } catch (AlipayApiException exception) {
            throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR, String.format("支付宝交易退款异常，transactionNo = %s",
                    request.getTransactionSn()), exception);
        }

        return result;
    }

    @Override
    public TransactionOrderRefundResponse queryTransactionOrderRefund(QueryTransactionOrderRefundRequest request) {
        TransactionOrderRefundResponse result = new TransactionOrderRefundResponse();
        AlipayTradeFastpayRefundQueryRequest req = new AlipayTradeFastpayRefundQueryRequest();
        AlipayTradeFastpayRefundQueryModel model = new AlipayTradeFastpayRefundQueryModel();
        model.setTradeNo(request.getTransactionSn());
        model.setOutTradeNo(request.getOutTransactionSn());
        model.setOutRequestNo(request.getRequestRefundSn());
        req.setBizModel(model);
        try {
            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(req);
            if (log.isDebugEnabled()) {
                log.debug("查询支付宝退款响应, {}", response);
            }
            if (response.isSuccess()) {
                Money refundAmount = CurrencyIsoCode.CNY.ofText(response.getRefundAmount());
                Money orderAmount = CurrencyIsoCode.CNY.ofText(response.getTotalAmount());
                result.setTransactionSn(request.getTransactionSn())
                        .setTransactionRefundSn(response.getOutTradeNo())
                        .setOutTransactionRefundSn(response.getOutRequestNo())
                        .setRefundAmount(refundAmount)
                        .setOrderAmount(orderAmount)
                        .setTransactionState(Objects.equals(refundAmount, orderAmount) ? PaymentTransactionState.REFUNDED :
                                PaymentTransactionState.PARTIAL_REFUND)
                        .setRawResponse(response);
            } else {
                throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR,
                        String.format("查询支付宝交易退款失败，transactionNo = %s。" + ERROR_PATTERN, request.getTransactionSn(), response.getCode(),
                                response.getMsg()));
            }
        } catch (AlipayApiException exception) {
            throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR, String.format("查询支付宝交易退款异常，transactionNo = %s",
                    request.getTransactionSn()), exception);
        }
        return result;
    }

    @Override
    public QueryTransactionOrderResponse onPaymentEvent(PaymentTransactionEventRequest request) {
        verifyPaymentNotifyRequest(request);
        QueryTransactionOrderResponse result = new QueryTransactionOrderResponse();
        AlipayAsyncNotificationRequest noticeRequest = request.getRawRequest();
        result.setOutTransactionSn(noticeRequest.getTrade_no())
                .setTransactionSn(noticeRequest.getOut_trade_no())
                .setOrderAmount(CurrencyIsoCode.CNY.of(noticeRequest.getTotal_amount()));
        AliPayTransactionState tradeState = noticeRequest.getTrade_status();
        Money buyerPayAmount;
        BigDecimal payAmount = noticeRequest.getBuyer_pay_amount();
        if (payAmount == null) {
            buyerPayAmount = request.getOrderAmount();
        } else {
            buyerPayAmount = CurrencyIsoCode.CNY.of(payAmount);
        }
        result.setTransactionState(this.transformTradeState(tradeState.name(), buyerPayAmount.getIntAmount()))
                .setBuyerPayAmount(buyerPayAmount)
                .setUseSandboxEnv(this.isUseSandboxEnv())
                .setPayerAccount(noticeRequest.getBuyer_logon_id())
                .setRawResponse(noticeRequest);
        BigDecimal receiptAmount = noticeRequest.getReceipt_amount();
        if (receiptAmount != null) {
            // TODO 实收金额验证
            result.setReceiptAmount(CurrencyIsoCode.CNY.of(receiptAmount));
        }
        return result;
    }

    @Override
    public TransactionOrderRefundResponse onRefundEvent(PaymentTransactionRefundEventRequest request) {
        verifyRefundNotifyRequest(request);
        // 退款处理订单通知
        TransactionOrderRefundResponse result = new TransactionOrderRefundResponse();
        AlipayAsyncNotificationRequest noticeRequest = request.getRawRequest();
        result.setTransactionRefundSn(request.getTransactionRefundSn());
        result.setOutTransactionRefundSn(noticeRequest.getOut_biz_no());
        result.setOrderAmount(CurrencyIsoCode.CNY.of(noticeRequest.getTotal_amount()));
        result.setRefundAmount(CurrencyIsoCode.CNY.of(noticeRequest.getRefund_fee()));
        return result;
    }

    @Override
    public Object getWebHookResponse(boolean isSuccessful) {
        return isSuccessful ? PAYMENT_RESULT_HANDLE_SUCCESS_RETURN_CODE : PAYMENT_RESULT_HANDLE_FAILURE_RETURN_CODE;
    }

    /**
     * @return 是否使用沙箱环境
     */
    protected boolean isUseSandboxEnv() {
        return this.config.getServiceUrl().contains(ALI_PAY_DEV);
    }

    /**
     * @param state           支付宝交易状态
     * @param buyerPaidAmount 买家实付金额
     * @return 支付交易状态
     */
    private PaymentTransactionState transformTradeState(String state, int buyerPaidAmount) {
        AliPayTransactionState aliPayTradeState = Enum.valueOf(AliPayTransactionState.class, state);
        if (AliPayTransactionState.WAIT_BUYER_PAY.equals(aliPayTradeState)) {
            return PaymentTransactionState.PAYING;
        }

        if (AliPayTransactionState.TRADE_SUCCESS.equals(aliPayTradeState)) {
            return PaymentTransactionState.COMPLETED;
        }

        if (AliPayTransactionState.TRADE_FINISHED.equals(aliPayTradeState)) {
            return PaymentTransactionState.COMPLETED;
        }

        if (AliPayTransactionState.TRADE_CLOSED.equals(aliPayTradeState)) {
            if (buyerPaidAmount == 0) {
                // 买家实付金额为0 说明未支付
                return PaymentTransactionState.CLOSED;
            }
            return PaymentTransactionState.UNKNOWN;
        }

        return PaymentTransactionState.UNKNOWN;
    }

    /**
     * 验证支付宝支付通知请求
     */
    private void verifyPaymentNotifyRequest(PaymentTransactionEventRequest request) {
        // 参数验证
        String tradeNo = request.getTransactionSn();
        AlipayAsyncNotificationRequest rawRequest = request.getRawRequest();
        BigDecimal orderAmount = request.getOrderAmount().fen2Yuan();
        boolean paramVerify = Objects.equals(tradeNo, rawRequest.getOut_trade_no())
                && Objects.equals(orderAmount, rawRequest.getTotal_amount());
        AssertUtils.isTrue(paramVerify, () -> String.format("支付宝支付通知，【%s】参数验证失:%s", tradeNo, rawRequest));
        verifySign(rawRequest);
    }


    /**
     * 验证支付宝退款通知请求
     */
    private void verifyRefundNotifyRequest(PaymentTransactionRefundEventRequest request) {
        // 参数验证
        Map<String, String> params = request.getRawRequest();
        String transactionRefundNo = request.getTransactionRefundSn();
        BigDecimal refundAmount = request.getOrderAmount().fen2Yuan();

        boolean paramVerify = Objects.equals(transactionRefundNo, params.get("out_trade_no"))
                && Objects.equals(refundAmount.toString(), params.get("refund_fee"));
        AssertUtils.isTrue(paramVerify, String.format("支付宝退款通知，【%s】参数验证失败，%s", transactionRefundNo, params));
        verifySign(request.getRawRequest());
    }


    /**
     * 验证签名
     *
     * @param request 回调参数
     */
    private void verifySign(AlipayAsyncNotificationRequest request) {
        // 签名验证
        Map<String, String> signParams = new HashMap<>();
        // 深 Copy
        Map<String, String> params = JSON.parseObject(JSON.toJSONString(request), new TypeReference<Map<String, String>>() {
        });
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if (entry.getValue() != null) {
                signParams.put(key, entry.getValue());
            }
        }
        AliPayPartnerConfig.EncryptType signType = AliPayPartnerConfig.EncryptType.valueOf(params.get("sign_type"));
        try {
            // 切记 rsaPublicKey 是支付宝的公钥，请去 open.alipay.com 对应应用下查看。
            boolean result = AlipaySignature.rsaCheckV1(signParams, config.getRsaPublicKey(), config.getCharset(), signType.name());
            AssertUtils.isTrue(result, "支付宝通知签名验证失败");
        } catch (AlipayApiException exception) {
            throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR, "支付宝支付通知签名验证异常", exception);
        }
    }

    static String normalizationBody(String description) {
        return StringUtils.abbreviate(description, 128);
    }

    @NotNull
    static String getExpireTimeOrUseDefault(Duration expireTime) {
        if (expireTime == null) {
            expireTime = Duration.ofMinutes(30);
        }
        return DateFormatUtils.format(new Date(System.currentTimeMillis() + expireTime.toMillis()), DATE_FORMAT_PATTERN);
    }
}
