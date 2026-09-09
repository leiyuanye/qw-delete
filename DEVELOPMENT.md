# 开发文档 — android-qw-delete

> 企业微信单向客户清理工具，基于 Android 无障碍服务（AccessibilityService）

***

## 目录

1. [项目概览](#1-项目概览)
2. [环境要求](#2-环境要求)
3. [启动与构建](#3-启动与构建)
4. [权限配置](#4-权限配置)
5. [项目结构](#5-项目结构)
6. [功能模块与代码位置](#6-功能模块与代码位置)
7. [核心架构](#7-核心架构)
8. [命令系统参考](#8-命令系统参考)
9. [新增功能开发指南](#9-新增功能开发指南)

***

## 1. 项目概览

| 属性                     | 值                                                     |
| ---------------------- | ----------------------------------------------------- |
| 应用名称                   | qwdelete6.3                                           |
| 应用包名                   | com.yaonan.qwdelete                                   |
| 命名空间                   | com.yaonan                                            |
| 仓库地址                   | <http://ycgit.tuielf.cn/yaonan/android-qw-delete.git> |
| compileSdk / targetSdk | 34 (Android 14)                                       |
| minSdk                 | 24 (Android 7.0)                                      |
| AGP 版本                 | 8.3.2                                                 |
| Kotlin 版本              | 1.9.0                                                 |
| Java 版本                | 1.8                                                   |

### 核心依赖

| 依赖                                            | 版本     | 用途                    |
| --------------------------------------------- | ------ | --------------------- |
| `com.tencent:mmkv`                            | 2.3.0  | 跨进程键值存储，无障碍服务与应用主进程通信 |
| `com.fasterxml.jackson.core:jackson-databind` | 2.11.4 | JSON 序列化/反序列化，定时任务存储  |
| `com.google.android.material`                 | 1.10.0 | Material Design UI 组件 |

***

## 2. 环境要求

* **IDE**: Android Studio (推荐 2024.1.2+ / Koala)

* **JDK**: 17+ (AGP 8.x 要求)

* **Android SDK**: API 34 (Android 14)

* **Gradle**: 项目内置 wrapper，无需单独安装

* **测试设备**: Android 7.0+，已安装企业微信，已开启 USB 调试

* **签名**: 项目包含 `testks.jks` 签名文件，配置在 `testks-sign.gradle`

***

## 3. 启动与构建

### 3.1 克隆项目

```bash
git clone http://ycgit.tuielf.cn/yaonan/android-qw-delete.git
cd android-qw-delete
```

### 3.2 使用 Android Studio 打开

1. 启动 Android Studio → Open → 选择项目根目录
2. 等待 Gradle Sync 完成（首次会下载依赖，需联网）
3. 如遇 Gradle 下载慢，修改 `gradle/wrapper/gradle-wrapper.properties` 中的镜像地址

### 3.3 配置 SDK

* File → Project Structure → SDK Location

* 设置 Android SDK 路径，确保已安装 API 34

### 3.4 构建

* **Debug 构建**: Build → Build Bundle(s)/APK(s) → Build APK(s)

* **命令行构建**:

  ```bash
  # Windows
  gradlew.bat assembleDebug
  # 生成 APK 路径: AndroidDemo/build/outputs/apk/debug/AndroidDemo-debug.apk
  ```

### 3.5 运行到设备

1. 手机开启 USB 调试模式，连接电脑
2. 在 Device Manager 中确认设备已连接
3. 点击 Run（绿色三角）或 Shift+F10

### 3.6 生成 Release APK

```bash
gradlew.bat assembleRelease
# APK 路径: AndroidDemo/build/outputs/apk/release/AndroidDemo-release.apk
```

签名配置见 `testks-sign.gradle`：

```
签名文件: testks.jks (项目根目录)
```

***

## 4. 权限配置

### 4.1 AndroidManifest 声明的权限

| 权限                                    | 类型  | 用途                  |
| ------------------------------------- | --- | ------------------- |
| `SYSTEM_ALERT_WINDOW`                 | 运行时 | 悬浮窗（ScreenshotView） |
| `POST_NOTIFICATIONS`                  | 运行时 | 前台服务通知              |
| `FOREGROUND_SERVICE`                  | 普通  | 前台服务基础权限            |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | 普通  | 截屏服务                |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK`   | 普通  | 定时/短信服务             |
| `RECORD_AUDIO`                        | 运行时 | 录屏                  |
| `INTERNET`                            | 普通  | 网络通信                |
| `ACCESS_NETWORK_STATE`                | 普通  | 网络状态检测              |
| `WAKE_LOCK`                           | 普通  | 唤醒屏幕/保持CPU运行        |
| `DISABLE_KEYGUARD`                    | 普通  | 解锁屏幕                |
| `RECEIVE_SMS`                         | 运行时 | 接收短信                |
| `READ_SMS`                            | 运行时 | 读取短信                |
| `READ_PHONE_STATE`                    | 运行时 | 读取手机状态              |
| `READ_PHONE_NUMBERS`                  | 运行时 | 读取手机号               |

### 4.2 运行时需手动授权

| 权限    | 触发时机        | 代码位置                            |
| ----- | ----------- | ------------------------------- |
| 通知权限  | App 启动时检查   | `NotificationHelper.check()`    |
| 悬浮窗权限 | 点击「显示弹窗」时检查 | `WindowHelper.checkOverlay()`   |
| 无障碍服务 | 点击「授权」跳转设置  | `MainActivity` → `btn_start_a`  |
| 截屏授权  | 点击「启动」屏幕录制  | `MediaProjectionHelper.start()` |
| 短信权限  | 点击「启动」短信监控  | `SmsService.start()`            |

### 4.3 无障碍服务配置

配置文件: `AndroidDemo/src/main/res/xml/accessibility_config.xml`

```xml
android:accessibilityEventTypes="typeAllMask"      <!-- 监听所有事件 -->
android:accessibilityFeedbackType="feedbackGeneric"
android:accessibilityFlags="flagDefault|flagReportViewIds"
android:canRetrieveWindowContent="true"            <!-- 可读取窗口内容 -->
android:canPerformGestures="true"                  <!-- 可执行手势 -->
```

注册位置: `AndroidManifest.xml` 中的 `SelectToSpeakService`：

* 运行在独立进程 `:BackgroundService`

* 优先级 `10000`（最高）

* 绑定权限 `BIND_ACCESSIBILITY_SERVICE`

### 4.4 网络安全配置

配置文件: `AndroidDemo/src/main/res/xml/network_security_config.xml`

* 允许明文流量（`cleartextTrafficPermitted="true"`）

* 用于连接 HTTP 接口 `t2.tuielf.com`

***

## 5. 项目结构

```
android-qw-delete/
├── build.gradle                          # 根构建文件
├── settings.gradle                       # 项目设置（模块声明）
├── gradle.properties                     # Gradle 属性
├── testks-sign.gradle                    # 签名配置
├── testks.jks                            # 签名证书
├── gradle/
│   ├── libs.versions.toml                 # 版本目录（依赖版本管理）
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── README.md                             # 原始说明文档
└── AndroidDemo/                           # 主应用模块
    ├── build.gradle                       # 模块构建文件
    └── src/main/
        ├── AndroidManifest.xml            # 清单文件
        ├── java/com/yaonan/
        │   ├── App.java                   # Application 入口
        │   ├── activity/
        │   │   ├── MainActivity.java     # 主界面
        │   │   └── AboutActivity.java    # 关于页面
        │   ├── service/
        │   │   ├── AccessibilitySampleService.java  ★ 无障碍核心服务
        │   │   ├── TimerService.java                # 定时任务服务
        │   │   ├── SmsService.java                  # 短信监控服务
        │   │   └── MediaProjectionService.java     # 截屏服务
        │   ├── view/
        │   │   └── ScreenshotView.java              # 悬浮窗控制
        │   └── util/
        │       ├── WindowHelper.java                # 窗口管理
        │       ├── MediaProjectionHelper.java       # 截屏辅助
        │       ├── NotificationHelper.java          # 通知辅助
        │       ├── jna/
        │       │   ├── UI.java                      # 应用启动/唤醒/MMKV
        │       │   └── ClipboardUtil.java           # 剪贴板
        │       ├── global/Global.java               # 全局常量(TAG)
        │       ├── http/                            # HTTP 客户端
        │       │   ├── API.java
        │       │   ├── HTTPUtil.java
        │       │   ├── Response.java
        │       │   └── KeyValue.java
        │       ├── io/                              # 文件/图像工具
        │       │   ├── FileUtil.java
        │       │   └── ImageUtil.java
        │       ├── json/                            # JSON 解析器
        │       │   ├── JSON.java
        │       │   ├── Array.java
        │       │   ├── Element.java
        │       │   └── JsonParseException.java
        │       ├── lang/                            # 基础工具
        │       │   ├── MathUtil.java
        │       │   ├── MyColor.java
        │       │   ├── StringUtil.java
        │       │   ├── ThreadUtil.java
        │       │   └── TimeUtil.java
        │       ├── codec/                           # 编码工具
        │       │   ├── CharsetUtil.java
        │       │   └── Codec.java
        │       ├── exception/                       # 异常定义
        │       │   ├── MyException.java
        │       │   └── MyRuntimeException.java
        │       └── function/                        # 函数式接口
        │           ├── ConsumerE.java
        │           ├── SupplierE.java
        │           ├── FunctionE.java
        │           ├── BiConsumerE.java
        │           ├── BiFunctionE.java
        │           ├── RunnableE.java
        │           └── Tuple2~5.java
        └── res/
            ├── xml/
            │   ├── accessibility_config.xml         # 无障碍配置
            │   └── network_security_config.xml       # 网络安全配置
            ├── layout/
            │   ├── activity_main.xml                # 主界面布局
            │   ├── activity_about.xml
            │   ├── layout_screenshot_view.xml        # 悬浮窗布局
            │   ├── layout_projection_view.xml
            │   ├── layout_toast_view.xml
            │   └── layout_video_record_view.xml
            ├── values/
            │   ├── strings.xml                      # 字符串资源
            │   ├── colors.xml
            │   ├── dimens.xml
            │   ├── styles.xml
            │   └── themes.xml
            └── raw/
                ├── x.bmp                             # 模板图片
                ├── yjbm.bmp
                └── zdsp.bmp
```

***

## 6. 功能模块与代码位置

### 6.1 单删检测与清理（核心功能）

| 功能点         | 文件                                          | 关键代码位置                                              |
| ----------- | ------------------------------------------- | --------------------------------------------------- |
| 单删删除逻辑      | `AccessibilitySampleService.java`           | `onAccessibilityEvent()` 中 `#@#danxiangkehu#` 分支    |
| 单删触发入口（手动）  | `ScreenshotView.java`                       | 点击事件中 `type == 1` 分支，循环调用 `cmd("#@#danxiangkehu#")` |
| 批量单删流程（定时）  | `TimerService.java`                         | `type == 2` 分支，6个企微实例遍历                             |
| 批量单删流程（手动）  | `ScreenshotView.java`                       | `type == 2` 分支                                      |
| 企微导航：通讯录    | `AccessibilitySampleService.java`           | `#@#通讯录#` 命令                                        |
| 企微导航：我的客户   | `AccessibilitySampleService.java`           | `#@#我的客户#` 命令                                       |
| 企微导航：全部微信客户 | `AccessibilitySampleService.java`           | `#@#全部微信客户#` 命令                                     |
| 企微导航：单向微信客户 | `AccessibilitySampleService.java`           | `#@#单向微信客户#` 命令                                     |
| 企微导航：编辑     | `AccessibilitySampleService.java`           | `#@#编辑#` 命令                                         |
| 客户数量检查      | `AccessibilitySampleService.java`           | `#@#共n个客户#` 命令，返回 break/continue                    |
| 打开企微多开实例    | `AccessibilitySampleService.java`           | `#@#打开企业微信` + index(0-5) 命令                         |
| 删会话         | `AccessibilitySampleService.java`           | `#@#huihua#` 命令                                     |
| 清除最近任务      | `TimerService.java` / `ScreenshotView.java` | `qingChuRenWu()` 方法                                 |

### 6.2 朋友圈发布

| 功能点       | 文件                                | 关键代码位置         |
| --------- | --------------------------------- | -------------- |
| 朋友圈流程（定时） | `TimerService.java`               | `type == 3` 分支 |
| 朋友圈流程（手动） | `ScreenshotView.java`             | `type == 3` 分支 |
| 导航：工作台    | `AccessibilitySampleService.java` | `#@#工作台#` 命令   |
| 导航：客户朋友圈  | `AccessibilitySampleService.java` | `#@#客户朋友圈#` 命令 |
| 导航：去发表    | `AccessibilitySampleService.java` | `#@#去发表#` 命令   |
| 导航：发表     | `AccessibilitySampleService.java` | `#@#发表#` 命令    |

### 6.3 H5 监控

| 功能点      | 文件                            | 关键代码位置                       |
| -------- | ----------------------------- | ---------------------------- |
| H5 监控主循环 | `ScreenshotView.java`         | 点击事件中 `h5` 标志分支              |
| 网页颜色检测   | `MediaProjectionService.java` | `getColorAndScreenshot()` 方法 |
| 模板图匹配    | `MediaProjectionService.java` | `getScreenMatchImg()` 方法     |
| 图像模板匹配算法 | `ImageUtil.java`              | `cvMatchTemplate()` 方法       |
| 数据上报     | `ScreenshotView.java`         | HTTP 请求到 `t2.tuielf.com`     |

### 6.4 短信监控

| 功能点   | 文件                | 关键代码位置                                  |
| ----- | ----------------- | --------------------------------------- |
| 短信轮询  | `SmsService.java` | `onCreate()` 中循环，10秒间隔                  |
| 短信读取  | `SmsService.java` | 查询 `content://sms/inbox`                |
| 短信上报  | `SmsService.java` | HTTP POST 到 `t2.tuielf.com/api/sms/log` |
| 启动/停止 | `SmsService.java` | `start()` / `stop()` 静态方法               |

### 6.5 定时任务

| 功能点    | 文件                  | 关键代码位置                           |
| ------ | ------------------- | -------------------------------- |
| 定时主循环  | `TimerService.java` | `onCreate()` 中 20秒轮询             |
| 任务存储   | `MainActivity.java` | `saveTasks()` → MMKV `"tasks"` 键 |
| 任务行 UI | `MainActivity.java` | `addRow()` / `delRow()` 方法       |
| 时间选择器  | `MainActivity.java` | `onTimeClick` → TimePickerDialog |
| 任务类型选择 | `MainActivity.java` | Spinner: 批量单删/朋友圈/推送             |
| 启动/停止  | `TimerService.java` | `start()` / `stop()` 静态方法        |

### 6.6 屏幕截图与投屏

| 功能点    | 文件                            | 关键代码位置                                   |
| ------ | ----------------------------- | ---------------------------------------- |
| 截屏服务   | `MediaProjectionService.java` | `screenshot()` 方法                        |
| 像素颜色获取 | `MediaProjectionService.java` | `getColor()` / `getColorAndScreenshot()` |
| 截屏权限请求 | `MediaProjectionHelper.java`  | `start()` / `onStartResult()`            |
| 悬浮窗投屏  | `ScreenshotView.java`         | 浮动窗口显示截图                                 |
| 屏幕尺寸   | `WindowHelper.java`           | `getRealMetrics()`                       |

### 6.7 基础设施

| 功能点      | 文件                           | 关键代码位置                                          |
| -------- | ---------------------------- | ----------------------------------------------- |
| MMKV 初始化 | `App.java`                   | `onCreate()` → `MMKV.initialize()`              |
| 跨进程数据共享  | `App.java`                   | `getData()` / `setData()`                       |
| 悬浮窗管理    | `WindowHelper.java`          | `showScreenshotView()` / `hideScreenshotView()` |
| 应用启动     | `UI.java` (jna)              | `launchApp(String packageName)`                 |
| 唤醒解锁     | `UI.java` (jna)              | `wakeup()` / `wakeup(Activity)`                 |
| MMKV 获取  | `UI.java` (jna)              | `getMMKV()` — MULTI\_PROCESS\_MODE              |
| 剪贴板      | `ClipboardUtil.java`         | 粘贴文本到输入框                                        |
| HTTP 请求  | `API.java` / `HTTPUtil.java` | 封装的 HTTP 客户端                                    |
| 异步线程     | `ThreadUtil.java`            | `async()` 方法                                    |
| 通知辅助     | `NotificationHelper.java`    | 前台服务通知 + 权限检查                                   |

***

## 7. 核心架构

### 7.1 进程模型

```
┌─────────────────────────────────────────────────┐
│  主进程 (com.yaonan.qwdelete)                    │
│                                                  │
│  App.java          ← MMKV 初始化                  │
│  MainActivity      ← UI 控制面板                   │
│  TimerService      ← 定时前台服务                   │
│  SmsService        ← 短信监控前台服务               │
│  MediaProjectionService ← 截屏前台服务             │
│  ScreenshotView    ← 悬浮窗                       │
│                                                  │
│  通信方式:                                         │
│  1. App._data (HashMap) — 进程内共享               │
│  2. MMKV (MULTI_PROCESS_MODE) — 跨进程通信        │
│  3. announceForAccessibility() — 发送无障碍事件     │
└─────────────────────────────────────────────────┘
          │
          │ MMKV + AccessibilityEvent
          │
┌─────────▼───────────────────────────────────────┐
│  无障碍进程 (:BackgroundService)                  │
│                                                  │
│  SelectToSpeakService (AccessibilitySampleService)│
│                                                  │
│  - onAccessibilityEvent() 接收并解析命令           │
│  - 查找 UI 节点 (findAccessibilityNodeInfosByText)│
│  - 执行手势 (dispatchGesture)                     │
│  - 写响应到 MMKV (response方法)                   │
└─────────────────────────────────────────────────┘
```

### 7.2 命令通信机制

**异步命令** (`cmd`)：

```
ScreenshotView/TimerService
    │
    │ announceForAccessibility("#@#命令")
    │
    ▼
AccessibilitySampleService.onAccessibilityEvent()
    │
    │ 解析 "#@#" 前缀，匹配命令
    │ 执行操作（查找节点/手势点击等）
    └──→ 完成（无响应）
```

**同步命令** (`cmdWait`)：

```
ScreenshotView/TimerService
    │
    │ 1. 生成 UUID 作为 msgid
    │ 2. MMKV.putString(msgid, "")
    │ 3. announceForAccessibility("#@#命令" + msgid)
    │ 4. 轮询 MMKV.getString(msgid) 每1秒
    │
    ▼
AccessibilitySampleService.onAccessibilityEvent()
    │
    │ 1. 解析命令 + msgid
    │ 2. 执行操作
    │ 3. MMKV.putString(msgid, "break" / "continue" / "error")
    │
    ▼
ScreenshotView/TimerService
    │
    │ 5. 读取到响应，返回结果
    └──→ 继续/退出循环
```

### 7.3 手势操作机制

项目使用 `GestureDescription` + `dispatchGesture()` 替代 `performAction(ACTION_CLICK)`：

```java
// AccessibilitySampleService.java 中的手势方法
_Tap(x, y, time, callback)      // 精确点击，使用 GestureDescription
Tap(x, y)                        // 普通点击
MediumTap(x, y)                   // 中等时长点击
LongTap(x, y)                     // 长按
_Swipe(x1, y1, x2, y2, time, callback)  // 精确滑动
Swipe(x1, y1, x2, y2)            // 普通滑动
```

原因：企业微信许多节点的 `isClickable == false`，`performAction(ACTION_CLICK)` 无效，必须通过坐标手势模拟真实触摸。

### 7.4 数据存储

| 存储                   | 用途               | 位置                                      |
| -------------------- | ---------------- | --------------------------------------- |
| MMKV `"tasks"`       | 定时任务列表 (JSON)    | `MainActivity.saveTasks()`              |
| MMKV `"type"`        | 当前操作类型 (1/2/3/4) | `App.setData("type", n)`                |
| MMKV msgid           | 同步命令响应           | `AccessibilitySampleService.response()` |
| MMKV `"h5"`          | H5 监控 URL 列表     | `ScreenshotView`                        |
| App.\_data (HashMap) | 进程内共享数据          | `App.getData()/setData()`               |

***

## 8. 命令系统参考

无障碍服务通过 `onAccessibilityEvent()` 接收 `TYPE_ANNOUNCEMENT` 事件，解析 `#@#` 前缀命令：

### 企微导航命令

| 命令                 | 说明          | 同步 |
| ------------------ | ----------- | -- |
| `#@#打开企业微信` + 0\~5 | 打开第N个企微多开实例 | 否  |
| `#@#通讯录#`          | 点击「通讯录」tab  | 是  |
| `#@#我的客户#`         | 进入「我的客户」    | 否  |
| `#@#全部微信客户#`       | 进入「全部微信客户」  | 是  |
| `#@#单向微信客户#`       | 进入「单向微信客户」  | 否  |
| `#@#编辑#`           | 点击「编辑」按钮    | 否  |
| `#@#共n个客户#`        | 检查客户数量      | 是  |
| `#@#工作台#`          | 点击「工作台」tab  | 是  |
| `#@#客户朋友圈#`        | 进入「客户朋友圈」   | 否  |
| `#@#去发表#`          | 点击「去发表」     | 否  |
| `#@#发表#`           | 点击「发表」按钮    | 否  |

### 操作命令

| 命令                      | 说明                |
| ----------------------- | ----------------- |
| `#@#danxiangkehu#`      | 单删核心：查找并删除单向客户    |
| `#@#huihua#`            | 删除会话              |
| `#@#全部清除#`              | 清除最近任务（Motorola）  |
| `#@#tap#x,y`            | 点击坐标              |
| `#@#longtap#x,y`        | 长按坐标              |
| `#@#swipe#L`            | 长滑动（下拉）           |
| `#@#swipe#S`            | 短滑动               |
| `#@#nextpage#`          | 左滑翻页              |
| `#@#滑屏调起任务#`            | 滑屏调起（Motorola 专用） |
| `#@#上划解锁#`              | 上划解锁屏幕            |
| `#@#action#home`        | 按下 Home 键         |
| `#@#action#back`        | 按下返回键             |
| `#@#action#recents`     | 打开最近任务            |
| `#@#action#lock_screen` | 锁屏                |
| `#@#action#paste`       | 粘贴文本到输入框          |
| `#@#debug#`             | 输出完整节点树到日志        |

### 快团团/电商命令

| 命令                   | 说明                    |
| -------------------- | --------------------- |
| `#@#find#yjbm`       | 查找「一键帮卖」并点击           |
| `#@#find#qhsf`       | 查找身份切换                |
| `#@#find#sf`         | 查找身份选择                |
| `#@#find#ckgd`       | 查找「查看更多」              |
| `#@#find#webview0~2` | WebView 页面操作（日/用户/交易） |

***

## 9. 新增功能开发指南

### 9.1 添加新的无障碍命令

1. 在 `AccessibilitySampleService.java` 的 `onAccessibilityEvent()` 中添加命令分支：

```java
String cmd = event.getText().toString();
if (cmd.startsWith("#@#myCommand")) {
    // 解析参数
    String param = cmd.substring("#@#myCommand".length()).trim();
    // 查找节点
    List<AccessibilityNodeInfo> nodes = 
        rootNode.findAccessibilityNodeInfosByText("目标文本");
    if (nodes != null && nodes.size() > 0) {
        // 执行操作
        _Tap(bounds.centerX(), bounds.centerY());
    }
    // 如果是同步命令，写响应
    response(msgid, "success");
}
```

1. 在 `ScreenshotView.java` 或 `TimerService.java` 中调用：

```java
// 异步调用
cmd("#@#myCommand");
// 同步调用
String result = cmdWait("#@#myCommand", 20);
```

### 9.2 添加新的定时任务类型

1. 在 `MainActivity.java` 的 Spinner 选项中添加新类型
2. 在 `TimerService.java` 的 `onCreate()` 循环中添加 `type == N` 分支
3. 在 `ScreenshotView.java` 的点击事件中添加对应分支

### 9.3 添加新的 UI 页面

```java
// 1. activity 目录右键 → New → Activity → Empty Activity
// 2. 修改 layout XML（可参考 activity_main.xml）
// 3. 修改 Activity 代码
// 4. AndroidManifest.xml 中确认 activity 标签配置
```

### 9.4 添加新的前台服务

```java
// 1. 创建 Service 类
public class MyService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        // 创建通知
        Notification notification = ...;
        // 启动前台服务
        startForeground(NOTIFICATION_ID, notification);
        // 启动子线程执行逻辑
        ThreadUtil.async(() -> {
            // 业务逻辑
        });
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 清理资源
    }
}

// 2. AndroidManifest.xml 添加
<service android:name=".service.MyService"
         android:foregroundServiceType="mediaPlayback" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

// 3. 启动/停止
startForegroundService(new Intent(context, MyService.class));
stopService(new Intent(context, MyService.class));
```

### 9.5 版本更新流程

1. 修改 `strings.xml` 中的 `app_name` 后缀版本号
2. 构建 Release APK
3. 重命名为 `ynkey.apk`
4. 上传到服务器
5. 在 App 内点击更新按钮安装

### 9.6 换图标

1. 修改 `res/drawable/ic_launcher_background.xml` 颜色
2. res 右键 → New → Image Asset
3. 选择 `ic_launcher_background.xml` 和 `ic_launcher_foreground.xml`

