package com.yaonan.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;

import com.yaonan.App;

/**
 * 通知工具类。
 *
 * <p>负责通知权限检测与引导授权。</p>
 */
public class NotificationHelper {
    /** 系统通知管理器，用于检测通知开关状态。 */
    private static final android.app.NotificationManager NOTIFICATION_MANAGER =
            (android.app.NotificationManager) App.getApp().getSystemService(Activity.NOTIFICATION_SERVICE);

    /**
     * 检测通知权限：未开启时弹窗提示并引导用户跳转到通知设置页。
     *
     * @param activity 当前界面
     */
    public static void check(Activity activity) {
        if (!NotificationManagerCompat.from(activity).areNotificationsEnabled()) {
            new AlertDialog.Builder(activity)
                    .setTitle("通知")
                    .setMessage("请授予通知权限。")
                    .setNegativeButton("取消", (dialog, which) -> {
                    })
                    .setPositiveButton("确认", (dialog, which) -> {
                        Intent intent;
                        // Android 8.0 及以上跳转到应用通知设置页，以下跳转到应用详情页
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
                        } else {
                            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.parse("package:" + activity.getPackageName()));
                        }
                        activity.startActivity(intent);
                    }).create().show();
        }
    }
}
