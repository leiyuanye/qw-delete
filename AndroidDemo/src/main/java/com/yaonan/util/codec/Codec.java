package com.yaonan.util.codec;

import android.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaonan.util.json.JsonParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.zip.CRC32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 编码encode、加密encrypt、转义escape、序列化serialize，工具类
 *
 * |------------------------------------------|
 * | 单向加密　 | MD5（签名） SHA（签名） SM3 |
 * |------------+-----------------------------|
 * | 对称加密　 | DES（加密） AES（加密） SM4 |
 * |------------+-----------------------------|
 * | 非对称加密 | RSA（加密） DSA（签名） SM2 |
 * |------------------------------------------|
 *
 * 加密方法的参数和返回值原始类型基本都是byte[]，实际数据传递会转为十六进制字符串、base64字符串等
 *
 * 多系统使用对称密钥加密，需要扩展appid与密钥的一一对应关系表，每个厂商发一个appid及对应secret
 * 厂商请求：{"appid":"","data":encrypt(secretKey, 实际请求数据JSON)}
 * 解析厂商请求JSON：根据请求JSON的appid值获取对应的密钥secretKey，decrypt(secretKey, 请求JSON.data)获得解密的JSON
 *
 * Created by yaonan on 2022/1/22.
 */
public class Codec {

    /** Jackson 序列化/反序列化核心对象，用于 JSON 编解码。 */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /* *************************************************** 单向加密 ***************************************************/

    /**
     * 字符串md5加密，32位小写
     * @param str
     * @return 十六进制字符串
     */
    public static String md5(String str) {
        return md5(str.getBytes());
    }

