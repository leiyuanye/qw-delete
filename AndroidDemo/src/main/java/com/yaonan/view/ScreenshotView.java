package com.yaonan.view;
import static com.yaonan.util.global.Global.TAG;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tencent.mmkv.MMKV;
import com.yaonan.App;
import com.yaonan.R;
import com.google.android.accessibility.selecttospeak.SelectToSpeakService;
import com.yaonan.util.codec.Codec;
import com.yaonan.util.jna.UI;
import com.yaonan.util.lang.StringUtil;
import com.yaonan.util.lang.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 悬浮截图视图（悬浮窗内容）。
 *
 * <p>职责：作为无障碍服务承载的悬浮 UI，提供"开始/停止"脚本开关与脚本类型选择，
 * 支持拖动，并负责驱动企业微信执行单删/批量单删自动化脚本。</p>
 */
public class ScreenshotView extends FrameLayout {

    /** 可选的脚本类型列表（"单删"对应类型 1，"批量单删"对应类型 2） */
    public static final List<String> types = List.of("单删", "批量单删");

    /** 执行脚本的后台循环线程（null 表示当前未在运行） */
    public static Thread loopThread = null;

    /** 布局（拖动位置）变化监听器，用于将拖动结果回传给宿主 */
    @Nullable
    private ILayoutListener mListener;

    /** 代码中直接创建视图时使用 */
    public ScreenshotView(@NonNull Context context) {
        super(context);
        init();
    }

