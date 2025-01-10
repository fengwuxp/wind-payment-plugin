package com.wind.payment.core;

import com.wind.payment.core.request.PaymentTransactionEventRequest;
import com.wind.payment.core.request.PaymentTransactionRefundNoticeRequest;
import com.wind.payment.core.response.QueryTransactionOrderResponse;
import com.wind.payment.core.response.TransactionOrderRefundResponse;

/**
 * 支付交易回调处理
 *
 * @author wuxp
 * @date 2023-10-01 13:40
 **/
public interface PaymentTransactionWebHooker {


    /**
     * 支付通知
     *
     * @param request 支付通知请求参数
     * @return 处理响应
     */
    QueryTransactionOrderResponse paymentEvent(PaymentTransactionEventRequest request);

    /**
     * 退款通知
     *
     * @param request 退款通知请求参数
     * @return 处理响应
     */
    TransactionOrderRefundResponse refundEvent(PaymentTransactionRefundNoticeRequest request);

    /**
     * 获取通知处理响应
     *
     * @param isSuccessful 是否处理成功
     * @return 处响应
     */
    Object getWebHookResponse(boolean isSuccessful);

}
