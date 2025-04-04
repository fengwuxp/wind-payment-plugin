package com.wind.payment.wechat;

import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.bean.notify.WxPayRefundNotifyResult;
import com.github.binarywang.wxpay.bean.request.WxPayRefundQueryRequest;
import com.github.binarywang.wxpay.bean.request.WxPayRefundRequest;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryResult;
import com.github.binarywang.wxpay.bean.result.WxPayRefundQueryResult;
import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import com.wind.common.WindConstants;
import com.wind.common.enums.DescriptiveEnum;
import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.DefaultExceptionCode;
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
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 微信支付插件抽象类
 *
 * @author wuxp
 * @date 2023-10-03 09:36
 **/
@Slf4j
public abstract class AbstractWechatPaymentPlugin implements PaymentTransactionPlugin {

    /**
     * 支付结果处理成功返回码
     */
    private static final String PAYMENT_RESULT_HANDLE_SUCCESS_RETURN_CONTENT = WxPayNotifyResponse.success("success");

    /**
     * 支付结果处理失败返回码
     */
    private static final String PAYMENT_RESULT_HANDLE_FAILURE_RETURN_CONTENT = WxPayNotifyResponse.success("failure");

    private static final List<String> SUCCESS_CODES = Arrays.asList(WxPayConstants.ResultCode.SUCCESS, WindConstants.EMPTY);

    private static final String DATE_FORMAT_PATTERN = "yyyyMMddHHmmss";

    /**
     * SUCCESS -—支付成功,
     * REFUND —-转入退款,
     * NOTPAY —-未支付,
     * CLOSED —-已关闭,
     * REVOKED —-已撤销（刷卡支付）,
     * USERPAYING --用户支付中
     * ,PAYERROR --支付失败(其他原因，如银行返回失败)
     */
    @AllArgsConstructor
    @Getter
    protected enum WeChatTradeSate implements DescriptiveEnum {

        SUCCESS("支付成功"),

        REFUND("转入退款"),

        NOTPAY("未支付"),

        CLOSED("已关闭"),

        REVOKED("已撤销（刷卡支付）"),

        USERPAYING("用户支付中"),

        PAYERROR("支付失败(其他原因，如银行返回失败)");

        // 说明
        private final String desc;
    }

    private final WechatPayPartnerConfig config;

    @Getter
    private final WxPayService wxPayService;

    protected AbstractWechatPaymentPlugin(WechatPayPartnerConfig config) {
        AssertUtils.hasLength(config.getAppId(), "wechat AppId must not empty");
        AssertUtils.hasLength(config.getPartner(), "wechat Partner must not empty");
        AssertUtils.hasLength(config.getPartnerSecret(), "wechat PartnerSecret must not empty");
        this.config = config;
        this.wxPayService = buildWxPayService();
    }