    /** 从 XML 布局解析（无样式属性）时使用 */
    public ScreenshotView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /** 从 XML 布局解析（带样式属性）时使用 */
    public ScreenshotView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化视图：加载布局、绑定悬浮球的拖动与点击事件，并初始化脚本类型下拉框。
     */
    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.layout_screenshot_view, this);
        findViewById(R.id.tv_screenshot).setOnTouchListener(new OnTouchListener() {
            /** 手指按下时的 X 坐标，用于计算拖动距离 */
            private float mDownX = 0F;
            /** 手指按下时的 Y 坐标，用于计算拖动距离 */
            private float mDownY = 0F;
            /** 是否已判定为拖动（而非点击） */
            private boolean mIsMoving = false;
            /** 判定为拖动所需的最小移动像素阈值 */
            private final int MIN_MOVING_PIXELS = getResources().getDimensionPixelSize(R.dimen.min_moving_pixels);
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mDownX = event.getX();
                        mDownY = event.getY();
                        mIsMoving = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mIsMoving = mIsMoving || isMoving(event);
                        if (mIsMoving) {
                            v.setPressed(false);
                            if (mListener != null) {
                                int x = (int) (event.getRawX() - mDownX);
                                int y = (int) (event.getRawY() - mDownY);
                                mListener.onLayout(x, y);
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        // 未发生拖动时视为点击：未运行则启动脚本，运行中则停止脚本
                        if (!mIsMoving) {
                            if (loopThread == null) {
                                TextView textView = (TextView) v;
                                loopThread = ThreadUtil.async(() -> {
                                    try {
                                        SelectToSpeakService.isRunning = true;
                                        UI.invokeLater(() -> {
                                            UI.alert("已开始");
                                        });
                                        UI.invokeLater(() -> {
                                            textView.setText("停止");
                                        });
                                        int type = App.getApp().getData("type");
                                        Log.e(TAG, "类型：" + type);

                                        String res = "";
                                        int speed = 5000; // 速度
                                        // 摩托罗拉，分辨率1080 * 2400/2520
                                        // * 不锁定任务，不开启无障碍服务，清除任务后，app进程会被kill
                                        // * 不锁定任务，开启无障碍服务，清除任务后，app进程不会立刻被kill，但是锁屏、launchApp时会被kill，但是无障碍服务还是开启
                                        // * 不锁定任务，开启无障碍服务，清除任务后，重新打开app，再锁屏等就不会被kill
                                        // * 结论：开启锁定任务（不要手动删除锁定的任务）

                                        if (type == 1) { // 单删
                                            while (!ThreadUtil.isInterrupted()) {
                                                cmd("#@#danxiangkehu#");
                                                Thread.sleep(1500);
                                            }

                                        } else if (type == 2) { // 批量单删
                                            // 清除任务
                                            qingChuRenWu(speed);

                                            // 全部清除后，重新打开本app，否则执行launchApp时qwdelete进程会被kill
                                            // （不开启无障碍服务，清除任务qwdelete进程也会被kill，开启则不会）
                                            UI.launchApp("com.yaonan.qwdelete");
                                            Thread.sleep(speed);

                                            for (int i = 0; i < 6; i++) {
                                                int appIndex = i;
                                                UI.invokeLater(() -> {
                                                    textView.setText("停止" + appIndex);
                                                });

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
                                        UI.invokeLater(() -> {
                                            UI.alert("已停止");
                                        });
                                    } catch (InterruptedException e) {
                                        // 手动停止
                                        SelectToSpeakService.isRunning = false;
                                        UI.invokeLater(() -> {
                                            UI.alert("已停止");
                                        });
                                    } catch (Exception e) {
                                        SelectToSpeakService.isRunning = false;
                                        UI.invokeLater(() -> {
                                            UI.alert(e);
                                        });
                                    }
                                    SelectToSpeakService.isRunning = false;
                                    loopThread = null;
                                    UI.invokeLater(() -> {
                                        textView.setText("开始");
                                    });
                                });
                            } else {
                                // 暂停脚本
                                Log.e(TAG, "手动停止");
                                SelectToSpeakService.isRunning = false;
                                loopThread.interrupt();
                            }
                        }
                }
                return true;
            }

            private boolean isMoving(MotionEvent event) {
                return Math.abs(event.getX() - mDownX) > MIN_MOVING_PIXELS || Math.abs(event.getY() - mDownY) > MIN_MOVING_PIXELS;
            }
        });

        // 类型设置
        // 根据各类型是否授权（permission_1/permission_2）过滤可选项
        List<String> typeOptions = new ArrayList<>(types);
        MMKV kv = UI.getMMKV();
        for (int i = 0; i < types.size(); i++) {
            if (!kv.getBoolean("permission_" + (i + 1), true)) {
                typeOptions.remove(types.get(i));
            }
        }
        Spinner spinnerType = findViewById(R.id.spinner_type);
        spinnerType.setAdapter(new ArrayAdapter<String>(
                getContext(), android.R.layout.simple_spinner_dropdown_item, typeOptions) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextSize(10); // 已选项的字体大小
                return view;
            }
        });
        spinnerType.setSelection(typeOptions.indexOf("批量单删")); // 触发一次onItemSelected，-1选中0
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String typeName = (String) spinnerType.getSelectedItem();
                int type = types.indexOf(typeName) + 1;
                App.getApp().setData("type", type);
                Log.d(TAG, "类型" + App.getApp().getData("type") + "：" + typeName);
                UI.alert("类型" + App.getApp().getData("type") + "：" + typeName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    /**
     * 设置布局（拖动）监听器，供宿主注册以接收拖动位移。
     */
    public void setLayoutListener(ILayoutListener listener) {
        mListener = listener;
    }

    /**
     * 布局拖动监听接口。
     */
    public interface ILayoutListener {
        void onLayout(int x, int y);
    }

    /* ****************************************** 命令 *********************************************/

    /**
     * 清除任务
     * 要兼容摩托罗拉手机：手动点击弹窗开始后
     * @param speed
     * @throws InterruptedException
     */
    public void qingChuRenWu(long speed) throws InterruptedException {
        // 不执行home，后面获取不到"全部清除"节点
        cmd("#@#action#home");
        Thread.sleep(speed);

        // 返回桌面，前台服务解锁屏幕后，第一次执行会失败，没有反应
        cmd("#@#action#home");
        Thread.sleep(speed);

        // 先home再recents，才能获取到"全部清除"节点，否则是当前app节点
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
        ScreenshotView.this.announceForAccessibility(str);
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

            ScreenshotView.this.announceForAccessibility(msgid + "{msgid}" + str);
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

            ScreenshotView.this.announceForAccessibility(msgid + "{msgid}" + str);
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