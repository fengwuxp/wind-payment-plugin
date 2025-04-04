package com.wind.payment.core.response;

import com.wind.payment.core.enums.PaymentTransactionState;
import com.wind.transaction.core.Money;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Objects;

/**
 * 支付订单退款响应
 *
 * @author wuxp
 * @date 2023-09-30 19:49
 **/
@Data
@Accessors(chain = true)
public class TransactionOrderRefundResponse implements Serializable {

    private static final long serialVersionUID = -8644745208933551366L;

    /**
     * 交易状态
     */
    private PaymentTransactionState transactionState;

    /**
     * 退款的金额
     * 单位：分
     */
    private Money refundAmount;

    /**
     * 订单金额
     * 单位：分
     */
    private Money orderAmount;

    /**
     * 应用内的交易流水号
     */
    private String transactionSn;

    /**
     * 应用内的交易退款流水号
     */
    private String transactionRefundSn;

    /**
     * 第三方退款流水号
     */
    private String outTransactionRefundSn;

    /**
     * 原始响应
     */
    private Object rawResponse;

    /**
     * @return 是否全额退款
     */
    public boolean isFullRefund() {
        return Objects.equals(orderAmount, refundAmount);
    }

    @SuppressWarnings("unchecked")
    public <T> T getRawResponse() {
        return (T) rawResponse;
    }
}
