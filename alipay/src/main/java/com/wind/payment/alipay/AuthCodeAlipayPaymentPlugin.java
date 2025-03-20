package com.wind.payment.alipay;

import com.alibaba.fastjson2.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.domain.AlipayTradePayModel;
import com.alipay.api.request.AlipayTradePayRequest;
import com.alipay.api.response.AlipayTradePayResponse;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.payment.alipay.request.AliPayAuthCodePaymentRequest;
import com.wind.payment.core.PaymentTransactionException;
import com.wind.payment.core.request.PrePaymentOrderRequest;
import com.wind.payment.core.response.PrePaymentOrderResponse;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付宝授权码支付
 * 参见：https://opendocs.alipay.com/open/194/106039
 *
 * @author wuxp
 * @date 2023-10-01 17:37
 **/
@Slf4j
public class AuthCodeAlipayPaymentPlugin extends AbstractAlipayPaymentPlugin {

    /**
     * 产品代码
     */
    private static final String ALI_FACE_PAY_PRODUCT_CODE = "FACE_TO_FACE_PAYMENT";

    public AuthCodeAlipayPaymentPlugin(String config) {
        this(JSON.parseObject(config, AliPayPartnerConfig.class));
    }

    public AuthCodeAlipayPaymentPlugin(AliPayPartnerConfig config) {
        super(config);
    }

    @Override
    public PrePaymentOrderResponse preOrder(PrePaymentOrderRequest request) {
        AliPayAuthCodePaymentRequest authCodeRequest = (AliPayAuthCodePaymentRequest) request;
        AlipayTradePayRequest req = new AlipayTradePayRequest();
        AlipayTradePayModel model = new AlipayTradePayModel();
        model.setProductCode(ALI_FACE_PAY_PRODUCT_CODE);
        model.setBody(normalizationBody(request.getDescription()));
        model.setTimeExpire(getExpireTimeOrUseDefault(request.getExpireTime()));
        model.setSubject(request.getSubject());
        model.setTotalAmount(request.getOrderAmount().fen2Yuan().toString());
        model.setOutTradeNo(request.getTransactionSn());
        model.setScene(authCodeRequest.getScene());
        model.setAuthCode(authCodeRequest.getAuthCode());
        req.setBizModel(model);
        req.setNotifyUrl(request.getAsynchronousNotificationUrl());
        req.setReturnUrl(request.getSynchronousCallbackUrl());
        if (log.isDebugEnabled()) {
            log.debug("支付请求参数：{}", req);
        }
        PrePaymentOrderResponse result = new PrePaymentOrderResponse();
        try {
            AlipayTradePayResponse response = getAlipayClient().execute(req);
            if (log.isDebugEnabled()) {
                log.debug("支付响应 :{}", response);
            }
            if (response.isSuccess()) {
                result.setTransactionSn(response.getOutTradeNo())
                        .setOutTransactionSn(response.getTradeNo())
                        .setUseSandboxEnv(this.isUseSandboxEnv())
                        .setOrderAmount(CurrencyIsoCode.CNY.ofText(response.getTotalAmount()))
                        .setRawResponse(response);
            } else {
                throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR, String.format("支付宝授权码支付交易失败，transactionNo = %s。" +
                        ERROR_PATTERN, request.getTransactionSn(), response.getCode(), response.getMsg()));
            }
        } catch (AlipayApiException exception) {
            throw new PaymentTransactionException(DefaultExceptionCode.COMMON_ERROR, String.format("支付宝授权码支付交易异常，transactionNo = %s。",
                    request.getTransactionSn()), exception);
        }
        return result;
    }

}
