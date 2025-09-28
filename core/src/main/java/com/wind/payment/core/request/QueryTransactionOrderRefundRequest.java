package com.wind.payment.core.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询交易订单退款
 *
 * @author wuxp
 * @date 2023-09-30 19:52
 **/
@Data
public class QueryTransactionOrderRefundRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 5498627853961301700L;

    /**
     * 应用内的交易流水号
     */
    private String transactionSn;

    /**
     * 第三方交易流水号
     */
    private String outTransactionSn;

    /**
     * 交易退款流水号
     */
    private String requestRefundSn;

    /**
     * 第三方退款流水号
     */
    private String outTransactionRefundSn;
}