    @Override
    public QueryTransactionOrderResponse queryTransactionOrder(QueryTransactionOrderRequest request) {
        try {
            WxPayOrderQueryResult response = wxPayService.queryOrder(request.getOutTransactionSn(), request.getTransactionSn());
            if (log.isDebugEnabled()) {
                log.debug("查询微信支付结果，transactionNo = {}，响应：{}", request.getTransactionSn(), response);
            }
            QueryTransactionOrderResponse result = new QueryTransactionOrderResponse();
            return result.setOutTransactionSn(response.getTransactionId())
                    .setOutTransactionSn(response.getOutTradeNo())
                    .setOrderAmount(CurrencyIsoCode.CNY.of(response.getTotalFee()))
                    .setBuyerPayAmount(CurrencyIsoCode.CNY.of(response.getSettlementTotalFee()))
                    .setReceiptAmount(CurrencyIsoCode.CNY.of(response.getSettlementTotalFee()))
                    .setTransactionState(this.transformTradeState(response.getTradeState()))
                    .setUseSandboxEnv(isUseSandboxEnv())
                    .setRawResponse(response);
        } catch (WxPayException exception) {
            throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR, String.format("查询微信交易单异常，transactionNo = %s",
                    request.getTransactionSn()), exception);
        }
    }

    @Override
    public TransactionOrderRefundResponse transactionOrderRefund(TransactionOrderRefundRequest request) {
        WxPayRefundRequest req = new WxPayRefundRequest();
        req.setRefundFee(request.getRefundAmount().getAmount());
        req.setTransactionId(request.getOutTransactionSn());
        req.setOutTradeNo(request.getTransactionSn());
        req.setOutRefundNo(request.getTransactionRefundSn());
        req.setTotalFee(request.getOrderAmount().getAmount());
        req.setNotifyUrl(request.getAsynchronousNotificationUrl());
        req.setRefundDesc(request.getRefundReason());
        try {
            WxPayRefundResult response = wxPayService.refund(req);
            if (log.isDebugEnabled()) {
                log.debug("微信退款请求结果，transactionNo = {}，响应：{}", request.getTransactionSn(), response);
            }
            TransactionOrderRefundResponse result = new TransactionOrderRefundResponse();
            return result.setTransactionSn(request.getTransactionSn())
                    .setTransactionRefundSn(response.getOutRefundNo())
                    .setOutTransactionRefundSn(response.getRefundId())
                    .setRefundAmount(CurrencyIsoCode.CNY.of(response.getRefundFee()))
                    .setOrderAmount(CurrencyIsoCode.CNY.of(response.getTotalFee()))
                    .setTransactionState(PaymentTransactionState.WAIT_REFUND)
                    .setRawResponse(response);
        } catch (WxPayException exception) {
            throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR, String.format("微信交易退款异常，transactionNo = %s",
                    request.getTransactionSn()), exception);
        }
    }

    @Override
    public TransactionOrderRefundResponse queryTransactionOrderRefund(QueryTransactionOrderRefundRequest request) {
        WxPayRefundQueryRequest req = new WxPayRefundQueryRequest();
        req.setTransactionId(request.getOutTransactionSn());
        req.setOutTradeNo(request.getTransactionSn());
        req.setOutRefundNo(request.getRequestRefundSn());
        req.setRefundId(request.getOutTransactionRefundSn());
        try {
            WxPayRefundQueryResult response = wxPayService.refundQuery(req);
            if (log.isDebugEnabled()) {
                log.debug("微信退款查询结果，transactionNo = {}，响应：{}", request.getTransactionSn(), response);
            }
            AssertUtils.notEmpty(response.getRefundRecords(), "微信退款记录不存在");
            TransactionOrderRefundResponse result = new TransactionOrderRefundResponse();
            // TODO 处理多次退款的情况
            WxPayRefundQueryResult.RefundRecord refundRecord = response.getRefundRecords().get(0);
            return result.setTransactionSn(request.getTransactionSn())
                    .setTransactionRefundSn(refundRecord.getOutRefundNo())
                    .setOutTransactionRefundSn(refundRecord.getOutRefundNo())
                    .setRefundAmount(CurrencyIsoCode.CNY.of(refundRecord.getSettlementRefundFee()))
                    .setOrderAmount(CurrencyIsoCode.CNY.of(response.getTotalFee()))
                    .setTransactionState(transformTradeState(refundRecord.getRefundStatus()))
                    .setRawResponse(response);
        } catch (WxPayException exception) {
            throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR, String.format("查询微信交易退款异常，transactionNo = %s",
                    request.getTransactionSn()), exception);
        }
    }

    @Override
    public QueryTransactionOrderResponse onPaymentEvent(PaymentTransactionEventRequest request) {
        WxPayOrderNotifyResult notifyResult = verifyPaymentNotifyRequest(request);
        QueryTransactionOrderResponse result = new QueryTransactionOrderResponse();
        result.setOutTransactionSn(notifyResult.getTransactionId())
                .setTransactionSn(notifyResult.getOutTradeNo())
                .setOrderAmount(CurrencyIsoCode.CNY.of(notifyResult.getTotalFee()))
                .setBuyerPayAmount(CurrencyIsoCode.CNY.of(notifyResult.getSettlementTotalFee()))
                .setUseSandboxEnv(isUseSandboxEnv());
        if (isSuccessful(notifyResult.getReturnCode(), notifyResult.getResultCode())) {
            result.setTransactionState(PaymentTransactionState.COMPLETED);
        } else {
            result.setTransactionState(PaymentTransactionState.FAILED);
        }
        result.setRawResponse(notifyResult);
        return result;
    }

    @Override
    public TransactionOrderRefundResponse onRefundEvent(PaymentTransactionRefundEventRequest request) {
        // 验签
        WxPayRefundNotifyResult notifyResult = verifyRefundNotifyRequest(request);
        WxPayRefundNotifyResult.ReqInfo reqInfo = notifyResult.getReqInfo();
        TransactionOrderRefundResponse result = new TransactionOrderRefundResponse();
        result.setTransactionRefundSn(reqInfo.getOutRefundNo())
                .setOutTransactionRefundSn(reqInfo.getRefundId())
                .setOrderAmount(CurrencyIsoCode.CNY.of(reqInfo.getTotalFee()))
                .setRefundAmount(CurrencyIsoCode.CNY.of(reqInfo.getSettlementRefundFee()))
                .setRawResponse(notifyResult);
        if (isSuccessful(notifyResult.getReturnCode(), notifyResult.getResultCode())) {
            result.setTransactionState(Objects.equals(result.getOrderAmount(), result.getRefundAmount()) ? PaymentTransactionState.REFUNDED :
                    PaymentTransactionState.PARTIAL_REFUND);

        } else {
            result.setTransactionState(PaymentTransactionState.REFUND_FAILED);
        }
        return result;
    }

    @Override
    public Object getWebHookResponse(boolean isSuccessful) {
        return isSuccessful ? PAYMENT_RESULT_HANDLE_SUCCESS_RETURN_CONTENT :
                PAYMENT_RESULT_HANDLE_FAILURE_RETURN_CONTENT;
    }


    /**
     * @param tradeState 交易状态
     * @return PaymentTransactionState
     */
    private PaymentTransactionState transformTradeState(String tradeState) {
        if (tradeState.equals(WeChatTradeSate.SUCCESS.name())) {
            return PaymentTransactionState.COMPLETED;
        }
        if (tradeState.equals(WeChatTradeSate.REFUND.name())) {
            return PaymentTransactionState.WAIT_REFUND;
        }
        if (tradeState.equals(WeChatTradeSate.NOTPAY.name())) {
            return PaymentTransactionState.WAIT_PAY;
        }
        if (tradeState.equals(WeChatTradeSate.CLOSED.name())) {
            return PaymentTransactionState.CLOSED;
        }
        if (tradeState.equals(WeChatTradeSate.REVOKED.name())) {
            return PaymentTransactionState.CLOSED;
        }
        if (tradeState.equals(WeChatTradeSate.USERPAYING.name())) {
            return PaymentTransactionState.PAYING;
        }
        if (tradeState.equals(WeChatTradeSate.PAYERROR.name())) {
            return PaymentTransactionState.FAILED;
        }
        return PaymentTransactionState.UNKNOWN;
    }

    private boolean isSuccessful(String returnCode, String resultCode) {
        return SUCCESS_CODES.contains(StringUtils.trimToEmpty(returnCode).toUpperCase())
                && SUCCESS_CODES.contains(StringUtils.trimToEmpty(resultCode).toUpperCase());
    }

    /**
     * 验证支付通知
     *
     * @param request 支付通知参数
     * @return WxPayOrderNotifyResult
     */
    private WxPayOrderNotifyResult verifyPaymentNotifyRequest(PaymentTransactionEventRequest request) {
        try {
            WxPayOrderNotifyResult notifyResult = wxPayService.parseOrderNotifyResult(request.getRawRequest());
            boolean verifyResult = Objects.equals(config.getPartner(), notifyResult.getMchId())
                    && Objects.equals(request.getTransactionSn(), notifyResult.getOutTradeNo())
                    && Objects.equals(request.getOrderAmount(), notifyResult.getTotalFee());
            if (log.isDebugEnabled()) {
                log.debug("微信支付通知，transactionNo = {}，参数验证 = {}，通知内容 = {}", request.getTransactionSn(), verifyResult, notifyResult);
            }
            AssertUtils.isTrue(verifyResult, "参数验证失败");
            // 验签
            notifyResult.checkResult(this.wxPayService, config.getSignType(), false);
            return notifyResult;
        } catch (WxPayException exception) {
            throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR, "微信支付通知签名验证异常", exception);
        }
    }


    /**
     * 退款通知验签
     *
     * @param request 通知请求
     * @return WxPayRefundNotifyResult
     */
    private WxPayRefundNotifyResult verifyRefundNotifyRequest(PaymentTransactionRefundEventRequest request) {
        try {
            WxPayRefundNotifyResult notifyResult = wxPayService.parseRefundNotifyResult(request.getRawRequest());
            boolean verifyResult = Objects.equals(config.getPartner(), notifyResult.getMchId())
                    && Objects.equals(request.getTransactionRefundSn(), notifyResult.getReqInfo().getOutRefundNo());
            if (log.isDebugEnabled()) {
                log.debug("微信退款通知，transactionRefundNo = {}，参数验证 = {}，通知内容 = {}", request.getTransactionRefundSn(), verifyResult, notifyResult);
            }
            AssertUtils.isTrue(verifyResult, "参数验证失败");
            // 验签
            notifyResult.checkResult(wxPayService, config.getSignType(), false);
            return notifyResult;
        } catch (WxPayException exception) {
            throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR, "微信退款通知签名验证异常", exception);
        }
    }

    /**
     * @return 获取微信支付服务
     */
    private WxPayService buildWxPayService() {
        WxPayConfig payConfig = new WxPayConfig();
        payConfig.setAppId(StringUtils.trimToNull(config.getAppId()));
        payConfig.setMchId(StringUtils.trimToNull(config.getPartner()));
        payConfig.setMchKey(StringUtils.trimToNull(config.getPartnerSecret()));
        payConfig.setSubAppId(StringUtils.trimToNull(config.getSubAppId()));
        payConfig.setSubMchId(StringUtils.trimToNull(config.getSubMchId()));
        payConfig.setKeyPath(StringUtils.trimToNull(config.getKeyPath()));
        payConfig.setSignType(config.getSignType());
        // 可以指定是否使用沙箱环境
        payConfig.setUseSandboxEnv(isUseSandboxEnv());
        WxPayService result = new WxPayServiceImpl();
        result.setConfig(payConfig);
        return result;
    }

    protected boolean isUseSandboxEnv() {
        return config.isUseSandboxEnv();
    }

    static String normalizationBody(String description) {
        return StringUtils.abbreviate(description.replaceAll("[^0-9a-zA-Z\\u4e00-\\u9fa5 ]", ""), 600);
    }

    @NotNull
    static String getExpireTimeOrUseDefault(Duration expireTime) {
        if (expireTime == null) {
            expireTime = Duration.ofMinutes(30);
        }
        return DateFormatUtils.format(new Date(System.currentTimeMillis() + expireTime.toMillis()), DATE_FORMAT_PATTERN);
    }
}
