package com.yaonan.service;

import static com.yaonan.util.global.Global.TAG;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.yaonan.App;
import com.yaonan.R;
import com.yaonan.util.codec.Codec;
import com.yaonan.util.exception.ExceptionUtil;
import com.yaonan.util.http.API;
import com.yaonan.util.http.Response;
import com.yaonan.util.lang.ThreadUtil;
import com.yaonan.util.lang.TimeUtil;

import java.util.Objects;

/**
 * 短信监控前台服务。
 *
 * <p>职责：常驻后台轮询系统收件箱，检测到新短信后将其号码、时间、内容上报至服务器。</p>
 */
/**
 * 短信监控
 */
public class SmsService extends Service {

    /** 通知渠道 ID（Android 8.0+ 通知分类使用） */
    private static final String CHANNEL_ID = "CHANNEL_ID_SMS";

    /** 通知渠道名称 */
    private static final String CHANNEL_NAME = "短信监控";

    /** 前台服务通知的固定 ID */
    private static final int NOTIFICATION_ID = 3;

    /** 服务是否已启动的标记（静态，便于外部通过 start/stop 控制） */
    private static boolean isStart = false;

    /** 执行短信轮询的后台线程 */
    private static Thread thread = null;

    /** 唤醒锁，用于锁屏后保持 CPU 运行 */
    private static PowerManager.WakeLock wakeLock = null;

    /** 系统通知管理器，用于创建通知渠道和发送通知 */
    private static final NotificationManager NOTIFICATION_MANAGER =
            (NotificationManager) App.getApp().getSystemService(Context.NOTIFICATION_SERVICE);

    /** 用于启动/停止本服务的 Intent 常量 */
    private static final Intent SERVICE_INTENT = new Intent(App.getApp(), SmsService.class);

    /**
     * 启动短信监控前台服务（幂等：已启动则直接返回）。
     */
    public static void start() {
        if (isStart) {
            return;
        }
        isStart = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            App.getApp().startForegroundService(SERVICE_INTENT);
        } else {
            App.getApp().startService(SERVICE_INTENT);
        }
    }

    /**
     * 停止短信监控前台服务（幂等：未启动则直接返回）。
     */
    public static void stop() {
        if (!isStart) {
            return;
        }
        isStart = false;

        App.getApp().stopService(SERVICE_INTENT);
    }

    /**
     * 服务创建时初始化：获取唤醒锁、构建前台通知，并启动后台轮询短信的线程。
     */
    @SuppressLint({"Range", "MissingPermission", "HardwareIds", "WakelockTimeout"})
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, CHANNEL_NAME + "开始");

        // 锁屏后继续保持运行
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                ":" + getClass().getName());
        wakeLock.acquire();

        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(CHANNEL_NAME + "已启动");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            NOTIFICATION_MANAGER.createNotificationChannel(channel);

            notificationBuilder.setChannelId(CHANNEL_ID);
        }
        Notification notification = notificationBuilder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK); // 前台服务类型：媒体
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        thread = ThreadUtil.async(() -> {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            String number = Objects.requireNonNullElse(telephonyManager.getLine1Number(), ""); // 本机号码

            String startTime = TimeUtil.nowTime();
            // oldId：上一次轮询到的最新短信 ID，-1 表示首次轮询（首轮仅记录不触发上报）
            int oldId = -1;
            while (!ThreadUtil.isInterrupted()) {
                Cursor cursor = null;
                try {
                    Thread.sleep(10_000);
                    Log.d(TAG, "=== 最新短信 ===");

                    cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), // 收件箱，content://sms所有短信
                            new String[] { "_id", "address", "body", "date" },
                            null, null, "date desc");
                    if (cursor != null && cursor.moveToFirst()) {
                        int lastId = cursor.getInt(cursor.getColumnIndex("_id"));
                        if (oldId != -1 && lastId != oldId) { // 新短信

                            long date = cursor.getLong(cursor.getColumnIndex("date"));
                            String address = cursor.getString(cursor.getColumnIndex("address")); // 发送号码
                            String body = cursor.getString(cursor.getColumnIndex("body")); // 短信内容

                            Log.e(TAG, "短信ID：" + lastId);
                            Log.e(TAG, "本机号码：" + number);
                            Log.e(TAG, "发送时间：" + TimeUtil.formatTime(date));
                            Log.e(TAG, "发送号码：" + address);
                            Log.e(TAG, body
                                    .replace("\r", "")
                                    .replace("\n", "")
                                    .replace(" ", ""));

                            // http通知服务器
                            Response resp = API.sendOriginal(true,
                                    "https://t2.tuielf.com/api/sms/log" +
                                            "?number=" + Codec.url_encode(number) +
                                            "&date=" + Codec.url_encode("" + date) +
                                            "&address=" + Codec.url_encode(address) +
                                            "&body=" + Codec.url_encode(body),
                                    null, null);
                            if (resp.isError()) {
                                Log.e(TAG, Codec.json_encode(resp));
                            } else {
                                Log.d(TAG, number + ":" + resp.getBody());
                            }

                        } else {
                            Log.e(TAG, "没有新短信, oldId: " + oldId + ", lastId: " + lastId);
                        }
                        oldId = lastId;
                    } else {
                        Log.e(TAG, "没有短信");
                    }

                    if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                        Notification logNotification = notificationBuilder
                                .setContentText("执行时间：" + TimeUtil.nowTime() + "\n启动时间：" + startTime)
                                .build();
                        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, logNotification);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // 短信监控，只能进入app点击定时任务的停止按钮，才能停止
                } catch (Exception e) {
                    Log.e(TAG, CHANNEL_NAME + "异常");
                    ExceptionUtil.getStackTrace(e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            Log.e(TAG, CHANNEL_NAME + "停止");
        });
    }

    /**
     * 本地服务无需绑定，返回 null。
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 服务销毁时释放唤醒锁并中断后台线程，避免资源泄漏。
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }
}
