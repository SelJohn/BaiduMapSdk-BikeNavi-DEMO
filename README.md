# BaiduMapSdk-BikeNavi-DEMO

&zwnj;**基于百度地图步骑行SDK实现步骑行导航的DEMO**&zwnj;

## 📚 目录 

- [快速开始](#快速开始)
- [功能特性](#功能特性)

---  
## 快速开始  
申请AK
[百度地图开放平台申请Android AK](https://lbsyun.baidu.com/apiconsole/key#/home)

> 使用AK
<img width="1098" height="578" alt="image" src="https://github.com/user-attachments/assets/3fe90ca8-1d4f-44f7-8de1-8540856dd5dc" />
> 到这可以正常显示地图 可以开始导航了

## 功能特性  
配置TTS，实现导航播报
[百度地图开放平台申请``TTS](https://lbsyun.baidu.com/apiconsole/key/tts)

获取的 AK SN 填入DEMO
<img width="800" height="304" alt="image" src="https://github.com/user-attachments/assets/fbad79dd-b63f-4fae-85ed-83c07406a213" />


###  步行后台导航投屏功能文档

后台导航投屏功能允许应用在无界面或后台状态下进行步行导航，并将地图画面以截图方式呈现给用户。该功能基于百度地图SDK的多实例地图技术实现。

#### 参考BackgroundNaviService (后台导航服务)

https://github.com/user-attachments/assets/8c008afa-cb36-4b4e-bbf3-bd8fdec60547

实现流程

1. 初始化阶段

```java
// 步骤1：授权验证
authWalk()

// 步骤2：引擎初始化  
initWalkEngine()

// 步骤3：路由规划
routePlan()

// 步骤4：开始后台导航
startNavi()
```

2. 地图创建与配置

```java
// 创建后台绘制地图（800x500像素）
mBackgroundDrawMapView = OffScreenMapNaviHelper.getInstance()
    .createBackgroundDrawMapView(null, 800, 500);

// 添加导航图层
IBackgroundDrawLayer naviLayer = OffScreenMapNaviHelper.getInstance().getNaviLayer();
mBackgroundDrawMapView.addLayer(naviLayer);

// 配置绘制选项
BkgCustomDrawOptions.Builder builder = new BkgCustomDrawOptions.Builder();
builder.backgroundColor(0x00000000); // 透明背景
mBackgroundDrawMapView.setCustomDrawOption(builder.build());
```

3. 截图回调设置

```java
mBackgroundDrawMapView.setScreenShotCallback(new IBackgroundMapView.IScreenShotCallback() {
    @Override
    public void onScreenShot(BitmapDrawable bitmap) {
        // 处理截图数据，可以用于显示在ImageView或上传到服务器
        Log.i(TAG, "截图数据大小: " + bitmap.getBitmap().getByteCount());
    }
});
```

#### 地图显示设置

```java
// 设置地图范围（全览路线边距配合setNaviMapViewAllStatus使用）
mBackgroundDrawMapView.setNaviMapMargin(0, 50, 0, 50);

// 设置全览模式
mBackgroundDrawMapView.setNaviMapViewAllStatus(true);

// 设置默认缩放级别（4-22级）
mBackgroundDrawMapView.setDefaultLevel(19);

// 设置DPI缩放系数
mBackgroundDrawMapView.setMapDpiScale(1);

// 设置帧率
mBackgroundDrawMapView.setFps(5);

// 设置正北朝向true 路线朝上 false 默认路线朝上
mBackgroundDrawMapView.setNorthMode(true);

// 设置车辆图标偏移 水平，垂直方向 单位 px
mBackgroundDrawMapView.setCarOffset(0, 130);
```
#### 地图自定义
参考类BkgCustomDrawOptions
 * - 地图路线颜色
 * - 地图背景颜色
 * - 未走过导航路线颜色
 * - 已走过导航路线颜色
 * - 导航路线宽度
 * - 车标图片
 * - 起点图片
 * - 终点图片
#### 导航模式设置

```java
// 启动后台导航（真实导航模式）
boolean success = WalkNavigateHelper.getInstance().startBkgNavi(
    null, 
    context, 
    WalkNavigateHelper.NaviMode.RealNavi
);
```

#### 注意事项：

1. **权限检查**：启动前必须验证多实例地图权限
2. **内存管理**：服务销毁时需要释放地图资源





## 图像投屏文档pdf
[投屏接口文档.pdf](https://github.com/user-attachments/files/22022365/default.pdf)




