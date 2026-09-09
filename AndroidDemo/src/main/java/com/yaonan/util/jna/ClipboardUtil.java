package com.yaonan.util.jna;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import com.yaonan.App;
import com.yaonan.util.codec.Codec;
import com.yaonan.util.lang.StringUtil;

public class ClipboardUtil {
    public static boolean check() {
        String uuid = Codec.uuid();
        setString(uuid);
        return uuid.equals(getString());
    }

    public static void setString(String text) {
        if (StringUtil.isEmpty(text)) return;
        ClipboardManager clipboard =
                (ClipboardManager) App.getApp().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData data = ClipData.newPlainText("text", text);
        clipboard.setPrimaryClip(data);
    }

    public static String getString() {
        ClipboardManager clipboard =
                (ClipboardManager) App.getApp().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData data = clipboard.getPrimaryClip();
        if (data != null && data.getItemCount() > 0) {
            ClipData.Item item = data.getItemAt(0);
            return item.getText() == null ? null : item.getText().toString();
        }
        return null;
    }
}
