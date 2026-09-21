package com.google.android.accessibility.selecttospeak;

import static com.yaonan.util.global.Global.TAG;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tencent.mmkv.MMKV;
import com.yaonan.util.WindowHelper;
import com.yaonan.util.codec.Codec;
import com.yaonan.util.exception.ExceptionUtil;
import com.yaonan.util.jna.UI;
import com.yaonan.util.lang.StringUtil;
import com.yaonan.util.lang.ThreadUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 无障碍服务实现类（注册为 SelectToSpeak，实际作为自动化脚本执行器使用）。
 * 职责：以后台无障碍服务的形式常驻运行，监听系统无障碍事件（TYPE_ANNOUNCEMENT），
 * 解析控制端注入的 "#@#" 指令，通过对无障碍节点执行查找、点击、长按、滑动、手势等模拟操作，
 * 驱动企业微信自动完成"单向客户单删/批量单删"任务，并将执行结果通过 MMKV 回传给控制端。
 */
public class SelectToSpeakService extends AccessibilityService {

    /** 服务运行状态标志：true 表示当前无障碍服务已连接并处于运行状态，供其他线程判断服务是否可用 */
    public static volatile boolean isRunning = false;
    /** 手势执行中标志：true 表示当前正在执行某个手势，用于在手势进行期间跳过新命令，避免打断正在执行的手势 */
    private static volatile boolean isGesturing = false;

    /**
     * 单删勾选手势节奏参数。
     * 说明：Android 无障碍手势（dispatchGesture）由系统串行执行，同一时刻只允许一个手势在跑，
     * 多线程并发调用 dispatchGesture 时新手势会取消旧手势，因此无法做到真正意义上的"多线程并行点击"。
     * 正确做法是把所有复选框点击打包成「单个手势 + 多条错峰 stroke」一次下发，通过下面两个参数控制点击节奏：
     * - STROKE_DURATION_MS：单个复选框点击（按下→抬起）的持续时间，即每次点击的按压时长；
     * - STROKE_INTERVAL_MS：相邻两个 stroke 的起始时间间隔，直接决定勾选速度（原保守值为 700ms）。
     * 调小 STROKE_INTERVAL_MS 可加快勾选，但过小会导致企业微信来不及响应而漏勾；若出现漏勾请适当调大。
     */
    private static final long STROKE_DURATION_MS = 200L;
    private static final long STROKE_INTERVAL_MS = 300L;

    /**
     * 服务连接成功回调：无障碍服务启动后由系统调用，当前仅调用父类默认实现，预留服务启动后的初始化扩展点。
     */
    @Override
    protected void onServiceConnected() {
        //Log.e(TAG, "无障碍服务启动");
        super.onServiceConnected();
    }

    /**
     * 服务被系统中断回调：当无障碍服务被系统关闭或重启时调用，当前未做任何处理。
     */
    @Override
    public void onInterrupt() {

    }

    /**
     * 如何查找指定view节点：
     *  1直接通过findAccessibilityNodeInfosByText findAccessibilityNodeInfosByViewId获取view节点
     *  2先通过GestureDescription点击、再findFocus获取view节点
     *  3通过Id、Text或findFocus找到临近节点、再通过getParent和getChild(i)并结合各节点特定属性、定位到view节点
     *  4直接通过getRootInActiveWindow得到根节点、再getChild(i)判断每级节点特定属性、逐级判断定位到view节点
     *  找到view节点后，如performAction无法操作，可结合节点Rect、手势GestureDescription继续操作
     *
     * 已使用的全局动作（api16以上可用）：
     * performGlobalAction(GLOBAL_ACTION_HOME); // 按home键
     * performGlobalAction(GLOBAL_ACTION_RECENTS); // 模拟最近任务键
     * GLOBAL_ACTION_LOCK_SCREEN // 锁屏 APIlevel28
     *
     * 已使用的performAction（isClickable必须是true）：
     * ACTION_CLICK
     * ACTION_SCROLL_FORWARD
     *
     * event.getEventType：
     * TYPE_ANNOUNCEMENT 播报（本服务的命令注入通道）
     * TYPE_WINDOW_STATE_CHANGED:32 窗口显示
     * TYPE_WINDOW_CONTENT_CHANGED:2048
     *
     * @param event
     */
    // 无障碍事件回调入口：捕获 TYPE_ANNOUNCEMENT 事件中携带的 "#@#" 指令，分发到下方对应的脚本操作分支执行
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            int eventType = event.getEventType();

