package com.wind.payment.core.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Duration;

/**
 * 预下单支付请求
 *
 * @author wuxp
 * @date 2023-09-30 19:27
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PrePaymentOrderRequest extends AbstractPaymentContextVariables implements Serializable {

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
     * 订单有效期
     * 默认：15 分钟
     */
    @NotBlank
    private Duration expireTime = Duration.ofMinutes(15);

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
     * 订单主题
     */
    private String subject;

    /**
     * 支付场景说明
     */
    private String sceneInfo;


}
