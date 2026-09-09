package com.google.android.accessibility.selecttospeak;

import static com.yaonan.util.global.Global.TAG;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tencent.mmkv.MMKV;
import com.yaonan.util.WindowHelper;
import com.yaonan.util.codec.Codec;
import com.yaonan.util.exception.ExceptionUtil;
import com.yaonan.util.http.API;
import com.yaonan.util.http.Response;
import com.yaonan.util.jna.UI;
import com.yaonan.util.lang.StringUtil;
import com.yaonan.util.lang.ThreadUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SelectToSpeakService extends AccessibilityService {

    public static volatile boolean isRunning = false;
    private static volatile boolean isGesturing = false;

    @Override
    protected void onServiceConnected() {
        //Log.e(TAG, "无障碍服务启动");
        super.onServiceConnected();
    }

    @Override
    public void onInterrupt() {

    }

    /**
     * 为UI线程
     *
     *
     * 有没有用？
     * |flagIncludeNotImportantViews|flagRetrieveInteractiveWindows
     *
     *
     * 如何查找指定view节点：
     *  1直接通过findAccessibilityNodeInfosByText findAccessibilityNodeInfosByViewId获取view节点
     *  2先通过GestureDescription点击、再findFocus获取view节点
     *  3通过Id、Text或findFocus找到临近节点、再通过getParent和getChild(i)并结合各节点特定属性、定位到view节点
     *  4直接通过getRootInActiveWindow得到根节点、再getChild(i)判断每级节点特定属性、逐级判断定位到view节点
     *  找到view节点后，如performAction无法操作，可结合节点Rect、图片比对坐标、GestureDescription、输入法按钮等继续操作
     *
     *
     * 执行全局动作，api16以上可用
     * 已使用performGlobalAction(GLOBAL_ACTION_HOME); // 按home键
     * 已使用performGlobalAction(GLOBAL_ACTION_BACK); // 按back键
     * GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN // 切换到分屏
     * GLOBAL_ACTION_QUICK_SETTINGS // 打开快速设置，暂时不知道这个有什么用
     * GLOBAL_ACTION_NOTIFICATIONS // 打开通知栏
     * 已使用GLOBAL_ACTION_RECENTS // 模拟最近任务键
     * GLOBAL_ACTION_POWER_DIALOG // 打开电源键长按对话框
     * 已使用GLOBAL_ACTION_LOCK_SCREEN // 锁屏 APIlevel28
     * GLOBAL_ACTION_TAKE_SCREENSHOT // 截图 APIlevel28
     *
     *
     * performAction：聚焦 选中 点击 滚屏 复制粘贴 输入文本等
     * ACTION_FOCUS
     * ACTION_CLEAR_FOCUS
     * ACTION_SELECT
     * ACTION_CLEAR_SELECTION
     * 已使用ACTION_CLICK isClickable必须是true
     * ACTION_LONG_CLICK
     * ACTION_ACCESSIBILITY_FOCUS
     * ACTION_CLEAR_ACCESSIBILITY_FOCUS
     * ACTION_NEXT_AT_MOVEMENT_GRANULARITY
     * ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
     * ACTION_NEXT_HTML_ELEMENT
     * ACTION_PREVIOUS_HTML_ELEMENT
     * 已使用ACTION_SCROLL_FORWARD
     * ACTION_SCROLL_BACKWARD
     * ACTION_COPY
     * 已使用ACTION_PASTE
     * ACTION_CUT
     * ACTION_SET_SELECTION
     * ACTION_EXPAND
     * ACTION_COLLAPSE
     * ACTION_DISMISS
     * 已使用ACTION_SET_TEXT
     *
     *
     * event.getEventType：
     * TYPE_VIEW_CLICKED:1 点击
     * TYPE_VIEW_LONG_CLICKED:2 输入框选中文本等长按
     * TYPE_VIEW_SELECTED:4 聊天文本等选中
     * TYPE_VIEW_FOCUSED:8 输入框等点击进入
     * TYPE_VIEW_TEXT_CHANGED:16 输入框文本变动
     * TYPE_WINDOW_STATE_CHANGED:32 窗口显示
     * TYPE_NOTIFICATION_STATE_CHANGED:64 通知Toast等
     * TYPE_VIEW_HOVER_EXIT:256 鼠标移出
     * TYPE_WINDOW_CONTENT_CHANGED:2048
     * TYPE_VIEW_SCROLLED:4096 滚屏
     * TYPE_VIEW_TEXT_SELECTION_CHANGED:8192 输入框选中位置变更
     *
     *
     * @param event
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            int eventType = event.getEventType();

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
                    //ThreadUtil.sleep(1000);
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

                    } else if ("#@#action#back".equals(cmd)) {
                        // 模拟全局操作，按back键
                        performGlobalAction(GLOBAL_ACTION_BACK);

                    } else if ("#@#action#recents".equals(cmd)) {
                        // 模拟全局操作，最近任务
                        performGlobalAction(GLOBAL_ACTION_RECENTS);

                    } else if ("#@#action#lock_screen".equals(cmd)) {
                        // 模拟全局操作，锁屏
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
                        }

                    } else if ("#@#action#paste".equals(cmd)) {
                        // 方法1、已聚集输入框，粘贴文字
                        // 微信performAction基本无效，应使用GestureDescription如利用输入法粘贴按钮输入
                        AccessibilityNodeInfo focusNode = findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                        if (focusNode != null) {
                            Log.e(TAG, "聚焦节点 粘贴" + focusNode.getPackageName() + "#" + focusNode.getClassName());
                            focusNode.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                        } else {
                            Log.e(TAG, "聚焦节点 null");
                        }
                        // 方法2、已聚集输入框，输入文字
                        /*Bundle arguments = new Bundle();
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "输入的文本");
                        focusNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);*/

                    } else if ("#@#find#yjbm".equals(cmd)) {
                        // 查找指定view节点并点击
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> yjbmNodes = rootNode.findAccessibilityNodeInfosByText("一键帮卖");
                        for (AccessibilityNodeInfo yjbmNode : yjbmNodes) {
                            Log.e(TAG, "点击 一键帮卖");
                            //ThreadUtil.sleep(1000);
                            yjbmNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }

                    } else if ("#@#find#x".equals(cmd)) {
                        // 查找指定view节点并点击
                        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> xNodes = rootNodeInfo.findAccessibilityNodeInfosByText("立即分享，邀请好友参团");
                        AAA:
                        for (AccessibilityNodeInfo xNode : xNodes) {
                            AccessibilityNodeInfo xNodeParent = xNode.getParent();

                            StringBuilder goodname = new StringBuilder();
                            for (int i = 0; i < xNodeParent.getChildCount(); i++) {
                                AccessibilityNodeInfo xNodeSibling = xNodeParent.getChild(i);
                                CharSequence text = xNodeSibling.getText();
                                if (text != null) {
                                    goodname.append(i).append(".").append(text).append("\n");
                                }
                            }

                            for (int i = 0; i < xNodeParent.getChildCount(); i++) {
                                AccessibilityNodeInfo xNodeSibling = xNodeParent.getChild(i);
                                if (xNodeSibling.getClassName().toString().contains("ImageView")) {
                                    Log.e(TAG, "点击 x");
                                    //ThreadUtil.sleep(1000);
                                    xNodeSibling.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                                    // 成功一次->http通知服务器，服务器心跳断了报警（performAction返回值判断？http发送弹窗中xNodeSibling商品名称，用于判断是否每次不一样）
                                    ThreadUtil.async(() -> {
                                        SharedPreferences sharedPreferences = UI.getSharedPreferences();
                                        String userName = sharedPreferences.getString("user_name", "");
                                        String url = "http://ynkey.fenfentuan.com?name=" + Codec.url_encode(userName) + "&good=" + Codec.url_encode(goodname.toString());
                                        Response resp = API.sendOriginal(false, url, null, null);
                                        if (resp.isError()) {
                                            Log.e(TAG, Codec.json_encode(resp));
                                        } else {
                                            Log.d(TAG, userName + ":" + resp.getBody());
                                        }
                                    });

                                    break AAA;
                                }
                            }
                        }

                    } else if ("#@#find#qhsf".equals(cmd)) { // 点击切换身份
                        // 查找指定view节点并点击
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> qhsfNodes = rootNode.findAccessibilityNodeInfosByViewId("com.xunmeng.kuaituantuan:id/account_desc_tv");
                        for (AccessibilityNodeInfo qhsfNode : qhsfNodes) {
                            Log.e(TAG, "点击 " + qhsfNode.getText());
                            //ThreadUtil.sleep(1000);
                            qhsfNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }

                    } else if (cmd.startsWith("#@#find#sf")) { // 点击身份index
                        // 查找指定view节点并点击
                        // 切换身份列表{"ViewGroup-[0,96,1080,2356]":{"FrameLayout-[0,96,1080,2356]":{"View-[0,96,1080,849]":null,"TextView-切换身份[46,895,254,965]":null,"ImageView-[976,895,1034,953]":null,"RecyclerView-[0,1012,1080,2298]":{"RelativeLayout-[0,1012,1080,1150]":{"TextView-呆呆呢[132,1052,261,1109]":null,"TextView-创建者[273,1051,402,1110]":null},"RelativeLayout-[0,1150,1080,1288]":{"TextView-团团尖货精选🥇订阅立减[132,1190,616,1247]":null,"TextView-管理员[628,1189,757,1248]":null},"RelativeLayout-[0,1288,1080,1426]":{"TextView-豆妈优选【点头像领红包】[132,1328,648,1385]":null,"TextView-管理员[660,1327,789,1386]":null},"RelativeLayout-[0,1426,1080,1564]":{"TextView-米家今日精选【点头像领红[132,1466,648,1523]":null,"TextView-管理员[660,1465,789,1524]":null},"RelativeLayout-[0,1564,1080,1702]":{"TextView-米家每日精选【点头像领红[132,1604,648,1661]":null,"TextView-管理员[660,1603,789,1662]":null},"RelativeLayout-[0,1702,1080,1840]":{"TextView-纷纷团精选【点头像领红包[132,1742,648,1799]":null,"TextView-管理员[660,1741,789,1800]":null},"RelativeLayout-[0,1840,1080,1978]":{"TextView-Button[132,1880,266,1937]":null,"TextView-管理员[278,1879,407,1938]":null},"RelativeLayout-[0,1978,1080,2116]":{"TextView-豆妈甄选好物【点头像领红[132,2018,648,2075]":null,"TextView-管理员[660,2017,789,2076]":null},"RelativeLayout-[0,2116,1080,2254]":{"TextView-团团超级精选(开单领红包[132,2156,618,2213]":null,"TextView-管理员[630,2155,759,2214]":null},"RelativeLayout-[0,2254,1080,2298]":{"TextView-好物推荐【点头像领红包】[132,2294,648,2298]":null,"TextView-管理员[660,2293,789,2298]":null}}}}}
                        String index = cmd.substring("#@#find#sf".length()); // 第几个管理员
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

                        List<AccessibilityNodeInfo> nameNodes = rootNode.findAccessibilityNodeInfosByViewId("com.xunmeng.kuaituantuan:id/account_name_tv");
                        if (nameNodes.size() > 0) {
                            // 角色列表第二页，重新计算点击的index
                            // TODO 只能返回可显示区域的节点，所以需要根据屏幕尺寸适配
                            int curr = Integer.parseInt(index);
                            if (curr >= 9) {
                                curr = curr - 9;
                            }
                            AccessibilityNodeInfo nameNode = nameNodes.get(curr);

                            AccessibilityNodeInfo sfNode = nameNode.getParent();
                            String name = nameNode.getText() + "";
                            Log.e(TAG, "点击 " + name);
                            //ThreadUtil.sleep(1000);
                            sfNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                            // 保存，第index账号是name
                            ThreadUtil.async(() -> {
                                String url = "https://t2.tuielf.com/api/ynkey/data";
                                String urlEncoded = "type=user&no=" + index + "&name=" + Codec.url_encode(name);
                                Response resp = API.sendOriginal(true, url, null, urlEncoded.getBytes());
                                if (resp.isError()) {
                                    Log.e(TAG, Codec.json_encode(resp));
                                } else {
                                    Log.d(TAG, index + ":" + resp.getBody());
                                }
                            });
                        }

                    } else if ("#@#find#fy".equals(cmd)) { // index>=9，角色列表先翻页
                        // 查找指定view节点并翻页
                        // TODO 只能返回可显示区域的节点，所以需要根据屏幕尺寸适配
                        // 第一页，包含好物推荐 {"ViewGroup-[0,96,1080,2356]":{"FrameLayout-[0,96,1080,2356]":{"View-[0,96,1080,849]":null,"TextView-切换身份[46,895,254,965]":null,"ImageView-[976,895,1034,953]":null,"RecyclerView-[0,1012,1080,2298]":{"RelativeLayout-[0,1012,1080,1150]":{"TextView-呆呆呢[132,1052,261,1109]":null,"TextView-创建者[273,1051,402,1110]":null},"RelativeLayout-[0,1150,1080,1288]":{"TextView-团团尖货精选🥇订阅立减[132,1037,616,1094]":null},"RelativeLayout-[0,1288,1080,1426]":{"TextView-豆妈优选【点头像领红包】[132,1020,648,1077]":null,"TextView-管理员[660,1019,789,1078]":null},"RelativeLayout-[0,1426,1080,1564]":{"TextView-米家今日精选【点头像领红[132,1466,648,1523]":null,"TextView-管理员[660,1465,789,1524]":null},"RelativeLayout-[0,1564,1080,1702]":{"TextView-米家每日精选【点头像领红[132,1604,648,1661]":null,"TextView-管理员[660,1603,789,1662]":null},"RelativeLayout-[0,1702,1080,1840]":{"TextView-纷纷团精选【点头像领红包[132,1742,648,1799]":null,"TextView-管理员[660,1741,789,1800]":null},"RelativeLayout-[0,1840,1080,1978]":{"TextView-Button[132,1572,266,1629]":null,"TextView-管理员[278,1441,407,1500]":null},"RelativeLayout-[0,1978,1080,2116]":{"TextView-豆妈甄选好物【点头像领红[132,1542,648,1599]":null,"TextView-管理员[660,1541,789,1600]":null},"RelativeLayout-[0,2116,1080,2254]":{"TextView-团团超级精选(开单领红包[132,2156,618,2213]":null,"TextView-管理员[630,2155,759,2214]":null},"RelativeLayout-[0,2254,1080,2298]":{"TextView-好物推荐【点头像领红包】[132,2294,648,2298]":null,"TextView-管理员[660,2293,789,2298]":null}}}}}
                        // 第二页，包含好物推荐 {"ViewGroup-[0,96,1080,2356]":{"FrameLayout-[0,96,1080,2356]":{"View-[0,96,1080,849]":null,"TextView-切换身份[46,895,254,965]":null,"ImageView-[976,895,1034,953]":null,"RecyclerView-[0,1012,1080,2298]":{"RelativeLayout-[0,1012,1080,1106]":{"TextView-好物推荐【点头像领红包】[132,1012,648,1065]":null,"TextView-管理员[660,1012,789,1066]":null},"RelativeLayout-[0,1106,1080,1244]":{"TextView-豆妈精选【点头像领红包】[132,1104,648,1161]":null,"TextView-管理员[660,1145,789,1204]":null},"RelativeLayout-[0,1244,1080,1382]":{"TextView-one 🦩[132,1284,272,1341]":null,"TextView-管理员[284,1283,413,1342]":null},"RelativeLayout-[0,1382,1080,1520]":{"TextView-优团奇妙优品[132,1422,390,1479]":null,"TextView-管理员[402,1421,531,1480]":null},"RelativeLayout-[0,1520,1080,1658]":{"TextView-L29[132,1560,206,1617]":null,"TextView-管理员[218,1559,347,1618]":null},"RelativeLayout-[0,1658,1080,1796]":{"TextView-leo[132,1698,192,1755]":null,"TextView-管理员[204,1697,333,1756]":null},"RelativeLayout-[0,1796,1080,1934]":{"TextView-团团[132,1836,218,1893]":null,"TextView-管理员[230,1835,359,1894]":null},"RelativeLayout-[0,1934,1080,2072]":{"TextView-安妮精选【点头像领红包】[132,1974,648,2031]":null,"TextView-管理员[660,1973,789,2032]":null},"RelativeLayout-[0,2072,1080,2210]":{"TextView-诚哥源头好货【点头像领红[132,2112,648,2169]":null,"TextView-管理员[660,2111,789,2170]":null},"RelativeLayout-[0,2210,1080,2298]":{"TextView-小刘[132,2250,218,2298]":null,"TextView-管理员[230,2249,359,2298]":null}}}}}
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> itemListNodes = rootNode.findAccessibilityNodeInfosByViewId("com.xunmeng.kuaituantuan:id/item_list");
                        for (AccessibilityNodeInfo itemListNode : itemListNodes) {
                            itemListNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                            break;
                        }

                    } else if (cmd.startsWith("#@#find#ckgd")) { // 点击展开更多
                        // 查找指定view节点并点击
                        // 查看更多{"androidx.viewpager.widget.ViewPager-[0,0,1220,2475]":{"ScrollView-[0,104,1220,2475]":{"ImageView-[52,169,202,319]":null,"TextView-1+1[235,173,314,242]":null,"TextView-创建者[327,181,497,233]":null,"ImageView-[235,262,508,314]":null,"FrameLayout-[39,358,276,511]":{"TextView-¥[122,457,145,509]":null,"TextView-0.1[145,450,192,511]":null},"FrameLayout-[276,358,502,511]":{"TextView-团员[350,459,428,511]":null},"FrameLayout-[502,358,728,511]":{"TextView-团长[576,459,654,511]":null},"FrameLayout-[728,358,954,511]":{"TextView-店铺[802,459,880,511]":null},"FrameLayout-[954,358,1181,511]":{"TextView-社群[1028,459,1106,511]":null},"TextView-数据中心[78,596,274,661]":null,"TextView-02/10 17:51更新[294,605,633,657]":null,"TextView-查看更多[947,596,1142,667]":null,"TextView-￥[187,706,233,784]":null,"TextView-0[233,709,271,784]":null,"TextView-今日订单金额[112,791,346,843]":null,"TextView-0[590,706,628,784]":null,"TextView-今日订单数[512,791,707,843]":null,"TextView-0[971,706,1009,784]":null,"TextView-今日下单人数[873,791,1107,843]":null,"TextView-团长功能[39,980,313,1045]":null,"RecyclerView-[39,1045,1181,1487]":{"FrameLayout-[39,1071,267,1279]":{"TextView-我的团购[39,1201,267,1253]":null},"FrameLayout-[267,1071,495,1279]":{"TextView-快团团小程序[267,1201,495,1253]":null},"FrameLayout-[496,1071,724,1279]":{"TextView-商品核销[496,1201,724,1253]":null},"FrameLayout-[724,1071,952,1279]":{"TextView-自提点管理[724,1201,952,1253]":null},"FrameLayout-[952,1071,1180,1279]":{"TextView-积分商城[952,1201,1180,1253]":null},"FrameLayout-[39,1279,267,1487]":{"TextView-输入法[39,1409,267,1461]":null},"FrameLayout-[267,1279,495,1487]":{"TextView-我的供货商[267,1409,495,1461]":null},"FrameLayout-[496,1279,724,1487]":{"TextView-官方客服[496,1409,724,1461]":null}},"ImageView-[0,1552,1220,1776]":null,"TextView-素材工具[39,1861,313,1926]":null,"RecyclerView-[39,1926,1181,2408]":{"android.view.ViewGroup-[39,1952,267,2180]":{"TextView-我的相册[80,2083,226,2135]":null},"android.view.ViewGroup-[267,1952,495,2180]":{"TextView-朋友圈抓图[290,2083,472,2135]":null},"android.view.ViewGroup-[496,1952,724,2180]":{"TextView-批量抓图[537,2083,683,2135]":null},"android.view.ViewGroup-[724,1952,952,2180]":{"TextView-群发至群聊[747,2083,929,2135]":null},"android.view.ViewGroup-[952,1952,1180,2180]":{"TextView-群发给好友[975,2083,1157,2135]":null},"android.view.ViewGroup-[39,2180,267,2408]":{"TextView-VIP特权[91,2311,216,2363]":null},"android.view.ViewGroup-[267,2180,495,2408]":{"TextView-相册清理[308,2311,454,2363]":null},"android.view.ViewGroup-[496,2180,724,2408]":{"TextView-批量发朋友圈[501,2311,719,2363]":null},"android.view.ViewGroup-[724,2180,952,2408]":{"TextView-检查更新[765,2311,911,2363]":null},"android.view.ViewGroup-[952,2180,1180,2408]":{"TextView-相册设置[993,2311,1139,2363]":null}}}},"HorizontalScrollView-[0,2475,1220,2664]":{"LinearLayout-[0,2475,244,2664]":{"TextView-首页[89,2588,155,2631]":null},"LinearLayout-[244,2475,488,2664]":{"TextView-订单[333,2588,399,2631]":null},"LinearLayout-[488,2475,732,2664]":{"TextView-一键开团[544,2588,676,2631]":null},"LinearLayout-[732,2475,976,2664]":{"TextView-2[867,2475,932,2540]":null,"TextView-消息[821,2588,887,2631]":null},"LinearLayout-[976,2475,1220,2664]":{"TextView-个人中心[1032,2588,1164,2631]":null}}}
                        //        {"ViewPager-[0,0,1220,2475]":{"FrameLayout-[0,0,1220,2475]":{"ScrollView-[0,104,1220,2475]":{"ImageView-[52,169,202,319]":null,"TextView-团团尖货精选🥇订阅立减5元[235,173,904,242]":null,"TextView-管理员[917,181,1087,233]":null,"TextView-带货等级LV.5[281,262,562,314]":null,"ImageView-[575,262,848,314]":null,"FrameLayout-[39,358,276,511]":{"TextView-¥[68,457,91,509]":null,"TextView-14526.18[91,450,246,511]":null},"FrameLayout-[276,358,502,511]":{"TextView-团员[350,459,428,511]":null},"FrameLayout-[502,358,728,511]":{"TextView-团长[576,459,654,511]":null},"FrameLayout-[728,358,954,511]":{"TextView-店铺[802,459,880,511]":null},"FrameLayout-[954,358,1181,511]":{"TextView-社群[1028,459,1106,511]":null},"TextView-数据中心[78,596,274,661]":null,"TextView-02/14 11:31更新[294,605,625,657]":null,"TextView-查看更多[947,596,1142,667]":null,"TextView-￥[112,706,158,784]":null,"TextView-507.54[158,709,345,784]":null,"TextView-今日订单金额[112,791,346,843]":null,"TextView-0[590,706,628,784]":null,"TextView-今日订单数[512,791,707,843]":null,"TextView-0[971,706,1009,784]":null,"TextView-今日下单人数[873,791,1107,843]":null,"TextView-团长功能[39,980,313,1045]":null,"RecyclerView-[39,1045,1181,1487]":{"FrameLayout-[39,1071,267,1279]":{"TextView-我的团购[39,1201,267,1253]":null},"FrameLayout-[267,1071,495,1279]":{"TextView-快团团小程序[267,1201,495,1253]":null},"FrameLayout-[496,1071,724,1279]":{"TextView-商品核销[496,1201,724,1253]":null},"FrameLayout-[724,1071,952,1279]":{"TextView-自提点管理[724,1201,952,1253]":null},"FrameLayout-[952,1071,1180,1279]":{"TextView-积分商城[952,1201,1180,1253]":null},"FrameLayout-[39,1279,267,1487]":{"TextView-输入法[39,1409,267,1461]":null},"FrameLayout-[267,1279,495,1487]":{"TextView-裂变抢券[267,1409,495,1461]":null},"FrameLayout-[496,1279,724,1487]":{"TextView-我的供货商[496,1409,724,1461]":null},"FrameLayout-[724,1279,952,1487]":{"TextView-官方客服[724,1409,952,1461]":null}},"TextView-素材工具[39,1598,313,1663]":null,"RecyclerView-[39,1663,1181,2145]":{"ViewGroup-[39,1689,267,1917]":{"TextView-我的相册[80,1820,226,1872]":null},"ViewGroup-[267,1689,495,1917]":{"TextView-朋友圈抓图[290,1820,472,1872]":null},"ViewGroup-[496,1689,724,1917]":{"TextView-批量抓图[537,1820,683,1872]":null},"ViewGroup-[724,1689,952,1917]":{"TextView-群发至群聊[747,1820,929,1872]":null},"ViewGroup-[952,1689,1180,1917]":{"TextView-群发给好友[975,1820,1157,1872]":null},"ViewGroup-[39,1917,267,2145]":{"TextView-VIP特权[91,2048,216,2100]":null},"ViewGroup-[267,1917,495,2145]":{"TextView-相册清理[308,2048,454,2100]":null},"ViewGroup-[496,1917,724,2145]":{"TextView-批量发朋友圈[501,2048,719,2100]":null},"ViewGroup-[724,1917,952,2145]":{"TextView-检查更新[765,2048,911,2100]":null},"ViewGroup-[952,1917,1180,2145]":{"TextView-相册设置[993,2048,1139,2100]":null}}}}},"HorizontalScrollView-[0,2475,1220,2664]":{"LinearLayout-[0,2475,244,2664]":{"TextView-首页[89,2588,155,2631]":null},"LinearLayout-[244,2475,488,2664]":{"TextView-38[379,2475,464,2540]":null,"TextView-订单[333,2588,399,2631]":null},"LinearLayout-[488,2475,732,2664]":{"TextView-一键开团[544,2588,676,2631]":null},"LinearLayout-[732,2475,976,2664]":{"TextView-消息[821,2588,887,2631]":null},"LinearLayout-[976,2475,1220,2664]":{"TextView-个人中心[1032,2588,1164,2631]":null}}}
                        String index = cmd.substring("#@#find#ckgd".length()); // 第几个管理员
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> ckgdNodes = rootNode.findAccessibilityNodeInfosByText("查看更多");
                        for (AccessibilityNodeInfo ckgdNode : ckgdNodes) {
                            Log.e(TAG, "点击 查看更多");
                            //ThreadUtil.sleep(1000);
                            ckgdNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                            // 保存，第index账号更新name
                            List<AccessibilityNodeInfo> nameNodes = rootNode.findAccessibilityNodeInfosByViewId("com.xunmeng.kuaituantuan:id/user_account_name");
                            for (AccessibilityNodeInfo nameNode : nameNodes) {
                                String name = nameNode.getText() + "";

                                ThreadUtil.async(() -> {
                                    String url = "https://t2.tuielf.com/api/ynkey/data";
                                    String urlEncoded = "type=name&no=" + index + "&name=" + Codec.url_encode(name);
                                    Response resp = API.sendOriginal(true, url, null, urlEncoded.getBytes());
                                    if (resp.isError()) {
                                        Log.e(TAG, Codec.json_encode(resp));
                                    } else {
                                        Log.d(TAG, index + ":" + resp.getBody());
                                    }
                                });

                                break;
                            }
                        }

                    } else if ("#@#find#webview0".equals(cmd)) { // 点击概况日
                        // 查找WebView中指定view节点并Tap点击
                        List<AccessibilityNodeInfo> nodes = findNodeInfos();
                        int minY = Integer.MAX_VALUE;
                        Rect rRect = null; // 节点有多个，采用第一个
                        for (AccessibilityNodeInfo node : nodes) {
                            String text = node.getText() + "";
                            if ("日".equals(text)) {
                                Rect rect = new Rect();
                                node.getBoundsInScreen(rect);
                                if (rect.top < minY) { // y1
                                    minY = rect.top;
                                    rRect = rect;
                                }
                            }
                        }

                        if (rRect != null) {
                            Log.e(TAG, "点击 日" + rRect);
                            //ThreadUtil.sleep(1000);
                            // x1:left y1:top x2:right y2:bottom
                            Tap((rRect.left + rRect.right) / 2, (rRect.top + rRect.bottom) / 2);
                        }

                    } else if ("#@#find#webview1".equals(cmd)) { // 点击用户日、展开更多
                        // 查找WebView中指定view节点并Tap点击：webview中节点经常刷新不出来
                        List<AccessibilityNodeInfo> nodes = findNodeInfos();
                        int minX = Integer.MAX_VALUE;
                        Rect yhRect = null; // 节点有多个，采用第一个
                        for (AccessibilityNodeInfo node : nodes) {
                            String text = node.getText() + "";
                            if ("用户".equals(text)) {
                                Rect rect = new Rect();
                                node.getBoundsInScreen(rect);
                                if (rect.left < minX) { // x1
                                    minX = rect.left;
                                    yhRect = rect;
                                }
                            }
                        }

                        minX = Integer.MAX_VALUE;
                        Rect ckgdRect = null; // 节点有多个，采用第一个
                        for (AccessibilityNodeInfo node : nodes) {
                            String text = node.getText() + "";
                            if ("查看更多".equals(text)) {
                                Rect rect = new Rect();
                                node.getBoundsInScreen(rect);
                                if (rect.left < minX) { // x1
                                    minX = rect.left;
                                    ckgdRect = rect;
                                }
                            }
                        }

                        if (yhRect != null) {
                            Log.e(TAG, "点击 用户" + yhRect);
                            //ThreadUtil.sleep(1000);
                            // x1:left y1:top x2:right y2:bottom
                            Tap((yhRect.left + yhRect.right) / 2, (yhRect.top + yhRect.bottom) / 2);
                        }

                        if (ckgdRect != null) {
                            Rect ckgdRect2 = ckgdRect;
                            ThreadUtil.async(() -> {
                                ThreadUtil.sleep(4000);

                                Log.e(TAG, "点击 查看更多" + ckgdRect2);
                                //ThreadUtil.sleep(1000);
                                // x1:left y1:top x2:right y2:bottom
                                Tap((ckgdRect2.left + ckgdRect2.right) / 2, (ckgdRect2.top + ckgdRect2.bottom) / 2);
                            });
                        }

                    } else if ("#@#find#webview2".equals(cmd)) { // 强制点击下交易，不然用户更多数据刷新不出来！
                        // 查找WebView中指定view节点并Tap点击：webview中节点经常刷新不出来
                        List<AccessibilityNodeInfo> nodes = findNodeInfos();
                        int minX = Integer.MAX_VALUE;
                        Rect tgRect = null; // 节点有多个，采用第一个
                        for (AccessibilityNodeInfo node : nodes) {
                            String text = node.getText() + "";
                            if ("交易".equals(text)) {
                                Rect rect = new Rect();
                                node.getBoundsInScreen(rect);
                                if (rect.left < minX) { // x1
                                    minX = rect.left;
                                    tgRect = rect;
                                }
                            }
                        }

                        if (tgRect != null) {
                            Log.e(TAG, "点击 交易" + tgRect);
                            //ThreadUtil.sleep(1000);
                            // x1:left y1:top x2:right y2:bottom
                            Tap((tgRect.left + tgRect.right) / 2, (tgRect.top + tgRect.bottom) / 2);
                        }

                    } else if (cmd.startsWith("#@#find#webview0get")) {
                        // 查找WebView中指定view节点并获取数据：webview中节点经常刷新不出来
                        String index = cmd.substring("#@#find#webview0get".length()); // 第几个管理员
                        List<String> titles = List.of(
                                "总订单金额(元)", "总订单数", "总下单人数", "总收入(元)",
                                "浏览人数", "浏览次数", "下单新用户", "退款金额(元)");

                        // 获取数据父节点
                        List<AccessibilityNodeInfo> nodes = findNodeInfos();
                        AccessibilityNodeInfo parentNode = null;
                        for (int i = 0; i < nodes.size(); i++) {
                            String text = nodes.get(i).getText() + "";
                            if (titles.get(0).equals(text)) {
                                parentNode = nodes.get(i).getParent();
                                break;
                            }
                        }
                        // 遍历数据兄弟节点
                        Map<String, Object> data = new LinkedHashMap<>();
                        if (parentNode != null) {
                            for (int i = 0; i < parentNode.getChildCount() - 1; i++) {
                                String currTitle = parentNode.getChild(i).getText() + "";
                                String nextValue = parentNode.getChild(i + 1).getText() + "";
                                if (titles.contains(currTitle)) {
                                    data.put(currTitle, nextValue);
                                    Log.d(TAG, currTitle + ": " + nextValue);
                                }
                            }
                        }

                        if (data.size() != titles.size()) {
                            Log.d(TAG, "概况数据失败");
                        } else {
                            // 保存，第index账号 订单数据
                            ThreadUtil.async(() -> {
                                String url = "https://t2.tuielf.com/api/ynkey/data";
                                String urlEncoded = "type=orderdata&no=" + index + "&orderdata=" + Codec.url_encode(Codec.json_encode(data));
                                Response resp = API.sendOriginal(true, url, null, urlEncoded.getBytes());
                                if (resp.isError()) {
                                    Log.e(TAG, Codec.json_encode(resp));
                                } else {
                                    Log.d(TAG, index + ":" + resp.getBody());
                                }
                            });
                        }

                    } else if (cmd.startsWith("#@#find#webview1get")) {
                        // 查找WebView中指定view节点并获取数据：webview中节点经常刷新不出来
                        String index = cmd.substring("#@#find#webview1get".length()); // 第几个管理员
                        List<String> titles = List.of(
                                "总下单人数", "浏览人数", "浏览次数", "主动分享团员人数", "团员人数", "新增团员人数", "累计订阅人数",
                                "新增订阅人数", "会员人数", "非会员人数", "新增会员人数", "会员升级人数", "回流下单用户", "累计沉默用户");

                        // 获取数据父节点
                        List<AccessibilityNodeInfo> nodes = findNodeInfos();
                        AccessibilityNodeInfo parentNode = null;
                        for (int i = 0; i < nodes.size(); i++) {
                            String text = nodes.get(i).getText() + "";
                            if (titles.get(0).equals(text)) {
                                parentNode = nodes.get(i).getParent();
                                break;
                            }
                        }
                        // 遍历数据兄弟节点
                        Map<String, Object> data = new LinkedHashMap<>();
                        if (parentNode != null) {
                            for (int i = 0; i < parentNode.getChildCount() - 1; i++) {
                                String currTitle = parentNode.getChild(i).getText() + "";
                                String nextValue = parentNode.getChild(i + 1).getText() + "";
                                if (titles.contains(currTitle)) {
                                    data.put(currTitle, nextValue);
                                    Log.d(TAG, currTitle + ": " + nextValue);
                                }
                            }
                        }

                        if (data.size() != titles.size()) {
                            Log.d(TAG, "用户数据失败");
                        } else {
                            // 保存，第index账号 用户数据
                            ThreadUtil.async(() -> {
                                String url = "https://t2.tuielf.com/api/ynkey/data";
                                String urlEncoded = "type=userdata&no=" + index + "&userdata=" + Codec.url_encode(Codec.json_encode(data));
                                Response resp = API.sendOriginal(true, url, null, urlEncoded.getBytes());
                                if (resp.isError()) {
                                    Log.e(TAG, Codec.json_encode(resp));
                                } else {
                                    Log.d(TAG, index + ":" + resp.getBody());
                                }
                            });
                        }
                    } else if ("#@#danxiangkehu#".equals(cmd)) { // 单向客户
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

                                    // 构建多短stroke手势：每个复选框一个300ms的tap，间隔700ms
                                    long strokeDuration = 300L;
                                    long strokeInterval = 700L;
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
                        ThreadUtil.sleep(1500);
                    } else if ("#@#huihua#".equals(cmd)) { // 删会话
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        if (rootNode == null) {
                            return;
                        }
                        // "TextView-标为未读[748,1873,1012,1938]com.tencent.wework:id/f6j" : null
                        // "TextView-删除[748,2449,1012,2514]com.tencent.wework:id/f6j" : null
                        AccessibilityNodeInfo del1Node = null; // 删除1
                        // "TextView-取消[595,1542,763,1655]com.tencent.wework:id/dm2" : null,
                        // "TextView-删除[823,1542,967,1655]com.tencent.wework:id/dm3" : null
                        AccessibilityNodeInfo del2Node = null; // 删除2
                        List<AccessibilityNodeInfo> delNodes = rootNode.findAccessibilityNodeInfosByText("删除");
                        List<AccessibilityNodeInfo> weiduNodes = rootNode.findAccessibilityNodeInfosByText("标为未读");
                        if (delNodes.size() > 0) {
                            if (weiduNodes.size() > 0) {
                                del1Node = delNodes.get(0);
                            } else {
                                for (AccessibilityNodeInfo delNode : delNodes) {
                                    if ("删除".equals(delNode.getText() + "")) {
                                        del2Node = delNode;
                                        break;
                                    }
                                }
                            }
                        }

                        if (del1Node != null) {
                            Rect del1Rect = new Rect();
                            del1Node.getBoundsInScreen(del1Rect);
                            //del1Node.performAction(AccessibilityNodeInfo.ACTION_CLICK); // isClickable false？
                            //ThreadUtil.sleep(300);
                            _Tap(del1Rect.centerX(), del1Rect.centerY(), 400L, null);
                            Log.e(TAG, "点击1 " + del1Node.getText());

                        } else if (del2Node != null) {
                            Log.e(TAG, "点击2 " + del2Node.getText());
                            del2Node.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                        } else { // 长按 x500 y640
                            LongTap(500, 640);
                            Log.e(TAG, "已长按");
                        }

                    } else if ("#@#全部清除#".equals(cmd)) { // 如果有“全部清除”就点击
                        debugRun();
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId("com.motorola.launcher3:id/action_clear_all");
                        for (AccessibilityNodeInfo node : nodes) {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            Log.e(TAG, "点击 " + node.getText());
                        }

                    } else if ("#@#nextpage#".equals(cmd)) { // 屏幕左滑翻页
                        _Swipe(980, 1200, 100, 1200, 500L, () -> {
                            Log.e(TAG, "屏幕左滑翻页");
                        });

                    } else if ("#@#滑屏调起任务#".equals(cmd)) { // 摩托罗拉
                        DisplayMetrics dm = WindowHelper.getRealMetrics();
                        float w = dm.widthPixels;
                        float h = dm.heightPixels;
                        Log.e(TAG, "分辨率[" + w + ", " + h + "]");

                        GestureDescription.Builder builder = new GestureDescription.Builder();
                        Path p = new Path();
                        p.moveTo(w / 3 , h - 10); // 底部，左三分之一
                        p.lineTo(w / 3 , h / 2); // 中部，左三分之一
                        p.lineTo(w / 3 * 2 , h / 2); // 中部，右三分之一
                        builder.addStroke(new GestureDescription.StrokeDescription(p, 0L, 500L));
                        GestureDescription gesture = builder.build();
                        dispatchGesture(gesture, new GestureResultCallback() {
                            @Override
                            public void onCompleted(GestureDescription gestureDescription) {
                                super.onCompleted(gestureDescription);
                                Log.e(TAG, "滑屏调起任务");
                            }
                        }, null);

                    } else if ("#@#上划解锁#".equals(cmd)) {
                        DisplayMetrics dm = WindowHelper.getRealMetrics();
                        float w = dm.widthPixels;
                        float h = dm.heightPixels;
                        Log.e(TAG, "分辨率[" + w + ", " + h + "]");

                        int x = (int) (w / 2);
                        int y1 = (int) (h * 0.75);
                        Swipe(x, y1, x, 10);

                    } else if (cmd.startsWith("#@#find#qiyeweixin")) { // 打开企业微信APP
                        String index = cmd.substring("#@#find#qiyeweixin".length()); // 第0-5个企业微信

                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        String appname = "0".equals(index) ? "企业微信" : "企业微信 " + index;
                        List<AccessibilityNodeInfo> qiyeweixinNodes = rootNode.findAccessibilityNodeInfosByText(appname);
                        for (int i = 0; i < qiyeweixinNodes.size(); i++) {
                            AccessibilityNodeInfo qiyeweixinNode = qiyeweixinNodes.get(i);
                            if (appname.equals(qiyeweixinNode.getText() + "")) {
                                qiyeweixinNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                Log.e(TAG, "点击 " + qiyeweixinNode.getText());

                                break;
                            }
                        }

                    } else if ("#@#gongzuotai#".equals(cmd)) { // 点击 工作台 按钮
                        String find = "工作台";
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> gongzuotaiNodes = rootNode.findAccessibilityNodeInfosByText(find);
                        for (AccessibilityNodeInfo gongzuotaiNode : gongzuotaiNodes) {
                            if (find.equals(gongzuotaiNode.getText() + "")) {
                                Rect gongzuotaiRect = new Rect();
                                gongzuotaiNode.getBoundsInScreen(gongzuotaiRect);
                                //gongzuotaiNode.performAction(AccessibilityNodeInfo.ACTION_CLICK); // isClickable false
                                MediumTap(gongzuotaiRect.centerX(), gongzuotaiRect.centerY());
                                Log.e(TAG, "点击 " + gongzuotaiNode.getText());
                                break;
                            }
                        }

                    } else if ("#@#daka#".equals(cmd)) { // 点击 打开 按钮，进入打开页面
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> dakaNodes = rootNode.findAccessibilityNodeInfosByText("打卡");
                        for (AccessibilityNodeInfo dakaNode : dakaNodes) {
                            Rect dakaRect = new Rect();
                            dakaNode.getBoundsInScreen(dakaRect);
                            //gongzuotaiNode.performAction(AccessibilityNodeInfo.ACTION_CLICK); // isClickable false
                            MediumTap(dakaRect.centerX(), dakaRect.centerY());
                            Log.e(TAG, "点击 " + dakaNode.getText());
                            break;
                        }

                    } else if ("#@#bandaka#".equals(cmd)) { // 点击 上班打卡、下班打卡
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> dakaNodes = rootNode.findAccessibilityNodeInfosByText("班打卡"); // 可打卡
                        for (AccessibilityNodeInfo dakaNode : dakaNodes) {
                            if ("上班打卡".equals(dakaNode.getText() + "") || "下班打卡".equals(dakaNode.getText() + "")) {
                                Rect dakaRect = new Rect();
                                dakaNode.getBoundsInScreen(dakaRect);
                                //gongzuotaiNode.performAction(AccessibilityNodeInfo.ACTION_CLICK); // isClickable false
                                MediumTap(dakaRect.centerX(), dakaRect.centerY());
                                Log.e(TAG, "点击 " + dakaNode.getText());
                                break;
                            }
                        }

                    } else if ("#@#querendaka#".equals(cmd)) { // 弹窗 确认打卡
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> querendakaNodes = rootNode.findAccessibilityNodeInfosByText("确认打卡"); // 可打卡
                        for (AccessibilityNodeInfo querendakaNode : querendakaNodes) {
                            querendakaNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            Log.e(TAG, "点击 " + querendakaNode.getText());
                            break;
                        }

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

                    } else if ("#@#工作台#".equals(cmd)) {
                        String find = "工作台";
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

                    } else if ("#@#客户朋友圈#".equals(cmd)) {
                        String find = "客户朋友圈";
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(find);
                        for (AccessibilityNodeInfo node : nodes) {
                            if (find.equals(node.getText() + "")) {
                                node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                Log.e(TAG, "点击 " + node.getText());
                                break;
                            }
                        }

                    } else if ("#@#去发表#".equals(cmd)) {
                        String find = "去发表";
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(find);
                        for (AccessibilityNodeInfo node : nodes) {
                            if (find.equals(node.getText() + "")) {
                                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                Log.e(TAG, "点击 " + node.getText());
                                break;
                            }
                        }

                    } else if ("#@#发表#".equals(cmd)) {
                        String find = "发表";
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(find);
                        for (AccessibilityNodeInfo node : nodes) {
                            if (find.equals(node.getText() + "")) {
                                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                Log.e(TAG, "点击 " + node.getText());
                                break;
                            }
                        }

                    } else if (cmd.startsWith("#@#点击输入框#")) {
                        // "ScrollView-[146,1621,952,1743]com.tencent.mm:id/o4q|false" : {
                        //   "EditText-[146,1615,952,1749]com.tencent.mm:id/bkk|true" : null
                        // },
                        String url = cmd.substring("#@#点击输入框#".length()); // 第几个管理员
                        List<AccessibilityNodeInfo> nodes = findNodeInfos();
                        for (AccessibilityNodeInfo scrollNode : nodes) {
                            if ((scrollNode.getClassName() + "").contains("ScrollView") && scrollNode.getChildCount() == 1) {
                                AccessibilityNodeInfo editNode = scrollNode.getChild(0);
                                if ((editNode.getClassName() + "").contains("EditText")) {
                                    //ScreenshotView.java：ClipboardUtil.setString(url);
                                    //editNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                                    //ThreadUtil.sleep(2000);
                                    //editNode.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                                    Bundle arguments = new Bundle();
                                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, url);
                                    editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                                    Log.e(TAG, "输入 " + url);
                                    break;
                                }
                            }
                        }

                    } else if ("#@#发送#".equals(cmd)) {
                        // "Button-发送[1013,2512,1196,2646]com.tencent.mm:id/bql|true" : null
                        String find = "发送";
                        List<AccessibilityNodeInfo> nodes = findNodeInfos();
                        for (AccessibilityNodeInfo node : nodes) {
                            if (find.equals(node.getText() + "") && (node.getClassName() + "").contains("Button")) {
                                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                Log.e(TAG, "点击 " + node.getText());
                                break;
                            }
                        }

                    } else if (cmd.startsWith("#@#查找链接#")) {
                        // "TextView-https://www.baidu.com[389,2330,1043,2460]com.tencent.mm:id/bkl|true" : null,
                        String url = cmd.substring("#@#查找链接#".length()); // 第几个管理员
                        List<AccessibilityNodeInfo> nodes = findNodeInfos();
                        for (AccessibilityNodeInfo node : nodes) {
                            if (url.equals(node.getText() + "") && (node.getClassName() + "").contains("TextView")) {
                                Rect rect = new Rect();
                                node.getBoundsInScreen(rect);
                                //node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                _Tap(rect.centerX(), rect.centerY(), 100L, null); // 25行 175行 200不行
                                Log.e(TAG, "点击 " + node.getText());
                                break;
                            }
                        }

                    } else if (cmd.startsWith("#@#H5关闭#")) {
                        boolean isWeb = true;
                        List<AccessibilityNodeInfo> nodes = findNodeInfos();
                        for (AccessibilityNodeInfo scrollNode : nodes) {
                            if ((scrollNode.getClassName() + "").contains("ScrollView") && scrollNode.getChildCount() == 1) {
                                AccessibilityNodeInfo editNode = scrollNode.getChild(0);
                                if ((editNode.getClassName() + "").contains("EditText")) {
                                    isWeb = false;
                                    Log.e(TAG, "上一步打开浏览器失败");
                                    break;
                                }
                            }
                        }
                        if (isWeb) {
                            String[] xy = cmd.substring("#@#H5关闭#".length()).split(",");
                            LongTap(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
                            Log.e(TAG, "点击H5关闭");
                        }

                    }





                }
            }
        } catch (Exception e) {
            Log.e(TAG, "无障碍回调异常");
            ExceptionUtil.getStackTrace(e);
            // 暂停脚本，跨进程无法执行，也无法直接alert
            //ScreenshotView.loopThread.interrupt();
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
    private void MediumTap(int x, int y) {
        _Tap(x, y, 400L, null);
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
     * 返回所有节点集合，用于WebView无法操作的情况
     * @return
     */
    private List<AccessibilityNodeInfo> findNodeInfos() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        List<AccessibilityNodeInfo> list = new ArrayList<>();
        _findNodeInfos(rootNode, list);
        return list;
    }

    private void _findNodeInfos(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> list) {
        if (node == null) {
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                list.add(child);
                _findNodeInfos(child, list);
            }
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

    /**
     * BFS查找列表容器：子节点最多的节点即为列表
     */
    private AccessibilityNodeInfo findListContainer(AccessibilityNodeInfo rootNode) {
        if (rootNode == null) return null;
        AccessibilityNodeInfo best = null;
        int maxChildren = 0;
        List<AccessibilityNodeInfo> queue = new ArrayList<>();
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            AccessibilityNodeInfo child = rootNode.getChild(i);
            if (child != null) queue.add(child);
        }
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.remove(0);
            if (node == null) continue;
            if (node.getChildCount() > maxChildren) {
                maxChildren = node.getChildCount();
                best = node;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        Log.d(TAG, "列表容器: " + (best == null ? "null" : best.getClassName()) + " 子节点数=" + maxChildren);
        return best;
    }
}