            // 仅处理 TYPE_ANNOUNCEMENT 事件且文本恰好一条的情况：控制端通过无障碍播报事件注入 "#@#" 指令
            if (event.getText().size() == 1 && eventType == AccessibilityEvent.TYPE_ANNOUNCEMENT) {
                String cmd = event.getText().get(0) == null ? null : event.getText().get(0).toString();

                // 同步操作id
                String msgid = "";
                if (cmd != null && cmd.contains("{msgid}")) {
                    String[] cmds = StringUtil.split(cmd, "{msgid}");
                    msgid = cmds[0];
                    cmd = cmds[1];
                }

                if (cmd != null && cmd.startsWith("#@#")) {
                    Log.e(TAG, cmd);
                    if ("#@#debug#".equals(cmd)) {
                        // 调试查找view
                        debugRun();

                    } else if (cmd.startsWith("#@#tap#")) { // #@#tap#320,2185
                        // 点击坐标
                        String[] xy = cmd.substring("#@#tap#".length()).split(",");
                        Tap(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));

                    } else if (cmd.startsWith("#@#longtap#")) { // #@#longtap#320,2185
                        // 长按坐标
                        String[] xy = cmd.substring("#@#longtap#".length()).split(",");
                        LongTap(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));

                    } else if ("#@#swipe#L".equals(cmd)) {
                        // 长距离下拉，手机分辨率必须大于985*1600
                        Swipe(985, 600, 985, 1600);

                    } else if ("#@#swipe#S".equals(cmd)) {
                        // 短距离下拉
                        Swipe(985, 600, 985, 700);

                    } else if ("#@#action#home".equals(cmd)) {
                        // 模拟全局操作，按home键
                        performGlobalAction(GLOBAL_ACTION_HOME);

                    } else if ("#@#action#recents".equals(cmd)) {
                        // 模拟全局操作，最近任务
                        performGlobalAction(GLOBAL_ACTION_RECENTS);

                    } else if ("#@#action#lock_screen".equals(cmd)) {
                        // 模拟全局操作，锁屏
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
                        }

                    } else if ("#@#danxiangkehu#".equals(cmd)) { // 单向客户
                        // 处理"单向客户"指令：进入编辑模式后用多 stroke 手势逐个勾选所有客户行，再点击"删除"完成清理
                        if (!isRunning) {
                            return;
                        }
                        try {
                            // 手势执行期间跳过新命令，避免打断正在进行的勾选手势
                            if (isGesturing) {
                                Log.e(TAG, "手势执行中, 跳过本次命令");
                                return;
                            }
                            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                            if (rootNode == null) {
                                return;
                            }
                            List<AccessibilityNodeInfo> sclxrNodes = rootNode.findAccessibilityNodeInfosByText("删除联系人");
                            List<AccessibilityNodeInfo> wcNodes = rootNode.findAccessibilityNodeInfosByText("完成");

                            if (sclxrNodes.size() >= 1) {
                                // 删除联系人确认框 -> 点击删除联系人
                                AccessibilityNodeInfo sclxrNode = sclxrNodes.get(0);
                                Rect sclxrRect = new Rect();
                                sclxrNode.getBoundsInScreen(sclxrRect);
                                ThreadUtil.sleep(300);
                                _Tap(sclxrRect.centerX(), sclxrRect.centerY(), 400L, null);
                                Log.e(TAG, "点击 删除联系人 " + sclxrRect);


                            } else if (wcNodes.size() >= 1) {
                                // 在编辑模式（看到"完成"按钮）
                                List<AccessibilityNodeInfo> scNodes = rootNode.findAccessibilityNodeInfosByText("删除");

                                // 检查是否在"选择单向客户"状态（未选择）
                                boolean isSelecting = false;
                                for (AccessibilityNodeInfo node : rootNode.findAccessibilityNodeInfosByText("选择单向客户")) {
                                    if ("选择单向客户".equals(node.getText() + "")) {
                                        isSelecting = true;
                                        break;
                                    }
                                }

                                // 检查是否已选择
                                boolean hasSelected = false;
                                for (AccessibilityNodeInfo node : rootNode.findAccessibilityNodeInfosByText("已选择")) {
                                    if ((node.getText() + "").startsWith("已选择")) {
                                        hasSelected = true;
                                        break;
                                    }
                                }

                                if (isSelecting && !hasSelected) {
                                    // 未选择状态 -> 多短stroke逐个勾选
                                    // 通过"@微信"文本查找所有客户行的Y坐标
                                    List<AccessibilityNodeInfo> customerNodes = rootNode.findAccessibilityNodeInfosByText("@微信");

                                    // 收集所有客户行Y坐标，去重排序
                                    List<Integer> rowYs = new ArrayList<>();
                                    for (AccessibilityNodeInfo node : customerNodes) {
                                        Rect r = new Rect();
                                        node.getBoundsInScreen(r);
                                        int y = r.centerY();
                                        boolean dup = false;
                                        for (int y2 : rowYs) {
                                            if (Math.abs(y - y2) < 30) { dup = true; break; }
                                        }
                                        if (!dup) rowYs.add(y);
                                    }
                                    rowYs.sort(Integer::compare);

                                    if (rowYs.isEmpty() || scNodes.isEmpty()) {
                                        Log.e(TAG, "未找到客户行(" + rowYs.size() + ")或删除按钮(" + scNodes.size() + ")");
                                        return;
                                    }

                                    // 复选框X坐标：屏幕宽度的6%
                                    DisplayMetrics dm = WindowHelper.getRealMetrics();
                                    int checkX = (int)(dm.widthPixels * 0.06f);

                                    // 构建多短stroke手势：把所有复选框点击打包成单个手势，一条 stroke 对应一个复选框点击
                                    long strokeDuration = STROKE_DURATION_MS;
                                    long strokeInterval = STROKE_INTERVAL_MS;
                                    GestureDescription.Builder builder = new GestureDescription.Builder();
                                    for (int i = 0; i < rowYs.size(); i++) {
                                        Path p = new Path();
                                        p.moveTo(checkX, rowYs.get(i));
                                        p.lineTo(checkX + 1, rowYs.get(i));
                                        long startTime = (long) i * strokeInterval;
                                        builder.addStroke(new GestureDescription.StrokeDescription(p, startTime, strokeDuration));
                                    }
                                    GestureDescription gesture = builder.build();

                                    Log.e(TAG, "多stroke勾选 " + rowYs.size() + " 个, checkX=" + checkX + " interval=" + strokeInterval + "ms");
                                    for (int i = 0; i < rowYs.size(); i++) {
                                        Log.e(TAG, "  stroke[" + i + "] (" + checkX + "," + rowYs.get(i) + ") startTime=" + (i * strokeInterval));
                                    }

                                    isGesturing = true;
                                    dispatchGesture(gesture, new GestureResultCallback() {
                                        @Override
                                        public void onCompleted(GestureDescription gestureDescription) {
                                            super.onCompleted(gestureDescription);
                                            isGesturing = false;
                                            Log.e(TAG, "勾选完成, 点击删除");
                                            // 勾选完成 -> 点击删除
                                            ThreadUtil.async(() -> {
                                                ThreadUtil.sleep(500);
                                                AccessibilityNodeInfo root2 = getRootInActiveWindow();
                                                if (root2 != null) {
                                                    List<AccessibilityNodeInfo> delNodes = root2.findAccessibilityNodeInfosByText("删除");
                                                    for (AccessibilityNodeInfo delNode : delNodes) {
                                                        if ("删除".equals(delNode.getText() + "")) {
                                                            Rect dr = new Rect();
                                                            delNode.getBoundsInScreen(dr);
                                                            _Tap(dr.centerX(), dr.centerY(), 400L, null);
                                                            Log.e(TAG, "点击删除 " + dr);
                                                            break;
                                                        }
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onCancelled(GestureDescription gestureDescription) {
                                            super.onCancelled(gestureDescription);
                                            isGesturing = false;
                                            Log.e(TAG, "勾选被取消");
                                        }
                                    }, null);

                                } else if (hasSelected) {
                                    // 已选择状态 -> 点击删除
                                    for (AccessibilityNodeInfo delNode : scNodes) {
                                        if ("删除".equals(delNode.getText() + "")) {
                                            Rect dr = new Rect();
                                            delNode.getBoundsInScreen(dr);
                                            _Tap(dr.centerX(), dr.centerY(), 400L, null);
                                            Log.e(TAG, "已选择状态点击删除 " + dr);
                                            break;
                                        }
                                    }
                                } else {
                                    Log.e(TAG, "非选择状态");
                                }
                            } else {
                                Log.e(TAG, "删除loading");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "danxiangkehu异常: " + e.getMessage());
                            ExceptionUtil.getStackTrace(e);
                        }
                        ThreadUtil.sleep(700);
                    } else if ("#@#全部清除#".equals(cmd)) { // 如果有"全部清除"就点击
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId("com.motorola.launcher3:id/action_clear_all");
                        for (AccessibilityNodeInfo node : nodes) {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            Log.e(TAG, "点击 " + node.getText());
                        }

                    } else if ("#@#上划解锁#".equals(cmd)) {
                        DisplayMetrics dm = WindowHelper.getRealMetrics();
                        float w = dm.widthPixels;
                        float h = dm.heightPixels;
                        Log.e(TAG, "分辨率[" + w + ", " + h + "]");

                        int x = (int) (w / 2);
                        int y1 = (int) (h * 0.75);
                        Swipe(x, y1, x, 10);

                    } else if (cmd.startsWith("#@#打开企业微信")) {
                        String index = cmd.substring("#@#打开企业微信".length()); // 第0-5个企业微信
                        String appname = "0".equals(index) ? "企业微信" : "企业微信 " + index;

                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(appname);
                        boolean hasBtn = false;
                        for (AccessibilityNodeInfo node : nodes) { // 第一次拉起APP，多开APP选择列表显示不全，但是也能ACTION_CLICK进行点击
                            if (appname.equals(node.getText() + "")) {
                                node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                Log.e(TAG, "===== 点击 " + node.getText());
                                hasBtn = true;
                                break;
                            }
                        }
                        if (!hasBtn) {
                            // "TextView-使用企业微信 5完成操作[179,1090,1013,1151]android:id/title|false" : null,
                            // "Button-仅此一次[630,1268,852,1419]android:id/button_once|true" : null,
                            List<AccessibilityNodeInfo> shiyongNodes = rootNode.findAccessibilityNodeInfosByText("使用" + appname + "完成操作");
                            List<AccessibilityNodeInfo> jinciyiciNodes = rootNode.findAccessibilityNodeInfosByText("仅此一次");
                            if (shiyongNodes.size() > 0 && jinciyiciNodes.size() > 0) { // 非第一次拉取并打开APP，最后一个多开APP变成默认仅此一次选项
                                AccessibilityNodeInfo jinciyiciNode = jinciyiciNodes.get(0);
                                jinciyiciNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                Log.e(TAG, "===== 点击 " + appname + " " + jinciyiciNode.getText());
                            } else { // 6个企微多开未安装全，未找到的不操作，等待下一步命令判断打开失败跳过
                                Log.e(TAG, "===== 打开失败 " + appname);
                            }
                        }

                    } else if ("#@#通讯录#".equals(cmd)) {
                        String find = "通讯录";
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(find);
                        boolean hasBtn = false;
                        for (AccessibilityNodeInfo node : nodes) {
                            if (find.equals(node.getText() + "")) {
                                node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                Log.e(TAG, "点击 " + node.getText());
                                hasBtn = true;
                                break;
                            }
                        }
                        if (hasBtn) {
                            response(msgid, "success");
                        } else {
                            response(msgid, "error");
                        }

                    } else if ("#@#我的客户#".equals(cmd)) {
                        String find = "我的客户";
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(find);
                        for (AccessibilityNodeInfo node : nodes) {
                            if (find.equals(node.getText() + "") &&
                                    (node.getParent().getClassName() + "").contains("ViewGroup")) {
                                node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                Log.e(TAG, "点击 " + node.getText());
                                break;
                            }
                        }

                    } else if ("#@#全部微信客户#".equals(cmd)) {
                        String find = "全部微信客户";
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(find);
                        boolean hasBtn = false;
                        for (AccessibilityNodeInfo node : nodes) {
                            if (find.equals((node.getText() + "").trim())) {
                                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                Log.e(TAG, "点击 " + node.getText());
                                hasBtn = true;
                                break;
                            }
                        }
                        if (hasBtn) {
                            response(msgid, "success");
                        } else {
                            response(msgid, "error");
                        }

                    } else if ("#@#单向微信客户#".equals(cmd)) {
                        String find = "单向微信客户";
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(find);
                        for (AccessibilityNodeInfo node : nodes) {
                            if (find.equals(node.getText() + "")) {
                                node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                Log.e(TAG, "点击 " + node.getText());
                                break;
                            }
                        }

                    } else if ("#@#编辑#".equals(cmd)) {
                        String find = "编辑";
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(find);
                        for (AccessibilityNodeInfo node : nodes) {
                            if (find.equals(node.getText() + "")) {
                                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                Log.e(TAG, "点击 " + node.getText());
                                break;
                            }
                        }

                    } else if ("#@#共n个客户#".equals(cmd)) {
                        String find = "个客户";
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> zwdxkhNodes = rootNode.findAccessibilityNodeInfosByText("暂无单向客户");
                        if (zwdxkhNodes.size() > 0) {
                            Log.e(TAG, "----- " + zwdxkhNodes.get(0).getText());
                            response(msgid, "break");
                        } else {
                            List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(find);
                            boolean canBreak = false;
                            for (AccessibilityNodeInfo node : nodes) {
                                String text = node.getText() + "";
                                if (text.startsWith("共") && text.endsWith("个客户")) {
                                    String countStr = text.replace("共", "").replace("个客户", "");
                                    int countNum = Integer.parseInt(countStr);
                                    Log.e(TAG, text + ": " + countNum);
                                    if (countNum < 10) {
                                        canBreak = true;
                                    }
                                    break;
                                }
                            }
                            if (canBreak) {
                                Log.e(TAG, "----- 共n个客户 break");
                                response(msgid, "break");
                            } else {
                                Log.e(TAG, "----- 共n个客户 continue");
                                response(msgid, "continue");
                            }

                        }

                    }




                }
            }
        } catch (Exception e) {
            Log.e(TAG, "无障碍回调异常");
            ExceptionUtil.getStackTrace(e);
        }
    }

    /* ****************************************** 命令 *********************************************/

    /**
     * 模拟点击事件
     *
     * @param x
     * @param y
     */
    private void Tap(int x, int y) {
        _Tap(x, y, 200L, null);
    }

    /**
     * 模拟长按事件
     *
     * @param x
     * @param y
     */
    private void LongTap(int x, int y) {
        _Tap(x, y, 1000L, null);
    }

    /**
     * 模拟点击事件
     *
     * @param x
     * @param y
     * @param time 长按毫秒
     * @param onCompleted 异步执行完成回调
     */
    private void _Tap(int x, int y, long time, Runnable onCompleted) {
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path p = new Path();
        p.moveTo(x , y);
        builder.addStroke(new GestureDescription.StrokeDescription(p, 0L, time));
        GestureDescription gesture = builder.build();
        dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                //Log.e(TAG, "onCompleted: 完成..........");
                if (onCompleted != null) {
                    onCompleted.run();
                }
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                //Log.e(TAG, "onCompleted: 取消..........");
            }
        }, null);
    }

    /**
     * 模拟滑动事件
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     */
    private void Swipe(int x1, int y1, int x2, int y2) {
        _Swipe(x1, y1, x2, y2, 2000L, null);
    }

    /**
     * 模拟滑动事件
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param time 滑动总时长
     * @param onCompleted 异步执行完成回调
     */
    private void _Swipe(int x1, int y1, int x2, int y2, long time, Runnable onCompleted) {
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path p = new Path();
        p.moveTo(x1 , y1);
        p.lineTo(x2 , y2);
        builder.addStroke(new GestureDescription.StrokeDescription(p, 0L, time)); // startTime 0即刻执行
        GestureDescription gesture = builder.build();
        dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                //Log.e(TAG, "onCompleted: 完成..........");
                if (onCompleted != null) {
                    onCompleted.run();
                }
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                //Log.e(TAG, "onCompleted: 取消..........");
            }
        }, null);
    }

    /**
     * 递归遍历无障碍节点树，把每个子节点序列化为"类名-文本-屏幕边界-viewId-是否可点击"的键，
     * 并将整棵节点树结构写入 parentMap，供调试时打印当前界面的节点树。
     */
    private void _debugGet(AccessibilityNodeInfo node, Map<String, Object> parentMap) {
        if (node == null) {
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) {
                continue;
            }

            String className = (child.getClassName() + "")
                    .replace("android.widget.", "")
                    .replace("androidx.recyclerview.widget.", "")
                    .replace("androidx.viewpager.widget.", "")
                    .replace("android.webkit.", "")
                    .replace("android.view.", "");
            String text = child.getText() == null ? "" : child.getText().toString();
            Rect rect = new Rect();
            child.getBoundsInScreen(rect);
            String bounds = "[" + rect.left + "," + rect.top + "," +
                    rect.right + "," + rect.bottom + "]"; // x1 y1 x2 y2
            String viewId = Objects.requireNonNullElse(child.getViewIdResourceName(), "");
            String info = className + "-" + text + bounds + viewId + "|" + child.isClickable();

            if (child.getChildCount() == 0) {
                parentMap.put(info, null);
            } else {
                Map<String, Object> childMap = new LinkedHashMap<>();
                parentMap.put(info, childMap);
                _debugGet(child, childMap);
            }
        }
    }

    /**
     * 调试查找view
     */
    private void debugRun() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        Map<String, Object> parentMap = new LinkedHashMap<>();
        _debugGet(rootNode, parentMap);

        String json = Codec.json_encode_pretty(parentMap);
        String[] lines = StringUtil.split(json, "\n");
        for (String line : lines) {
            Log.d(TAG, line);
        }
    }

    /**
     * 同步操作返回
     * @param msgid
     * @param res
     */
    private synchronized void response(String msgid, String res) {
        MMKV kv = UI.getMMKV();
        kv.putString(msgid, res, 3600);
        Log.d(TAG, "response " + msgid + "->" + res);
    }
}
