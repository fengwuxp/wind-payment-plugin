package com.wind.payment.core.request;

import com.wind.transaction.core.Money;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 交易订单退款
 *
 * @author wuxp
 * @date 2023-09-30 19:47
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TransactionOrderRefundRequest extends AbstractPaymentContextVariables implements Serializable {

    @Serial
    private static final long serialVersionUID = 8514897252783130486L;

    /**
     * 应用内的交易流水号
     */
    @NotBlank
    private String transactionSn;

    /**
     * 第三方交易流水号
     */
    @NotBlank
    private String outTransactionSn;

    /**
     * 应用内的交易退款流水号
     */
    @NotBlank
    private String transactionRefundSn;

    /**
     * 退款金额
     * 单位：分
     */
    @NotNull
    @Min(value = 1)
    private Money refundAmount;

    /**
     * 订单总金额
     * 单位：分
     */
    @NotNull
    @Min(value = 1)
    private Money orderAmount;

    /**
     * 退款异步通知 url
     */
    @NotBlank
    private String asynchronousNotificationUrl;

    /**
     * 退款原因
     */
    private String refundReason;
}
