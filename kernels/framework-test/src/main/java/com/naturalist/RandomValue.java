package com.naturalist;

import java.math.BigDecimal;
import java.util.UUID;

public class RandomValue {

    public static String string() {
        return UUID.randomUUID().toString();
    }

    public static String string(int length) {
        return UUID.randomUUID().toString().substring(0, length);
    }

    public static Integer integer() {
        return ((Double) Math.random()).intValue();
    }

    public static BigDecimal bigDecimal() {
        return BigDecimal.valueOf(Math.random());
    }
}
