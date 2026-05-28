# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此代码仓库中工作时提供指导。

## 项目概述

基于百度地图 SDK 的 Android 演示应用（Java/Kotlin），展示骑行和步行导航功能。minSdk 21，targetSdk 36。SDK 以本地 AAR 形式提供，位于 `libs/BaiduLBS_Android.aar`，原生库位于 `libs/arm64-v8a/` 和 `libs/armeabi-v7a/`。

## 构建命令

```bash
# 调试包构建
./gradlew assembleDebug

# 构建并安装到已连接设备
./gradlew installDebug

# 清理
./gradlew clean
```

本项目没有单元测试。Lint 配置为 `abortOnError false`。

## 必要配置

运行前需在 `src/main/java/com/baidu/bikenavi_demo/BNaviDemoApplication.java` 中填入凭证：

- **API Key** — `SDKInitializer.setApiKey(...)` (第 34 行)：在[百度地图开放平台](https://lbsyun.baidu.com/apiconsole/key#/home)申请
- **TTS appKey / authSn** — `WNTTsInitConfig.Builder().appKey(...).authSn(...)` (第 50–51 行)：在 TTS 控制台申请
- **shareDeviceId** — `CommonInfo.Builder().shareDeviceId(...)` (第 32 行)：仅多实例和后台导航功能需要

## 架构说明

### 骑行 / 步行双栈并行

所有导航流程均有骑行（`com.baidu.mapapi.bikenavi.*`）和步行（`com.baidu.mapapi.walknavi.*`）两套对称实现。SDK 限制同一时间只能有一个引擎处于激活状态，因此 `BNaviMainActivity` 中每个入口在初始化目标引擎前，都会先调用另一个 Helper 的 `unInitNaviEngine()`。

每套导航栈遵循三步生命周期：
1. `*NavigateHelper.getInstance().initNaviEngine(...)` → `engineInitSuccess`
2. `routePlanWithRouteNode(param, listener)` → `onRoutePlanSuccess`
3. 启动导航界面（`BNaviGuideActivity` / `WNaviGuideActivity`）或调用 `startBkgNavi`

`CommonGuideListener` 持有静态单例的 `IBRouteGuidanceListener` / `IWRouteGuidanceListener`，在全局复用，将转向图标和道路文本更新推送给 `ComObservable`。

### 多实例地图

多实例地图需要 license 授权（`BWAuthLicenseType.AUTH_TYPE_MULTI_MAP`）。启动前 `licAuth()` 调用 `WalkNavigateHelper.getInstance().getAuthManager().loadAuth(...)` 并等待 `IBWAuthListener` 回调。授权成功后，通过 `MultiNaviViewProvider.getInstance().createMultiNaviView(context)` 创建每个额外的地图实例。`BikeNaviMultiActivity` 和 `WalkMultiActivity` 负责渲染这些实例。

二次进入多实例导航前，必须显式清理旧的引擎状态——依次调用 `quit()`、`unInitNaviEngine()`，并对 `MultiNaviViewProvider.getInstance().getAllMultiNaviView()` 中的每个条目调用 `onDestroy()`。`BNaviMainActivity` 中的 `forceResetBikeMultiNaviState()` 和 `forceResetWalkMultiNaviState()` 展示了完整的清理模式。

### 后台 / 离屏导航（`BackgroundNaviService`）

`BackgroundNaviService` 使用 `OffScreenMapNaviHelper` 完全在后台进行导航渲染。流程如下：

1. `onCreate` 中调用 `authAll()` — 等待多实例 license 授权
2. `initWalkEngine()` / `initBikeEngine()` — 若前台已在导航中则直接复用现有引擎
3. `startBkgNavi(context, naviMode, index, total)` 返回整数 `tag`，每个离屏实例对应一个 tag
4. `OffScreenMapNaviHelper.getInstance().createBackgroundDrawMapView(tag, width, height)` 创建离屏画面
5. `bkgDrawMapView.setScreenShotCallback(...)` 将渲染好的 `BitmapDrawable` 帧回调给调用方

`BNaviMainActivity.backgroundNavi()` 通过静态方法 `BackgroundNaviService.setScreenShotCallback(index, cb)` 注册各索引的回调，并将 bitmap 设置为 `View` 的背景。离屏地图的样式定制（路线颜色、车标图片等）通过 `BkgCustomDrawOptions.Builder` 完成，所有支持的属性见 `applyProjectionStyle()`。

### 悬浮窗（`FloatingWindowService`）

`FloatingWindowService` 是一个前台服务（Android O 及以上），使用 `SYSTEM_ALERT_WINDOW` 权限在其他应用上层叠加导航信息。它通过监听 `ComObservable` 接收 `CommonGuideListener` 推送的转向图标和道路文本更新。

### `MapView` 生命周期规则

每个 `MapView` 实例必须将宿主 Activity/Service 的 `onResume()`、`onPause()`、`onDestroy()` 转发给它。缺少任何一个会导致地图渲染异常或内存泄漏。