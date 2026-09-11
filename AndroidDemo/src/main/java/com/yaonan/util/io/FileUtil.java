package com.yaonan.util.io;

import com.yaonan.util.lang.MathUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件操作工具类
 *
 * 私有外部储存不用授权
 * new File(App.getApp().getExternalFilesDir(null).getParent(), fileName)
 *
 * Created by yaonan on 2021/2/1.
 */
public class FileUtil {
    /**
     * 根据byte数组生成文件
     * @param bytes 文件字节数组
     * @param filePath 文件路径
     * @throws IOException
     */
    public static void byte2File(byte[] bytes, String filePath) throws IOException {
        byte2File(bytes, new File(filePath));
    }

    /**
     * 根据byte数组生成文件
     * @param bytes 文件字节数组
     * @param file 文件
     * @throws IOException
     */
    public static void byte2File(byte[] bytes, File file) throws IOException {
        File dir = file.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) { // 判断文件目录是否存在
            throw new IOException("目录" + dir + "创建失败");
        }

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            bos.write(bytes);
        }
    }

    /**
     * 获得指定文件的字节数组
     * @param filePath
     * @return
     * @throws IOException
     */
    public static byte[] file2Byte(String filePath) throws IOException {
        return file2Byte(new File(filePath));
    }

    /**
     * 获得指定文件的字节数组
     * @param file
     * @return
     * @throws IOException
     */
    public static byte[] file2Byte(File file) throws IOException {
        if (file.isDirectory()) {
            throw new IOException(file + "为文件夹");
        }
        if (!file.exists()) {
            throw new FileNotFoundException("文件" + file + "不存在");
        }

        int buff_size = 1024;
        byte[] buffer = new byte[buff_size]; // 缓冲区
        int len; // 本次读取到的字节
        try (
            ByteArrayOutputStream baos = new ByteArrayOutputStream((int) file.length()); // 改用byte[]会快很多
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        ) {
            while ((len = bis.read(buffer, 0, buff_size)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }

    /**
     * 文件大小转换换算
     * @param size 字节数
     * @return
     */
    public static String formatSize(long size) {
        long kb = 1024;
        long mb = 1024 * 1024;
        long gb = 1024 * 1024 * 1024;

        if (size < kb) {
            return size + " B";
        } else if (size < mb) {
            return MathUtil.round((double) size / kb, 0) + " K";
        } else if (size < gb) {
            return MathUtil.round((double) size / mb, 2) + " M";
        } else {
            return MathUtil.round((double) size / gb, 2) + " G";
        }
    }

    /**
     * 将输入流完整读取为字节数组（适用于小数据量流，读取完毕后由调用方负责关闭输入流）。
     *
     * @param input 输入流
     * @return 流中全部内容的字节数组
     * @throws IOException 读取失败时抛出
     */
    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024 * 4];
        int n = 0;
        // 循环读取直到流末尾（read 返回 -1）
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }

        return output.toByteArray();
    }
}
