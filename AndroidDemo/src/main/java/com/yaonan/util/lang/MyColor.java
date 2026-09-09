package com.yaonan.util.lang;

public class MyColor {
    public int r, g, b;

    public MyColor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int getRGB() {
        return ((255 & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                ((b & 0xFF) << 0);
    }

    public static MyColor decode(String nm) {
        Integer intval = Integer.decode(nm);
        int i = intval.intValue();
        return new MyColor((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
    }

    public static String encode(MyColor color) {
        return "#" + Integer.toHexString(color.getRGB()).substring(2);
    }

    @Override
    public String toString() {
        return "[r=" + r + ",g=" + g + ",b=" + b + "]";
    }
}
