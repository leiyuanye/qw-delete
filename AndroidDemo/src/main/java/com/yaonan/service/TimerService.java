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
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.tencent.mmkv.MMKV;
import com.yaonan.App;
import com.yaonan.R;
import com.yaonan.util.WindowHelper;
import com.yaonan.util.codec.Codec;
import com.yaonan.util.exception.ExceptionUtil;
import com.yaonan.util.jna.UI;
import com.yaonan.util.json.Array;
import com.yaonan.util.lang.StringUtil;
import com.yaonan.util.lang.ThreadUtil;
import com.yaonan.util.lang.TimeUtil;

/**
 * 应用保活：
 *
 * 任务中锁定、开启自启动、关闭电池优化
 * 使用前台服务（费电） <----- 本服务使用
 * 多进程守护（同时开启两个或更多进程，如同时开启无障碍服务和前台服务） <----- 本服务使用
 * 使用JobScheduler（WorkManager）
 * 使用前台服务+MediaPlayer，播放无声音乐（费电）
 */
public class TimerService extends Service {

    private static final String CHANNEL_ID = "CHANNEL_ID_TIMER";

    private static final String CHANNEL_NAME = "定时任务";

    private static final int NOTIFICATION_ID = 2;

    private static boolean isStart = false;

    private static Thread thread = null;

    private static PowerManager.WakeLock wakeLock = null;

    private static final NotificationManager NOTIFICATION_MANAGER =
            (NotificationManager) App.getApp().getSystemService(Context.NOTIFICATION_SERVICE);

    private static final Intent SERVICE_INTENT = new Intent(App.getApp(), TimerService.class);

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

