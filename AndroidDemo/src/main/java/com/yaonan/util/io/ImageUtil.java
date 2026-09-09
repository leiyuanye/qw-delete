package com.yaonan.util.io;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.yaonan.util.function.Tuple2;
import com.yaonan.util.function.Tuple5;
import com.yaonan.util.lang.MyColor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;

public class ImageUtil {
    /**
     * 获取图片类型
     * jpg png gif bmp webp ico
     * @param bytes
     * @return
     */
    public static Tuple2<String, String> getImageType(byte[] bytes) {
        int b0 = bytes[0] & 0xFF; // & 0xff 无符号byte转无符号int
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        int b3 = bytes[3] & 0xFF;

        int b4 = bytes[4] & 0xFF;
        int b5 = bytes[5] & 0xFF;
        int b6 = bytes[6] & 0xFF;
        int b7 = bytes[7] & 0xFF;

        int b8 = bytes[8] & 0xFF;
        int b9 = bytes[9] & 0xFF;
        int b10 = bytes[10] & 0xFF;
        int b11 = bytes[11] & 0xFF;

        if (b0 == 0xFF && b1 == 0xD8) {
            return new Tuple2<>("image/jpeg", "jpg");
        }
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47 &&
                b4 == 0x0D && b5 == 0x0A && b6 == 0x1A && b7 == 0x0A) {
            return new Tuple2<>("image/png", "png");
        }
        if (b0 == 'G' && b1 == 'I' && b2 == 'F') {
            return new Tuple2<>("image/gif", "gif");
        }
        if (b0 == 'B' && b1 == 'M') {
            return new Tuple2<>("image/bmp", "bmp");
        }
        if (b0 == 'R' && b1 == 'I' && b2 == 'F' && b3 == 'F' &&
                b8 == 'W' && b9 == 'E' && b10 == 'B' && b11 == 'P') {
            return new Tuple2<>("image/webp", "webp");
        }
        if (b0 == 0 && b1 == 0 && b2 == 1 && b3 == 0) { // 0010 0020
            return new Tuple2<>("image/x-icon", "ico");
        }

        return new Tuple2<>("image/unknown", "");
    }

    /**
     * BufferedImage转byte
     * jpg png
     * @param image
     * @param imageType
     * @return
     */
    public static byte[] imageToByte(Bitmap image, String imageType) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        if ("png".equalsIgnoreCase(imageType)) {
            image.compress(Bitmap.CompressFormat.PNG, 100, os);
        } else {
            image.compress(Bitmap.CompressFormat.JPEG, 100, os);
        }
        image.recycle();
        return os.toByteArray();
    }

    /**
     * byte转BufferedImage
     * @param bytes
     * @return
     */
    public static Bitmap byteToImage(byte[] bytes) {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * res转BufferedImage
     * Bitmap queryImage = BitmapFactory.decodeResource(resources, id)
     * @param is resources.openRawResource(id)
     * @return
     */
    public static Bitmap byteToImage(InputStream is) {
        return BitmapFactory.decodeStream(is);
    }

    /**
     * 图片转颜色数组[行][列]
     * @param image
     * @return
     */
    public static MyColor[][] image2Array(Bitmap image) {
        int height = image.getHeight();
        int width = image.getWidth();
        MyColor[][] array = new MyColor[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = image.getPixel(x, y);
                int r = ((color >> 16) & 0xff);
                int g = ((color >>  8) & 0xff);
                int b = ((color      ) & 0xff);
                array[y][x] = new MyColor(r, g, b);
            }
        }
        return array;
    }

    private static Bitmap _scale(Bitmap image, int scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(1f / scale, 1f / scale);
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
    }

    /**
     * 图片比对
     * @param originalImage
     * @param queryImage
     * @param scale 尺寸宽高先压缩到一半，再比较，提高速度；用于大区域中找大图；业务代码逻辑最好还是设计成：小区域中找小图
     * @return
     */
    public static Tuple5<Integer, Integer, Double, Double, Double> cvMatchTemplate(
            Bitmap originalImage, Bitmap queryImage, int scale) {

        if (scale > 1) {
            originalImage = _scale(originalImage, scale);
            queryImage = _scale(queryImage, scale);
        }

        MyColor[][] originalColors = image2Array(originalImage); // [行][列]
        MyColor[][] queryColors = image2Array(queryImage);

        int originalRowLength = originalColors.length;
        int originalColLength = originalColors[0].length;
        int queryRowLength = queryColors.length;
        int queryColLength = queryColors[0].length;

        int minX = 0; // 最小误差x坐标，左上角0
        int minY = 0; // 最小误差y坐标，左上角0
        double minValue = Double.MAX_VALUE; // 最小误差值
        double prevMinValue = Double.MAX_VALUE; // 第二小误差值

        AAA:
        for (int originalRow = 0; originalRow <= originalRowLength - queryRowLength; originalRow++) {
            for (int originalCol = 0; originalCol <= originalColLength - queryColLength; originalCol++) {
                // 该像素与query图片匹配误差值
                double totalValue = 1d; // 该像素总误差
                BBB:
                for (int queryRow = 0; queryRow < queryRowLength; queryRow++) {
                    for (int queryCol = 0; queryCol < queryColLength; queryCol++) {
                        MyColor queryColor = queryColors[queryRow][queryCol];
                        MyColor originalColor = originalColors[originalRow + queryRow][originalCol + queryCol];

                        totalValue +=
                                Math.abs(queryColor.r - originalColor.r) +
                                Math.abs(queryColor.g - originalColor.g) +
                                Math.abs(queryColor.b - originalColor.b);

                        if (totalValue >= minValue) { // 误差已过大，提前跳出提高性能
                            break BBB;
                        }
                    }
                }

                if (totalValue < minValue) {
                    minX = originalCol + queryColLength / 2;
                    minY = originalRow + queryRowLength / 2;
                    prevMinValue = minValue;
                    minValue = totalValue;

                    if (totalValue < 1.1) { // 误差最小1，一模一样直接就返回了
                        break AAA;
                    }
                }
            }
        }

        // 匹配度[0-1]，越大越匹配
        double upperLimit = 255.0 * 3 * queryRowLength * queryColLength; // 理论最大误差值
        double match = 1 - (minValue - 1) / upperLimit;
        match = new BigDecimal(match).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();

        return new Tuple5<>(minX * scale, minY * scale, minValue, prevMinValue, match);
    }
}
