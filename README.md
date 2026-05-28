# BaiduMapSdk-BikeNavi-DEMO

基于百度地图步骑行 SDK 实现步骑行导航的 Android 演示项目。

---

## 目录

- [快速开始](#快速开始)
- [TTS 语音播报](#tts-语音播报)
- [后台导航投屏](#后台导航投屏)
- [注意事项](#注意事项)

---

## 快速开始

### 1. 申请 API Key

前往 [百度地图开放平台](https://lbsyun.baidu.com/apiconsole/key#/home) 申请 Android 平台的 AK。

### 2. 填入 AK

在 `BNaviDemoApplication.java` 中替换 API Key：

```java
SDKInitializer.setApiKey("your_api_key_here");
```

完成后即可正常显示地图并开始导航。

---

## TTS 语音播报

### 申请 TTS 凭证

前往 [百度地图开放平台 TTS 控制台](https://lbsyun.baidu.com/apiconsole/key/tts) 申请，获得 `appKey` 和 `authSN`。

### 填入凭证

在 `BNaviDemoApplication.java` 中填入：

```java
WNTTsInitConfig config = new WNTTsInitConfig.Builder()
        .context(getApplicationContext())
        .appKey("your_tts_app_key")
        .authSn("your_tts_auth_sn")
        .build();
WNTTSManager.getInstance().initTTS(config);
```

---

## 后台导航投屏

允许应用在无界面或后台状态下进行步行/骑行导航，并将地图画面以截图方式回调给调用方。基于百度地图 SDK 的多实例地图技术实现，参考 `BackgroundNaviService`。

### 初始化流程

```java
// 1. 授权验证（多实例 license）
authAll();

// 2. 初始化引擎
initWalkEngine(); // 或 initBikeEngine()

// 3. 路线规划
routePlan();

// 4. 启动后台导航
startBkgNavi();
```

### 创建离屏地图

```java
// 创建后台绘制地图（宽 800px，高 500px）
IBackgroundMapView bkgMapView = OffScreenMapNaviHelper.getInstance()
        .createBackgroundDrawMapView(tag, 800, 500);

// 添加导航图层
IBackgroundDrawLayer naviLayer = OffScreenMapNaviHelper.getInstance().getNaviLayer(tag);
bkgMapView.addLayer(naviLayer);
```

### 接收截图回调

```java
bkgMapView.setScreenShotCallback(new IBackgroundMapView.IScreenShotCallback() {
    @Override
    public void onScreenShot(BitmapDrawable bitmap) {
        // 将 bitmap 显示到 ImageView 或上传服务器
    }
});
```

### 地图显示配置

| 方法 | 说明 |
|---|---|
| `setNaviMapViewAllStatus(true)` | 全览路线模式 |
| `setNaviMapMargin(l, t, r, b)` | 设置全览边距（配合全览模式使用） |
| `setDefaultLevel(19)` | 默认缩放级别（4–22） |
| `setMapDpiScale(1)` | DPI 缩放系数 |
| `setFps(5)` | 渲染帧率 |
| `setNorthMode(true)` | `true` 正北朝上，`false` 路线朝上 |
| `setCarOffset(0, 130)` | 车标偏移量（px） |

### 地图样式自定义

通过 `BkgCustomDrawOptions.Builder` 配置，支持以下属性：

- 路线颜色 / 未走过路线颜色 / 已走过路线颜色
- 地图背景颜色
- 导航路线宽度
- 车标 / 起点 / 终点图片及缩放比例
- 路网路名颜色与字体大小
- 罗盘图片
- Logo 显示与缩放

---

## 注意事项

1. **多实例 license**：使用后台导航及多实例地图前，必须完成 `BWAuthLicenseType.AUTH_TYPE_MULTI_MAP` 授权验证。
2. **引擎互斥**：骑行引擎与步行引擎不能同时初始化，切换前需先调用 `unInitNaviEngine()`。
3. **二次进入清理**：重新启动多实例导航前，需依次调用 `quit()`、`unInitNaviEngine()` 并销毁旧的多实例视图，否则可能导致算路回调不触发。
4. **MapView 生命周期**：每个 `MapView` 必须在宿主 Activity 的 `onResume`、`onPause`、`onDestroy` 中同步调用对应方法。
5. **内存管理**：服务销毁时需调用 `bkgMapView.onDestroy()` 和 `OffScreenMapNaviHelper.getInstance().destoryAllBkgNavi()` 释放资源。

---

## 接口文档

- [投屏接口文档 PDF](https://github.com/user-attachments/files/22022365/default.pdf)