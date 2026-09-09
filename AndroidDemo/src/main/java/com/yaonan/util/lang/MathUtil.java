package com.yaonan.util.lang;

import java.math.BigDecimal;
import java.util.Random;

public class MathUtil {
    public static BigDecimal round(BigDecimal val, int scale) {
        if (val == null) return null;
        return val.setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal round(Double val, int scale) {
        return new BigDecimal(val).setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    public static int random() {
        return new Random().nextInt();
    }

    public static int random(int upperBound) {
        return new Random().nextInt(upperBound);
    }

    public static int random(int lowerBound, int upperBound) {
        Random random = new Random();
        return random.nextInt(upperBound - lowerBound) + lowerBound;
    }

    public static double randomDouble() {
        return new Random().nextDouble();
    }
}
