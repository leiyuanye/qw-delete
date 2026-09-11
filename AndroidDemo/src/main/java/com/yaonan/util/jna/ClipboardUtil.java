package com.yaonan.util.jna;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import com.yaonan.App;
import com.yaonan.util.codec.Codec;
import com.yaonan.util.lang.StringUtil;

/**
 * 剪贴板工具类：提供系统剪贴板的读取、写入与可用性检测。
 */
public class ClipboardUtil {
    /**
     * 检测剪贴板是否可读写：写入随机 UUID 后读回比对是否一致。
     *
     * @return 剪贴板是否可用
     */
    public static boolean check() {
        String uuid = Codec.uuid();
        setString(uuid);
        return uuid.equals(getString());
    }

    /**
     * 向剪贴板写入纯文本（空文本忽略）。
     *
     * @param text 待写入文本
     */
    public static void setString(String text) {
        if (StringUtil.isEmpty(text)) return;
        ClipboardManager clipboard =
                (ClipboardManager) App.getApp().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData data = ClipData.newPlainText("text", text);
        clipboard.setPrimaryClip(data);
    }

    /**
     * 读取剪贴板中的纯文本。
     *
     * @return 剪贴板文本（无内容时返回 null）
     */
    public static String getString() {
        ClipboardManager clipboard =
                (ClipboardManager) App.getApp().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData data = clipboard.getPrimaryClip();
        // 仅当剪贴板非空且包含条目时读取第一项文本
        if (data != null && data.getItemCount() > 0) {
            ClipData.Item item = data.getItemAt(0);
            return item.getText() == null ? null : item.getText().toString();
        }
        return null;
    }
}
