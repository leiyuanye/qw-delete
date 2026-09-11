package com.yaonan;

import static com.yaonan.util.global.Global.TAG;

import android.app.Application;
import android.util.Log;

import com.tencent.mmkv.MMKV;
import com.yaonan.util.jna.UI;

import java.util.HashMap;
import java.util.Map;

/**
 * 应用程序入口类。
 *
 * <p>负责维护全局唯一的 Application 单例、初始化全局依赖组件（如 MMKV 键值存储），
 * 并提供进程内共享数据的存取能力，供各模块通过 {@link #getApp()} 获取应用上下文。</p>
 */
public class App extends Application {

    /** 全局唯一的 Application 实例，供静态方法获取应用上下文使用。 */
    private static App app;

    /**
     * 应用创建回调：保存全局实例并执行初始化。
     */
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        init();
    }

    /**
     * 获取全局唯一的 Application 实例。
     *
     * @return 应用实例
     */
    public static App getApp() {
        return app;
    }

    /**
     * Application共享数据
     */
    private Map<String, Object> _data = new HashMap<>();

    /**
     * 根据键获取共享数据（带泛型类型转换）。
     *
     * @param key 数据键
     * @return 对应键的值
     */
    @SuppressWarnings("unchecked")
    public synchronized <T> T getData(String key) {
        return (T) _data.get(key);
    }

    /**
     * 根据键写入共享数据。
     *
     * @param key   数据键
     * @param value 数据值
     */
    public synchronized void setData(String key, Object value) {
        _data.put(key, value);
    }

    /**
     * 初始化全局依赖组件（当前为 MMKV 键值存储）。
     */
    private void init() {
        MMKV.initialize(this);
    }
}
