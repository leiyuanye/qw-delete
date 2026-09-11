package com.yaonan.util;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;

import com.yaonan.App;
import com.yaonan.R;

/**
 * 通知工具类。
 *
 * <p>负责通知权限检测与引导授权，以及为前台服务（如屏幕录制服务）创建通知渠道并启动前台通知。</p>
 */
public class NotificationHelper {
    /** 屏幕录制前台服务使用的通知渠道 ID。 */
    private static final String CHANNEL_ID_MEDIA_PROJECTION = "CHANNEL_ID_MEDIA_PROJECTION";
    /** 屏幕录制前台服务使用的通知渠道名称。 */
    private static final String CHANNEL_NAME_MEDIA_PROJECTION = "屏幕录制";
    /** 屏幕录制前台服务通知的固定 ID。 */
    private static final int NOTIFICATION_ID_MEDIA_PROJECTION = 1;
    /** 系统通知管理器，用于创建通知渠道。 */
    private static final NotificationManager NOTIFICATION_MANAGER = (NotificationManager) App.getApp().getSystemService(Context.NOTIFICATION_SERVICE);

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

    /**
     * 以屏幕录制前台服务的方式启动服务，并显示常驻通知。
     *
     * @param service 目标服务
     * @param name    服务名称（用于通知标题）
     */
    public static void startMediaProjectionForeground(Service service, String name) {
        Notification.Builder notificationBuilder = new Notification.Builder(service)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(name+"服务已启动");
        // Android 8.0 及以上需先创建通知渠道并绑定到通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_MEDIA_PROJECTION, CHANNEL_NAME_MEDIA_PROJECTION, NotificationManager.IMPORTANCE_HIGH);
            NOTIFICATION_MANAGER.createNotificationChannel(channel);
            notificationBuilder.setChannelId(CHANNEL_ID_MEDIA_PROJECTION);
        }
        Notification notification = notificationBuilder.build();
        // Android 10 及以上需显式声明前台服务类型为屏幕录制
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            service.startForeground(NOTIFICATION_ID_MEDIA_PROJECTION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            service.startForeground(NOTIFICATION_ID_MEDIA_PROJECTION, notification);
        }
    }
}
