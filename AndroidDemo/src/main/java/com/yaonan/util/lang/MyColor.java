package com.yaonan.util.lang;

/**
 * 自定义颜色类：以 R、G、B 三个分量表示颜色，并提供与整型 ARGB 值、十六进制字符串的互转能力。
 */
public class MyColor {
    /** 红色分量（0-255）。 */
    public int r, g, b;

    /**
     * 通过三个颜色分量构造。
     *
     * @param r 红色分量
     * @param g 绿色分量
     * @param b 蓝色分量
     */
    public MyColor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /**
     * 将颜色转为 ARGB 整型值。
     *
     * @return ARGB 颜色值
     */
    public int getRGB() {
        // 依次拼装 alpha、R、G、B 四个通道
        return ((255 & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                ((b & 0xFF) << 0);
    }

    /**
     * 解析颜色字符串（支持 "#RRGGBB"、"0xRRGGBB" 等常见写法）。
     *
     * @param nm 颜色字符串
     * @return 解析出的颜色对象
     */
    public static MyColor decode(String nm) {
        Integer intval = Integer.decode(nm);
        int i = intval.intValue();
        // 依次取出 R、G、B 三个字节
        return new MyColor((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
    }

    /**
     * 将颜色编码为 "#RRGGBB" 形式的字符串。
     *
     * @param color 颜色对象
     * @return 十六进制颜色字符串
     */
    public static String encode(MyColor color) {
        // getRGB 前 8 位为 alpha，跳过前两位后得到 RRGGBB
        return "#" + Integer.toHexString(color.getRGB()).substring(2);
    }

    @Override
    public String toString() {
        return "[r=" + r + ",g=" + g + ",b=" + b + "]";
    }
}
