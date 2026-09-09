# 企微单删 (qwdelete)

基于 Android 无障碍服务 + MediaProjection 的企业微信客户批量单删工具，支持自动勾选、批量删除、定时任务等功能。

## 功能特性

### 核心功能
- **批量单删客户**：自动识别并勾选企业微信"单删客户"列表中的复选框，批量删除单向联系人
- **模板匹配检测**：通过截图 + 模板图像匹配精确定位复选框位置，不依赖控件树，适配 Android 15
- **每轮4个策略**：每轮只勾选前4个复选框进行删除，确保勾选成功率
- **定时任务**：支持设置定时执行批量删除任务，可添加多个时间点
- **悬浮窗截图**：通过 MediaProjection 实现实时截图辅助功能

### 界面主题
- **默认风格**：清新简洁绿色风格
- **手账风**：双击底部开发者名称切换为拼贴手账风格（马卡龙配色 + 贴纸卡片 + 胶带装饰）
- 主题状态通过 MMKV 持久化，下次启动自动恢复

### 技术特点
- 使用 `GestureDescription` + `dispatchGesture()` 实现可靠的系统级点击
- `isGesturing` 标志位防止手势执行期间被新命令打断
- `_TapSync` 同步等待手势完成（300ms持续时间，800ms间隔）
- 模板图片内嵌于 `res/drawable`，无外部文件依赖
- `android:testOnly="false"` + `adbOptions { installOptions '-t' }` 避免安装问题

## 环境要求

- Android Studio 2024.1.2+
- JDK 8+
- Android SDK 34（compileSdk 34, minSdk 24, targetSdk 34）
- 测试设备：Android 7.0+（推荐 Android 12-15）

## 快速开始

### 编译构建

```bash
# 克隆仓库
git clone https://github.com/leiyuanye/qw-delete.git
cd qw-delete

# 使用 Android Studio 打开项目
# 等待 Gradle Sync 完成
# 点击 Build > Build APKs 生成安装包
```

或使用命令行构建：

```bash
# Windows
gradlew.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

生成的 APK 位于 `AndroidDemo/build/outputs/apk/debug/`。

### 安装部署

```bash
# 通过 adb 安装（-t 允许测试包安装）
adb install -t AndroidDemo/build/outputs/apk/debug/AndroidDemo-debug.apk
```

### 使用步骤

1. **安装应用**后打开，首次启动会请求通知权限
2. **授权悬浮窗**：点击"弹窗"卡片中的"显示"按钮，授权悬浮窗权限
3. **开启无障碍服务**：点击"无障碍/已下载的服务"卡片中的"授权"按钮，在系统设置中找到"qwdelete模拟点击"并开启
4. **添加定时任务**（可选）：点击"批量单删"卡片中的"+"按钮添加定时任务行，设置执行时间和脚本类型
5. **启动定时任务**：点击"定时任务"卡片中的"启动"按钮
6. **手动执行**：打开企业微信 > 通讯录 > 客户 > 单删客户列表，应用会自动勾选前4个客户并执行删除

### 主题切换

双击首页底部"生活就是敲敲敲"标签，可在默认风格和手账风之间切换。

## 项目结构

```
qw-delete/
├── AndroidDemo/                    # 主模块
│   ├── src/main/
│   │   ├── AndroidManifest.xml     # 应用配置
│   │   ├── java/com/
│   │   │   ├── yaonan/             # 应用代码
│   │   │   │   ├── activity/       # MainActivity 等
│   │   │   │   ├── service/        # TimerService, MediaProjectionService
│   │   │   │   └── util/           # 工具类
│   │   │   └── google/android/     # 无障碍服务
│   │   │       └── accessibility/
│   │   │           └── selecttospeak/
│   │   │               └── SelectToSpeakService.java  # 核心删除逻辑
│   │   └── res/
│   │       ├── drawable/           # 背景、图标、模板图片
│   │       ├── layout/             # 布局文件
│   │       ├── values/             # 颜色、样式、字符串
│   │       └── xml/                # 无障碍服务配置
│   └── build.gradle                # 模块构建配置
├── build.gradle                    # 项目构建配置
├── gradle.properties               # Gradle 属性
├── testks-sign.gradle              # 签名配置
└── testks.jks                      # 测试签名密钥
```

## 关键实现说明

### 复选框检测（模板匹配）

项目使用截图 + 模板匹配方式检测复选框，而非依赖无障碍节点树（Android 15 节点结构不稳定）：

1. 通过 MediaProjection 获取当前屏幕截图
2. 与内嵌模板 `checkbox_unchecked.png`（60x60 像素）进行逐像素匹配
3. 像素差异小于 25 的区域判定为未勾选复选框
4. 50 像素去重，避免重复检测同一位置

### 手势执行

```java
// 同步等待手势完成，防止隔行点击
_TapSync(x, y, 300, 800);  // x, y, 持续300ms, 间隔800ms

// isGesturing 标志位防止手势执行期间新命令打断
if (isGesturing) return;  // 跳过新命令
```

### 删除流程

```
截屏 → 模板匹配定位复选框 → 勾选前4个 → 点击删除按钮 → 确认删除 → 等待页面刷新 → 下一轮
```

## 技术依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Material Components | (libs version) | Material Design UI |
| Jackson Databind | 2.22.2 | JSON 序列化 |
| MMKV | 2.3.0 | 轻量级键值存储 |

## 注意事项

- 每次 APK 更新后，需在系统设置中**手动重启无障碍服务**（关闭再开启）才能加载新代码
- 本工具仅用于管理企业微信客户列表，请遵守企业微信使用规范
- 签名密钥 `testks.jks` 为测试密钥，生产环境请替换为自己的签名

## License

MIT
