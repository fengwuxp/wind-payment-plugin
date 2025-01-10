package com.wind.payment.core.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用支付交易状态
 *
 * @author wuxp
 * @date 2023-10-01 09:11
 **/
@AllArgsConstructor
@Getter
public enum PaymentTransactionState implements DescriptiveEnum {

    /**
     * 已关闭
     */
    CLOSED("已关闭"),

    /**
     * 待支付
     */
    WAIT_PAY("待支付"),

    /**
     * 支付中
     */
    PAYING("支付中"),

    /**
     * 支付成功
     */
    COMPLETED("支付成功"),

    /**
     * 支付失败
     */
    FAILED("支付失败"),

    /**
     * 退款等待中
     */
    WAIT_REFUND("等待退款"),

    /**
     * 部分退款
     */
    PARTIAL_REFUND("部分退款"),

    /**
     * 已退款
     */
    REFUNDED("已退款"),

    /**
     * 退款失败
     */
    REFUND_FAILED("退款失败"),

    /**
     * 未知状态
     */
    UNKNOWN("未知");

    private final String desc;

}