    /**
     * 字节md5加密，32位小写
     * @param bytes
     * @return 十六进制字符串
     */
    public static String md5(byte[] bytes) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] result = md.digest(bytes);
        return byte2hex(result);
    }

    /**
     * 文件md5加密，32位小写
     * @param file
     * @return 十六进制字符串
     * @throws IOException
     */
    public static String md5(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("文件" + file + "不存在");
        }

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }

        int buff_size = 1024;
        byte[] buffer = new byte[buff_size];
        int len;
        try (FileInputStream fis = new FileInputStream(file)) {
            while ((len = fis.read(buffer, 0, buff_size)) != -1) {
                md.update(buffer, 0, len);
            }
        }

        byte[] result = md.digest();
        return byte2hex(result);
    }

    /**
     * 字符串sha1加密
     * @param str
     * @return 十六进制字符串
     */
    public static String sha1(String str) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] result = md.digest(str.getBytes());
        return byte2hex(result);
    }

    /**
     * 字符串sha256加密
     * @param str
     * @return 十六进制字符串
     */
    public static String sha256(String str) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] result = md.digest(str.getBytes());
        return byte2hex(result);
    }

    /**
     * 循环冗余校验
     * @param bytes
     * @return
     */
    public static long crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    /**
     * 字符串HmacSHA256加密
     * @param secretKey
     * @param str
     * @return base64字符串
     * @throws InvalidKeyException
     */
    public static String hmacSha256(String secretKey, String str)
            throws InvalidKeyException {

        final String s = "HmacSHA256";
        Mac mac;
        try {
            mac = Mac.getInstance(s);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }

        byte[] keyBytes = secretKey.getBytes();
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, 0, keyBytes.length, s);
        mac.init(keySpec);

        byte[] bytes = mac.doFinal(str.getBytes());
        return Codec.base64_encode(bytes);
    }

    /* ***************************************************** 编码 *****************************************************/

    /**
     * base64编码
     * @param bytes
     * @return
     */
    public static String base64_encode(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
        //return new BASE64Encoder().encode(bytes).replaceAll("\n", "").replaceAll("\r", ""); // 删除 \r\n
    }

    /**
     * base64解码
     * @param base64
     * @return
     */
    public static byte[] base64_decode(String base64) {
        return Base64.decode(base64, Base64.NO_WRAP);
        //return new BASE64Decoder().decodeBuffer(base64);
    }

    /**
     * url编码，即encodeURIComponent
     * @param url
     * @return
     */
    public static String url_encode(String url) {
        return url_encode(url, CharsetUtil.UTF_8_CHARSET);
    }

    /**
     * url编码，即encodeURIComponent
     * @param url
     * @param charset
     * @return
     */
    public static String url_encode(String url, Charset charset) {
        try {
            return URLEncoder.encode(url, charset.toString());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * url解码，即decodeURIComponent
     * @param url
     * @return
     */
    public static String url_decode(String url) {
        return url_decode(url, CharsetUtil.UTF_8_CHARSET);
    }

    /**
     * url解码，即decodeURIComponent
     * @param url
     * @param charset
     * @return
     */
    public static String url_decode(String url, Charset charset) {
        try {
            return URLDecoder.decode(url, charset.toString());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 对象转json字符串，jackson内核
     * @param object
     * @return
     */
    public static String json_encode(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonParseException(e);
        }
    }

    /**
     * json美化
     * @param object
     * @return
     */
    public static String json_encode_pretty(Object object) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonParseException(e);
        }
    }

    /**
     * json字符串转对象，jackson内核
     * @param jsonStr
     * @param valueType
     * @param <T> Map或List
     * @return
     */
    public static <T> T json_decode(String jsonStr, Class<T> valueType) {
        try {
            return objectMapper.readValue(jsonStr, valueType);
        } catch (JsonProcessingException e) {
            throw new JsonParseException(e);
        }
    }

    /**
     * 实体类转Map
     * @param entity
     * @return
     */
    public static Map json_convert(Object entity) {
        // 切换为时间类型转为时间字符串
        //objectMapper.setDateFormat(new SimpleDateFormat(TimeUtil.YYYY_MM_DD_HH_MM_SS));
        return objectMapper.convertValue(entity, Map.class);
    }

    /** 十六进制小写字符查找表，用于快速将字节转为十六进制字符串。 */
    private static final char[] toDigits =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * 将单个十六进制字符转换为对应数值，非法字符抛出异常。
     *
     * @param ch    十六进制字符
     * @param index 字符在字符串中的位置（用于异常提示）
     * @return 对应的数值（0-15）
     */
    private static int toDigit(final char ch, final int index) {
        final int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new RuntimeException("Illegal hexadecimal character " + ch + " at index " + index);
        }
        return digit;
    }

    /**
     * 字节数组转为十六进制字符串
     * @param bytes
     * @return
     */
    public static String byte2hex(byte[] bytes) {
        final int l = bytes.length;
        final char[] out = new char[l << 1];
        for (int i = 0, j = 0; i < l; i++) {
            // 先取高 4 位，再取低 4 位，每个字节对应两个十六进制字符
            out[j++] = toDigits[(0xF0 & bytes[i]) >>> 4];
            out[j++] = toDigits[0x0F & bytes[i]];
        }
        return new String(out);

        /*StringBuilder sb = new StringBuilder();
        String hex;
        for (byte b : bytes) {
            hex = Integer.toHexString(b & 0xFF); // & 0xff 无符号byte转无符号int
            if (hex.length() < 2) {
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();*/
    }

    /**
     * 十六进制字符串转为字节数组
     * @param data
     * @return
     * @throws RuntimeException
     */
    public static byte[] hex2byte(String data) {
        char[] data1 = data.toCharArray();
        final int len = data1.length;
        if ((len & 0x01) != 0) {
            throw new RuntimeException("Odd number of characters.");
        }
        final byte[] out = new byte[len >> 1];
        for (int i = 0, j = 0; j < len; i++) {
            // 每两个十六进制字符合并为一个字节：高 4 位左移后与低 4 位相或
            int f = toDigit(data1[j], j) << 4;
            j++;
            f = f | toDigit(data1[j], j);
            j++;
            out[i] = (byte) (f & 0xFF);
        }
        return out;
    }

    /* ***************************************************** 工具 *****************************************************/

    /**
     * 生成UID
     * @return
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
