package com.wind.payment.core.request;

import com.wind.core.WritableContextVariables;

import jakarta.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wuxp
 * @date 2025-03-19 09:59
 **/
public abstract class AbstractPaymentContextVariables implements WritableContextVariables {

    /**
     * 上下文变量
     */
    private Map<String, Object> contextVariables = new HashMap<>();

    @Override
    public WritableContextVariables putVariable(@NotBlank String name, Object val) {
        contextVariables.put(name, val);
        return this;
    }

    @Override
    public Map<String, Object> getContextVariables() {
        return Collections.unmodifiableMap(contextVariables);
    }

    @Override
    public WritableContextVariables removeVariable(@NotBlank String name) {
        contextVariables.remove(name);
        return this;
    }
}
