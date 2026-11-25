package org.dromara.common.json.handler;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.ser.jdk.NumberSerializer;

import java.math.BigInteger;

/**
 * 超出 JS 最大最小值 处理
 *
 * @author Lion Li
 */
@JacksonStdImpl
public class BigNumberSerializer extends NumberSerializer {

    /**
     * 提供实例
     */
    public static final BigNumberSerializer INSTANCE = new BigNumberSerializer(Number.class);
    /**
     * 根据 JS Number.MAX_SAFE_INTEGER 与 Number.MIN_SAFE_INTEGER 得来
     */
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;
    /**
     * JavaScript 最小安全整数。
     */
    private static final long MIN_SAFE_INTEGER = -9007199254740991L;


    public BigNumberSerializer(Class<? extends Number> rawType) {
        super(rawType);
    }

    @Override
    public void serialize(Number value, JsonGenerator gen, SerializationContext provider) {
        boolean safe = value instanceof BigInteger integer
            ? integer.abs().compareTo(BigInteger.valueOf(MAX_SAFE_INTEGER)) <= 0
            : value.longValue() >= MIN_SAFE_INTEGER && value.longValue() <= MAX_SAFE_INTEGER;
        if (safe) {
            super.serialize(value, gen, provider);
        } else {
            gen.writeString(value.toString());
        }
    }
}
