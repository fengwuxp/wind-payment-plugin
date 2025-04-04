package com.wind.payment.alipay;

import com.alibaba.fastjson2.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.payment.alipay.response.AliPayQrCodeTransactionPayResult;
import com.wind.payment.core.PaymentTransactionException;
import com.wind.payment.core.request.PrePaymentOrderRequest;
import com.wind.payment.core.response.PrePaymentOrderResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 二维码支付
 * 参见：https://opendocs.alipay.com/open/194/106078
 *
 * @author wuxp
 * @date 2023-10-01 18:20
 **/
@Slf4j
public class QrCodeAlipayPaymentPlugin extends AbstractAlipayPaymentPlugin {

    public QrCodeAlipayPaymentPlugin(String config) {
        this(JSON.parseObject(config, AliPayPartnerConfig.class));
    }

    public QrCodeAlipayPaymentPlugin(AliPayPartnerConfig config) {
        super(config);
    }

    @Override
    public PrePaymentOrderResponse preOrder(PrePaymentOrderRequest request) {
        AlipayTradePrecreateRequest req = new AlipayTradePrecreateRequest();
        AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
        model.setOutTradeNo(request.getTransactionSn());
        model.setBody(normalizationBody(request.getDescription()));
        model.setTimeExpire(getExpireTimeOrUseDefault(request.getValidityDuration()));
        model.setSubject(request.getSubject());
        model.setTotalAmount(request.getOrderAmount().fen2Yuan().toString());
        req.setBizModel(model);
        req.setNotifyUrl(request.getAsynchronousNotificationUrl());
        req.setReturnUrl(request.getSynchronousCallbackUrl());
        if (log.isDebugEnabled()) {
            log.debug("支付请求参数：{}", req);
        }
        PrePaymentOrderResponse result = new PrePaymentOrderResponse();
        try {

            AlipayTradePrecreateResponse response = getAlipayClient().execute(req);
            if (log.isDebugEnabled()) {
                log.debug("支付响应 :{}", response);
            }
            if (response.isSuccess()) {
                result.setTransactionSn(response.getOutTradeNo())
                        .setUseSandboxEnv(this.isUseSandboxEnv())
                        .setOrderAmount(request.getOrderAmount())
                        .setResult(new AliPayQrCodeTransactionPayResult(response.getQrCode(), response.getOutTradeNo()))
                        .setRawResponse(response);
            } else {
                throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR, String.format("支付宝二维码支付交易失败，transactionNo = %s。" +
                        ERROR_PATTERN, request.getTransactionSn(), response.getCode(), response.getMsg()));
            }

        } catch (AlipayApiException exception) {
            throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR, String.format("支付宝二维码支付交易异常，transactionNo = %s。",
                    request.getTransactionSn()), exception);
        }

        return result;
    }
}