    public static void stop() {
        if (!isStart) {
            return;
        }
        isStart = false;

        App.getApp().stopService(SERVICE_INTENT);
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public void onCreate() { // onCreate只会执行一次，onStartCommand可反复执行
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
            String startTime = TimeUtil.nowTime();
            while (!ThreadUtil.isInterrupted()) {
                try {
                    Thread.sleep(20_000);

                    // 判断当前时间是否执行
                    // 同时到时间只执行一个，确保时间间隔
                    int type = -1;
                    boolean isBegin = false;
                    MMKV kv = UI.getMMKV();
                    String tasksJsonStr = kv.getString("tasks", "[]");
                    Array typeAndTimes = new Array(tasksJsonStr);
                    Log.e(TAG, "=== 定时任务 ===");
                    for (int i = 0; i < typeAndTimes.size(); i++) {
                        Array typeAndTime = typeAndTimes.getArray(i);
                        String time = typeAndTime.getString(0);
                        String now = TimeUtil.now("HH:mm");
                        if (now.equals(time)) {
                            Log.e(TAG, "当前时间/任务时间：" + now + "==" + time);
                            type = typeAndTime.getInteger(1);
                            isBegin = true;
                            break;
                        } else {
                            Log.e(TAG, "当前时间/任务时间：" + now + "<>" + time);
                        }
                    }
                    if (isBegin) {

                        // 解锁屏幕
                        Log.e(TAG, "类型：" + type + " 已开始 定时任务");
                        UI.wakeup();
                        Thread.sleep(5_000);
                        cmd("#@#上划解锁#");
                        Thread.sleep(10_000); // 等待屏幕上方悬浮通知的遮挡问题，约5秒后自动消失

                        // 执行：悬浮窗口、无障碍服务、本服务，要一直开启（同时开启无障碍服务和本服务，清理任务再锁屏，本服务也不会停止）
                        String res;
                        int speed = 5000; // 速度

                        if (type == 2) { // 批量单删
                            // 清除任务
                            qingChuRenWu(speed);

                            // 全部清除后，重新打开本app，否则执行launchApp时qwdelete进程会被kill
                            // （不开启无障碍服务，清除任务qwdelete进程也会被kill，开启则不会）
                            UI.launchApp("com.yaonan.qwdelete");
                            Thread.sleep(speed);

                            for (int i = 0; i < 6; i++) {
                                int appIndex = i;

                                // 拉起企业微信多开选择弹窗
                                UI.launchApp("com.tencent.wework");
                                Thread.sleep(speed);

                                cmd("#@#打开企业微信" + appIndex);
                                Thread.sleep(speed * 3);

                                res = cmdWait("#@#通讯录#");
                                Thread.sleep(speed);
                                if ("error".equals(res)) {
                                    continue; // 未登录，跳下一个企微
                                }

                                cmd("#@#我的客户#");
                                Thread.sleep(speed/* * 2*/);

                                res = cmdWait("#@#全部微信客户#");
                                Thread.sleep(speed);
                                if ("error".equals(res)) {
                                    continue; // 没有客户，跳下一个企微
                                }

                                cmd("#@#单向微信客户#");
                                Thread.sleep(speed);

                                cmd("#@#编辑#");
                                Thread.sleep(speed);

                                // 单删
                                int count = 0;
                                while (!ThreadUtil.isInterrupted()) {
                                    cmd("#@#danxiangkehu#");
                                    Thread.sleep(1500);

                                    // 每10次，查询单删到尾部数字结果
                                    if ((++count) % 10 == 9) {
                                        res = cmdWait("#@#共n个客户#", 20);
                                        Thread.sleep(1000);
                                        if ("break".equals(res)) {
                                            break;
                                        }
                                    }
                                }

                                // 返回桌面
                                cmd("#@#action#home");
                                Thread.sleep(speed);
                            }

                            // 清除任务
                            qingChuRenWu(speed);

                        }

                        // 锁屏
                        cmd("#@#action#lock_screen");
                        Thread.sleep(30_000); // 按分钟执行，执行成功暂停1分钟防止重复
                        Log.e(TAG, "类型：" + type + " 已停止 定时任务");
                    } else {
                        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                            Notification logNotification = notificationBuilder
                                    .setContentText("执行时间：" + TimeUtil.nowTime() + "\n启动时间：" + startTime)
                                    .build();
                            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, logNotification);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // 定时任务，只能进入app点击定时任务的停止按钮，才能停止
                } catch (Exception e) {
                    Log.e(TAG, CHANNEL_NAME + "异常");
                    ExceptionUtil.getStackTrace(e);
                }
            }
            Log.e(TAG, CHANNEL_NAME + "停止");
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

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

    /* ****************************************** 命令 *********************************************/

    /**
     * 清除任务
     * 要兼容摩托罗拉手机：前台服务解锁屏幕后
     * @param speed
     * @throws InterruptedException
     */
    public void qingChuRenWu(long speed) throws InterruptedException {
        // 不执行home，后面获取不到“全部清除”节点
        cmd("#@#action#home");
        Thread.sleep(speed);

        // 返回桌面，前台服务解锁屏幕后，第一次执行会失败，没有反应
        cmd("#@#action#home");
        Thread.sleep(speed);

        // 先home再recents，才能获取到“全部清除”节点，否则是当前app节点
        cmd("#@#action#recents");
        Thread.sleep(speed);

        cmd("#@#全部清除#");
        Thread.sleep(speed);
    }

    /**
     * 点击、滑动等操作
     * 异步执行
     * @param str 如"#@#tap#320,2185"
     */
    public void cmd(String str) {
        WindowHelper.SCREENSHOT_VIEW.announceForAccessibility(str);
    }

    /**
     * 点击、滑动等操作
     * 同步执行，执行后不用再Thread.sleep()等待
     * @param str 如"#@#tap#320,2185"
     * @param maxTimes 等待多少次后继续执行，防止缓存更新失败等导致的死循环
     * @return
     * @throws InterruptedException
     */
    public String cmdWait(String str, int maxTimes) throws InterruptedException {
        String msgid = Codec.uuid();

        {
            MMKV kv = UI.getMMKV();
            kv.putString(msgid, "", 3600);
            Log.d(TAG, "cmdWait " + msgid + "<-");

            WindowHelper.SCREENSHOT_VIEW.announceForAccessibility(msgid + "{msgid}" + str);
        }

        for (int i = 0; i < maxTimes; i++) {
            Thread.sleep(1000);

            MMKV kv = UI.getMMKV();
            String res = kv.getString(msgid, "");
            Log.d(TAG, "cmdWait " + msgid + "<-" + res);
            if (StringUtil.isNotEmpty(res)) {
                kv.remove(msgid);
                return res;
            }
        }

        Log.d(TAG, "cmdWait " + msgid + "<-timeout");
        return "timeout";
    }

    /**
     * 点击、滑动等操作
     * 同步执行，执行后不用再Thread.sleep()等待
     * @param str 如"#@#tap#320,2185"
     * @return
     * @throws InterruptedException
     */
    public String cmdWait(String str) throws InterruptedException {
        String msgid = Codec.uuid();

        {
            MMKV kv = UI.getMMKV();
            kv.putString(msgid, "", 3600);
            Log.d(TAG, "cmdWait " + msgid + "<-");

            WindowHelper.SCREENSHOT_VIEW.announceForAccessibility(msgid + "{msgid}" + str);
        }

        while (true) {
            Thread.sleep(1000);

            MMKV kv = UI.getMMKV();
            String res = kv.getString(msgid, "");
            Log.d(TAG, "cmdWait " + msgid + "<-" + res);
            if (StringUtil.isNotEmpty(res)) {
                kv.remove(msgid);
                return res;
            }
        }
    }
}
