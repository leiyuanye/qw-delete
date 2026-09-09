package com.yaonan.util.jna;

import static android.content.Context.KEYGUARD_SERVICE;
import static com.yaonan.util.global.Global.TAG;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.tencent.mmkv.MMKV;
import com.yaonan.App;
import com.yaonan.R;
import com.yaonan.util.exception.ExceptionUtil;
import com.yaonan.util.lang.ThreadUtil;

public class UI {

    public static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Toast toast;

    public static void invokeLater(Runnable doRun) {
        MAIN.post(() -> {
            try {
                doRun.run();
            } catch (Exception e) {
                alert(e);
            }
        });
    }

    public static void alert(String message) {
        _toast(message, false);
    }

    public static void alert(String message, boolean isLong) {
        _toast(message, isLong);
    }

    public static void alert(Exception e) {
        String message = ExceptionUtil.getStackTrace(e);
        _toast(message, true);
    }

    public static void alert(String message, Context context) {
        _toast(message, false, context);
    }

    private static void _toast(String message, boolean isLong) {
        _toast(message, isLong, App.getApp());
    }

    private static void _toast(String message, boolean isLong, Context context) {
        MAIN.post(() -> {
            try {
                if (toast != null) toast.cancel();
                toast = Toast.makeText(context, message, isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
                toast.show();
            } catch (Exception e) {
                Log.e(TAG, "toast error", e);
            }
        });
    }

    public static MMKV getMMKV() {
        return MMKV.defaultMMKV();
    }

    public static void launchApp(String packageName) {
        Intent intent = App.getApp().getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            App.getApp().startActivity(intent);
        }
    }

    public static void wakeup() {
        PowerManager powerManager = (PowerManager) App.getApp().getSystemService(Context.POWER_SERVICE);
        if (!powerManager.isScreenOn()) {
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                    App.getApp().getPackageName() + ":wake");
            wakeLock.acquire(3000);
        }
        KeyguardManager keyguardManager = (KeyguardManager) App.getApp().getSystemService(KEYGUARD_SERVICE);
        if (keyguardManager.isKeyguardLocked()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                keyguardManager.requestDismissKeyguard((Activity) null, null);
            }
        }
    }
}
