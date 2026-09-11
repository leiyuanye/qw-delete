package com.yaonan.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;

import com.yaonan.App;
import com.yaonan.util.jna.UI;
import com.yaonan.view.ScreenshotView;

/**
 * 悬浮窗工具类。
 *
 * <p>负责悬浮窗（截屏视图 ScreenshotView）的显示/隐藏、悬浮窗权限检测，
 * 以及获取屏幕真实尺寸等窗口相关操作。</p>
 */
public class WindowHelper {
    /** 系统窗口管理器，用于添加、更新和移除悬浮窗。 */
    private static final WindowManager WINDOW_MANAGER = (WindowManager) App.getApp().getSystemService(Context.WINDOW_SERVICE);
    /** 全局共享的截屏视图实例。 */
    public static final ScreenshotView SCREENSHOT_VIEW = new ScreenshotView(App.getApp());
    /** 截屏视图对应的窗口布局参数。 */
    private static final WindowManager.LayoutParams SCREENSHOT_VIEW_PARAMS = newLayoutParams();
    /** 截屏视图是否正在显示的标记。 */
    private static boolean mScreenshotViewShowing = false;

    static {
        // 注册布局监听：当视图位置变化时，同步更新窗口布局参数
        SCREENSHOT_VIEW.setLayoutListener((x, y) -> {
            SCREENSHOT_VIEW_PARAMS.x = x;
            SCREENSHOT_VIEW_PARAMS.y = y;
            WINDOW_MANAGER.updateViewLayout(SCREENSHOT_VIEW, SCREENSHOT_VIEW_PARAMS);
        });
    }

    /**
     * 显示截屏悬浮窗（若已显示则忽略）。
     */
    public static void showScreenshotView() {
        if (mScreenshotViewShowing) return;
        WINDOW_MANAGER.addView(SCREENSHOT_VIEW, SCREENSHOT_VIEW_PARAMS);
        mScreenshotViewShowing = true;
    }

    /**
     * 隐藏截屏悬浮窗（若未显示则忽略）。
     */
    public static void hideScreenshotView() {
        if (!mScreenshotViewShowing) return;
        WINDOW_MANAGER.removeView(SCREENSHOT_VIEW);
        mScreenshotViewShowing = false;
    }

    /**
     * 检测悬浮窗权限：已授权返回 true，否则弹窗提示并跳转到授权设置页。
     *
     * @param activity 当前界面
     * @return 是否已获得悬浮窗权限
     */
    public static boolean checkOverlay(Activity activity) {
        if (Settings.canDrawOverlays(activity)) {
            return true;
        } else {
            UI.alert("请开启悬浮窗权限！");
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    .setData(Uri.parse("package:"+activity.getPackageName()));
            activity.startActivity(intent);
            return false;
        }
    }

    /**
     * 获取屏幕真实显示尺寸（含状态栏、导航栏区域）。
     *
     * @return 屏幕真实尺寸信息
     */
    public static DisplayMetrics getRealMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WINDOW_MANAGER.getDefaultDisplay().getRealMetrics(displayMetrics);
        return displayMetrics;
    }

    /**
     * 构造悬浮窗布局参数。
     *
     * @return 悬浮窗布局参数
     */
    private static WindowManager.LayoutParams newLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        params.gravity = Gravity.START | Gravity.TOP;
        params.format = PixelFormat.TRANSLUCENT;
        return params;
    }
}
