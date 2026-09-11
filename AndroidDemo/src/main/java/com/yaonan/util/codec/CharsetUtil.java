package com.yaonan.util.codec;

import java.nio.charset.Charset;

/**
 * 字符集工具类
 * java内部为UTF-16BE编码，仅能支持65535个字符，定长编码，即无论中英文，1个[字符-char]都是2个[字节-byte]，str.length()为几个字符
 * java外部要用UTF-8编码，支持全部Unicode字符，变长编码，1～4字节，国际化最优方案，str.getBytes()等默认编码受-Dfile.encoding控制
 *
 * 当前.java文件对应编码的字符串字面量 ==> 转为UTF-16BE编码的字符串变量 <==设置charset互相转换==> 某种编码的字节数组 <==设置charset互相转换==> 文件、浏览器等根据编码输出显示
 * 文件流本质是字节数组；字符串本质是某种编码的字节数组，可根据对应编码将字节数组解析为相应字符并显示出来
 *
 * Created by yaonan on 2022/1/20.
 */
public class CharsetUtil {

    public static final String US_ASCII = "US-ASCII"; // 英语
    public static final String ISO_8859_1 = "ISO-8859-1"; // 西欧
    public static final String UTF_8 = "UTF-8"; // Unicode
    public static final String GB2312 = "GB2312"; // 简体中文
    public static final String GBK = "GBK"; // 简体中文
    public static final String GB18030 = "GB18030"; // 简体中文
    public static final String BIG5 = "Big5"; // 台湾

    // 与上述名称常量对应的 Charset 对象，避免各处重复调用 Charset.forName
    public static final Charset US_ASCII_CHARSET = Charset.forName(US_ASCII);
    public static final Charset ISO_8859_1_CHARSET = Charset.forName(ISO_8859_1);
    public static final Charset UTF_8_CHARSET = Charset.forName(UTF_8);
    public static final Charset GB2312_CHARSET = Charset.forName(GB2312);
    public static final Charset GBK_CHARSET = Charset.forName(GBK);
    public static final Charset GB18030_CHARSET = Charset.forName(GB18030);
    public static final Charset BIG5_CHARSET = Charset.forName(BIG5);

    /** 所有支持的字符集名称集合。 */
    public static final String[] CHARSETS = { US_ASCII, ISO_8859_1, UTF_8, GB2312, GBK, GB18030, BIG5 };

    /**
     * ASCII控制字符
     */
    public class Ascii {
        public static final String NUL = "" + (char) 0; // \0
        public static final String SOH = "" + (char) 1;
        public static final String STX = "" + (char) 2;
        public static final String ETX = "" + (char) 3; // ^C ctrl+c
        public static final String EOT = "" + (char) 4;
        public static final String ENQ = "" + (char) 5;
        public static final String ACK = "" + (char) 6;
        public static final String BEL = "" + (char) 7;
        public static final String BS = "" + (char) 8; // \b Backspace
        public static final String HT = "" + (char) 9; // \t Tab
        public static final String LF = "" + (char) 10; // \n 换行键
        public static final String VT = "" + (char) 11;
        public static final String FF = "" + (char) 12; // \f
        public static final String CR = "" + (char) 13; // \r 回车键
        public static final String SO = "" + (char) 14;
        public static final String SI = "" + (char) 15;
        public static final String DLE = "" + (char) 16;
        public static final String DC1 = "" + (char) 17;
        public static final String DC2 = "" + (char) 18;
        public static final String DC3 = "" + (char) 19;
        public static final String DC4 = "" + (char) 20;
        public static final String NAK = "" + (char) 21;
        public static final String SYN = "" + (char) 22;
        public static final String ETB = "" + (char) 23;
        public static final String CAN = "" + (char) 24;
        public static final String EM = "" + (char) 25;
        public static final String SUB = "" + (char) 26;
        public static final String ESC = "" + (char) 27; // \033
        public static final String FS = "" + (char) 28;
        public static final String GS = "" + (char) 29;
        public static final String RS = "" + (char) 30;
        public static final String US = "" + (char) 31;
        public static final String DEL = "" + (char) 127;

        public static final String DOUBLE_QUOTES = "" + (char) 34; // \" 双引号
    }
}
