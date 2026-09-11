package com.yaonan.util.lang;

import java.math.BigDecimal;
import java.util.Random;

/**
 * 数学工具类：提供四舍五入与随机数生成等常用数学运算。
 */
public class MathUtil {
    /**
     * 将 BigDecimal 四舍五入到指定小数位。
     *
     * @param val   数值
     * @param scale 保留小数位数
     * @return 处理后的数值（val 为 null 时返回 null）
     */
    public static BigDecimal round(BigDecimal val, int scale) {
        if (val == null) return null;
        return val.setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 将 Double 四舍五入到指定小数位。
     *
     * @param val   数值
     * @param scale 保留小数位数
     * @return 处理后的数值
     */
    public static BigDecimal round(Double val, int scale) {
        return new BigDecimal(val).setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 生成一个随机整数。
     *
     * @return 随机整数
     */
    public static int random() {
        return new Random().nextInt();
    }

    /**
     * 生成 [0, upperBound) 范围内的随机整数。
     *
     * @param upperBound 上界（不含）
     * @return 随机整数
     */
    public static int random(int upperBound) {
        return new Random().nextInt(upperBound);
    }

    /**
     * 生成 [lowerBound, upperBound) 范围内的随机整数。
     *
     * @param lowerBound 下界（含）
     * @param upperBound 上界（不含）
     * @return 随机整数
     */
    public static int random(int lowerBound, int upperBound) {
        Random random = new Random();
        // nextInt(n) 返回 [0, n)，加上下界后得到 [lowerBound, upperBound)
        return random.nextInt(upperBound - lowerBound) + lowerBound;
    }

    /**
     * 生成 [0.0, 1.0) 范围内的随机浮点数。
     *
     * @return 随机浮点数
     */
    public static double randomDouble() {
        return new Random().nextDouble();
    }
}
