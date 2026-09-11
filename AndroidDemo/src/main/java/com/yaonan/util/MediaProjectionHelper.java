package com.yaonan.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;

import com.yaonan.App;
import com.yaonan.service.MediaProjectionService;
import com.yaonan.util.jna.UI;

/**
 * 屏幕录制（MediaProjection）辅助类。
 *
 * <p>负责发起屏幕捕获授权、处理授权结果，并启动/停止屏幕录制前台服务。</p>
 */
public class MediaProjectionHelper {

    /** 系统屏幕采集管理器，用于创建屏幕捕获授权 Intent。 */
    private static final MediaProjectionManager MEDIA_PROJECTION_MANAGER = (MediaProjectionManager) App.getApp().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    /** 屏幕录制服务的启动 Intent。 */
    private static final Intent SERVICE_INTENT = new Intent(App.getApp(), MediaProjectionService.class);
    /** 录制服务是否已启动的标记。 */
    private static boolean mStarted = false;

    /**
     * 获取系统屏幕采集管理器。
     *
     * @return 屏幕采集管理器
     */
    public static MediaProjectionManager getManager() {
        return MEDIA_PROJECTION_MANAGER;
    }

    /**
     * 发起屏幕捕获授权请求（弹出系统授权框）。
     *
     * @param activity 当前界面
     */
    public static void start(Activity activity) {
        if (mStarted) return;
        activity.startActivityForResult(MEDIA_PROJECTION_MANAGER.createScreenCaptureIntent(), 0);
    }

    /**
     * 处理屏幕捕获授权结果：授权成功后保存结果数据并启动录制服务。
     *
     * @param requestCode 请求码
     * @param resultCode  结果码
     * @param resultData  授权返回数据
     */
    public static void onStartResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode != 0) return;
        if (resultCode == Activity.RESULT_OK) {
            // 将授权结果保存到服务中，供其初始化 MediaProjection 使用
            MediaProjectionService.resultCode = resultCode;
            MediaProjectionService.resultData = resultData;
            // Android 8.0 及以上使用 startForegroundService 启动前台服务
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                App.getApp().startForegroundService(SERVICE_INTENT);
            } else {
                App.getApp().startService(SERVICE_INTENT);
            }
            mStarted = true;
        } else {
            UI.alert("录制服务启动失败");
        }
    }

    /**
     * 停止屏幕录制服务。
     */
    public static void stop() {
        if (!mStarted) return;
        App.getApp().stopService(SERVICE_INTENT);
        mStarted = false;
    }
}
