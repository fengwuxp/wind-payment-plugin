package com.wind.payment.core.request;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 预下单支付请求
 *
 * @author wuxp
 * @date 2023-09-30 19:27
 **/
@Data
public class PrePaymentOrderRequest implements Serializable {

    private static final long serialVersionUID = 6802966138298876457L;

    /**
     * 应用内的交易流水号
     */
    @NotBlank
    private String transactionSn;

    /**
     * 支付用户标识
     */
    @NotBlank
    private String userId;

    /**
     * 支付金额，单位分
     */
    @NotNull
    @Min(value = 1)
    private Integer orderAmount;

    /**
     * 同步通知(回调) url
     */
    @NotBlank
    private String synchronousCallbackUrl;

    /**
     * 异步通知 url
     */
    @NotBlank
    private String asynchronousNotificationUrl;

    /**
     * 交易结束时间
     * 使用阿里规则
     * 1m～15d。m-分钟，h-小时，d-天，1c-当天（1c-当天的情况下，无论交易何时创建，都在0点关闭）
     * <p>
     * 默认：30m，  30分钟过期
     */
    @NotBlank
    private String expireTime = "30m";

    /**
     * 支付说明
     */
    private String description;

    /**
     * 支付请求发起方 ip
     */
    private String requestSourceIp;

    /**
     * 商品展示 url
     */
    private String productShowUrl;

    /**
     * 主题
     */
    private String subject;

    /**
     * 支付场景说明
     */
    private String sceneInfo;
}
