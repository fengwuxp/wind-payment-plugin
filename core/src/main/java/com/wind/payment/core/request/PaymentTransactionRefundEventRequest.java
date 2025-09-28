package com.wind.payment.core.request;

import com.wind.transaction.core.Money;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * 支付交易退款通知请求对象
 *
 * @author wuxp
 * @date 2023-10-01 13:43
 **/
@Data
public class PaymentTransactionRefundEventRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 7026788394092470016L;

    /**
     * 应用内的交易退款流水号
     */
    @NotBlank
    private String transactionRefundSn;

    /**
     * 订单总金额
     * 单位：分
     */
    @NotNull
    private Money orderAmount;

    /**
     * 退款金额
     * 单位：分
     */
    @NotNull
    @Min(value = 1)
    private Money refundAmount;

    /**
     * 原始的通知请求参数
     */
    private Object rawRequest;

    @SuppressWarnings("unchecked")
    public <T> T getRawRequest() {
        return (T) rawRequest;
    }
}
