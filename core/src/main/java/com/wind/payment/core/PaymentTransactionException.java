package com.wind.payment.core;

import com.wind.common.exception.BaseException;
import com.wind.common.exception.ExceptionCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 交易相关异常
 *
 * @author wuxp
 * @date 2023-09-30 20:05
 **/
@Getter
public class PaymentTransactionException extends BaseException {

    @Serial
    private static final long serialVersionUID = 3450136622980599458L;

    private final String requestId;

    public PaymentTransactionException(String message) {
        this(message, null);
    }

    public PaymentTransactionException(String message, String requestId) {
        super(message);
        this.requestId = requestId;
    }

    public PaymentTransactionException(ExceptionCode code, String message) {
        this(code, message, null, null);
    }

    public PaymentTransactionException(ExceptionCode code, String message, String requestId) {
        this(code, message, requestId, null);
    }

    public PaymentTransactionException(ExceptionCode code, String message, Throwable throwable) {
        this(code, message, null, throwable);
    }

    public PaymentTransactionException(ExceptionCode code, String message, String requestId, Throwable cause) {
        super(code, message, cause);
        this.requestId = requestId;
    }
}
