package com.yaonan;

import static com.yaonan.util.global.Global.TAG;

import android.app.Application;
import android.util.Log;

import com.tencent.mmkv.MMKV;
import com.yaonan.util.jna.UI;

import java.util.HashMap;
import java.util.Map;

public class App extends Application {

    private static App app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        init();
    }

    public static App getApp() {
        return app;
    }

    /**
     * Application共享数据
     */
    private Map<String, Object> _data = new HashMap<>();

    @SuppressWarnings("unchecked")
    public synchronized <T> T getData(String key) {
        return (T) _data.get(key);
    }

    public synchronized void setData(String key, Object value) {
        _data.put(key, value);
    }

    private void init() {
        MMKV.initialize(this);
    }
}
