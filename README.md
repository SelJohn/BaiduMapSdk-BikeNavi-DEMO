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

1、多实例setMapDpiScale(float mapScale, float logoScale)，接口增加第二个参数，表示设置多实例logo缩放比例。设置成0不显示logo。
2、骑行导航增加设置全览路线的margin接口。
需要注意的是，设置导航中全览margin要保证NaviHelper.getNaviMap()已经添加可以计算出来宽高。
示例
mNaviHelper.getNaviMap().post(new Runnable() {
            @Override
            public void run() {
                mNaviHelper.setRouteMargin(500,0,0,0); // 四个参数分别代表，距离NaviMapView左上右下像素值
                
            }
        });

3、BikeNavigateHelper增加 setNeedSensor（bool），用来调节车标heading，不设置sensor（设置为false） 车头默认会按照路线方向。需要在开始导航之前调用。
4、增加行中动态设置地图元素接口。
主实例

BikeNaviDisplayOption.BikeMapCustomDrawOption bikeNaviDisplayOption = new BikeNaviDisplayOption.BikeMapCustomDrawOption();
                bikeNaviDisplayOption.setCompassCustomBitmap(); // 设置罗盘资源
                bikeNaviDisplayOption.setEndPointCustomBitmap(); // 设置终点资源
                bikeNaviDisplayOption.setCarPointCustomRes(); // 设置车标资源
                bikeNaviDisplayOption.setRouteNormalCustomBitmap(); // 设置未走过路线资源
                bikeNaviDisplayOption.setRoutePassedCustomBitmap(); // 设置已走过路线资源
                bikeNaviDisplayOption.setRouteCustomWidth(); // 设置路线宽度
                bikeNaviDisplayOption.setCarPointCustomScale(); // 设置车标缩放比例
                bikeNaviDisplayOption.setEndPointCustomScale(); // 设置终点缩放比例
                bikeNaviDisplayOption.setCompassCustomScale(); // 设置罗盘缩放比例
BikeNavigateHelper.getInstance().updateMapRenderCustomDrawOption(bikeNaviDisplayOption);
多实例

MapCustomDrawOption mapCustomDrawOption = new MapCustomDrawOption();
                mapCustomDrawOption.setCompassCustomBitmap(); // 设置罗盘资源
                mapCustomDrawOption.setEndPointCustomBitmap(); // 设置终点资源
                mapCustomDrawOption.setCarPointCustomRes(); // 设置车标资源
                mapCustomDrawOption.setRouteNormalCustomBitmap(); // 设置未走过路线资源
                mapCustomDrawOption.setRoutePassedCustomBitmap(); // 设置已走过路线资源
                mapCustomDrawOption.setRouteCustomWidth(); // 设置路线宽度
                mapCustomDrawOption.setCarPointCustomScale(); // 设置车标缩放比例
                mapCustomDrawOption.setEndPointCustomScale(); // 设置终点缩放比例
                mapCustomDrawOption.setCompassCustomScale(); // 设置罗盘缩放比例
                multiNaviView.updateExtMapRenderCustomDrawOption(mapCustomDrawOption);
5、增加行中定位回调

IWRouteGuidanceListener
    /**
     * GPS状态发生变化，来自诱导引擎的消息
     *
     * @param gpsSignalLevel GPS信号强弱等级 0 弱 1强 暂时只有两个状态
     */
     void onGpsStatusChange(int gpsSignalLevel) {}
6、设置起终点红线显隐

BikeNavigateHelper增加接口getNaviSettingManager()
setVibrationOpen // 震动提醒开关
setRedlineOpen // 起终点红线显隐开关
setCrossMapLevelOpen // 路口自动放大开关

7、TTS playTTSText 接口中 第二个参数 int bPreempt，用来判断当前的tts播报是否需要抢占焦点。getTTSState 接口废弃。
        WNTTSManager.getInstance().initTTS(new WNTTSManager.IWNOuterTTSPlayerCallback() {
            @Override
            public int playTTSText(String speech, int bPreempt, int type) {
                // bPreempt = 1表示优先级较高 建议抢占焦点
                // bPreempt = 0表示优先级较低，建议播报完成上条tts
                return 0;
            }

            @Override
            @Deprecated
            public int getTTSState() {
                return 0;
            }
        });
8、新增控制导航中元素显隐接口
CAR_TO_END_RED_LINE(1), // 到终点红线
TRAFFIC_LIGHT(1 << 1), // 红绿灯标签
TRAFFIC_LIGHT_POP(1 << 2), // 红绿灯泡泡
ROAD_NAME_POP(1 << 3), // 路名泡泡
MILESTONE(1 << 4), // 里程碑标签
FACILITY(1 << 5), // 交通设施标签
FACILITY_POP(1 << 6); // 交通设施泡泡
8.1、多实例地图
* @param isShow 控制元素的显隐
* @param elements 要控制的元素列表
IMultiNaviView.setNaviDrawElementsShow
8.2、主地图
* @param isShow 控制元素的显隐
* @param elements 要控制的元素列表
BikeNavigateHelper.getInstance().setNaviDrawElementsShow
9、多实例截图接口增加第二个参数，bool类型，截屏是否带上地图logo。
10、多实例支持设置个性化地图。setMapCustomStylePath,传入sty文件的路径。
// 示例
// multi_style_test.sty 文件放置在 sdcard/Android/data/包名/files/  下面
multiNaviView.setMapCustomStylePath(getExternalFilesDir(null) + "/multi_style_test.sty");
11、如何自定义导航路线宽度、颜色？（骑行）
如何自定义起止点及车辆图标？主view和多实例view车辆图？
EngineOptions options = new EngineOptions.Builder()
                    .setRouteCustomWidth(30) // 路线宽度最大30
                    .setRouteNormalCustomBitmap() // 未走过路线纹理
                    .setRouteNormalCustomBitmapExt() // 多实例未走过路线纹理
                    .setRoutePassedCustomBitmap() // 已走过路线纹理
                    .setRoutePassedCustomBitmapExt() // 多实例已走过路线纹理
                    .setCarPointCustomRes() // 自车位置
                    .setCarPointCustomResExt() // 多实例自车位置
                    .setEndPointCustomBitmap() // 终点图片
                    .setEndPointCustomBitmapExt() // 多实例终点图片
                    .setExtUseMainRes() // 多实例地图是否使用主地图的资源，默认使用默认资源
                    .build();



骑行：
BikeNavigateHelper.getInstance().initNaviEngine(this, options, new IBEngineInitListener());
步行：
WalkNavigateHelper.getInstance().initNaviEngine(this, options, new IBEngineInitListener());
12、模拟导航。
骑行：
// 开始模拟导航
BikeNavigateHelper.getInstance().startBikeNavi(this, BikeNavigateHelper.NaviMode.FakeNavi);
//设置模拟导航速度
BikeNavigateHelper.getInstance().setSimulateNaviSpeed(5);
步行：ar导航不支持
// 开始模拟导航
WalkNavigateHelper.getInstance().startBikeNavi(this, WalkNavigateHelper.NaviMode.FakeNavi);
//设置模拟导航速度
WalkNavigateHelper.getInstance().setSimulateNaviSpeed(5);
13、多实例地图增加显示隐藏POI接口
    /**
     * 是否显示多实例地图的poi
     * @param isShow true 显示 默认显示
     */
    void showPoiMark(boolean isShow);
14、红绿灯数据透出
IBRouteGuidanceListener
/**
 * 红绿灯数据透出
 */
void onTrafficLightOutDataUpdate(TrafficLightOutData trafficLightOutData);
BikeNavigateHelper
    
    /**
     * 设置诱导监听, 获取诱导信息
     *
     * @param routeGuidanceListener 诱导监听事件
     */
    public void setRouteGuidanceListener(Activity activity, IBRouteGuidanceListener routeGuidanceListener) {
        if (null != routeGuidanceListener) {
            bikeNaviManager.setRouteGuidanceListener(activity, routeGuidanceListener);
        }
    }
    
   
public class TrafficLightOutData {
    /**
     * 0 刷新红绿灯气泡 (开始展示气泡或者灯态数据过期需要更新)/
     * 1 隐藏红绿灯气泡 /
     * 2 隐藏红绿灯icon
     */
    private int renderType;
    /**
     * 0 不带方向的灯 /
     * 1 直行灯 /
     * 2 左转灯
     */
    private int direction;
    /**
     * 0 无效值
     * 1 普通态
     * 2 大灯态
     * 3 小灯态
     */
    private int popType;
    /**
     * 0 无效值 /
     * 1 直行或左转单灯 /
     * 2 左转双灯第一个灯 /
     * 3 左转双灯第二个灯
     */
    private int lightType;
    public static class TrafficLightDataInfo {
        /**
         * 0 无效值
         * 11 灭灯
         * 21 红灯
         * 22 黄灯 (黄灯的气泡不显示倒计时，用文案“注意”代替)
         * 23 绿灯
         */
        private int status;
        /**
         * 使用时，每过一秒, period 减一 。
         * 当period <= 0时，取红绿灯态数组的下一份数据继续展示使用
         */
        private int period;
        /**
         * 只在0 < countDown <= 999 的情况下展示倒计时。
         * 如果返回的倒计时为10000或者 countDown 走完归零 period > 0的情况下，红绿灯气泡改为显示文案。
         * 具体为红灯显示“等待”，黄灯显示“注意”，绿灯显示“通行”。
         */
        private int countDown;
15、导航状态常驻
设置导航导航状态常驻，退出页面 导航正常进行
BikeNavigateHelper.getInstance().setIfNaviStanding(true);
增加获取是否常驻导航
BikeNavigateHelper.getInstance().isNaviStanding();
增加方法获取导航状态，导航中返回true
BikeNavigateHelper.getInstance().isNavigating()
开发者：
原有的导航页面的destroy方法改动
BikeNavigateHelper.quit();方法正式退出导航，释放引擎。
之后需要重新初始化可以参考DEMO实现
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mNaviHelper.isNaviStanding()) {
            mNaviHelper.quit();
        } else {
            mNaviHelper.onDestroy(false);
        }
    }

16、新增TTS播报开关
开启 关闭tts播报 默认开启
WNTTSManager.getInstance().enableTTS();


## Q&A
1、如何设置车辆图标在地图中的位置？
骑行：
    /**
     * 行中设置自车位置偏移
     *
     * @param x    X轴偏移量（像素值）
     * @param y    Y轴偏移量（像素值）
     * @param type 0 主图，1 多实例
     */
    BikeNavigateHelper.getInstance().setCarPosOffset();
步行：
    /**
     * 行中设置自车位置偏移
     *
     * @param x    X轴偏移量（像素值）
     * @param y    Y轴偏移量（像素值）
     * @param type 0 主图，1 多实例
     */
    WalkNavigateHelper.getInstance().setCarPosOffset();

2、如何设置导航时缩放级别？
骑行:
// level = [4,22]
BikeNavigateHelper.getInstance().setDefaultNaviMapScale();
步行：
// level = [4,22]
WalkNavigateHelper.getInstance().setDefaultNaviMapScale();

3、副地图显示地图 Logo？

MultiNaviViewProvider.IMultiNaviViewProxy multiNaviView = MultiNaviViewProvider
        .getInstance().createDefaultMultiNaviView(BNaviMainActivity.this);
multiNaviView.setPadding(10, 10, 10, 10);
// 设置logo位置不设置默认左下角
multiNaviView.setLogoPosition(LogoPosition.logoPostionleftBottom); 
    /**
     * 屏幕左下位置
     */
    logoPostionleftBottom,
    /**
     * 屏幕左上位置
     */
    logoPostionleftTop,
    /**
     * 屏幕中下位置
     */
    logoPostionCenterBottom,
    /**
     * 屏幕中上位置
     */
    logoPostionCenterTop,
    /**
     * 屏幕右下位置
     */
    logoPostionRightBottom,
    /**
     * 屏幕右上位置
     */
    logoPostionRightTop

4、如何隐藏地图背景只保留路线以及道路信息。
可以将这三个接口结合使用，参考demo
     multiNaviView.setMapCustomStylePath(getExternalFilesDir(null).getAbsolutePath() + "/map123.sty");
     multiNaviView.showPoiMark(false);
     multiNaviView.getMapTextureView().setTraffic(true);
5、设置了步骑行动态红绿灯不渲染咋回事？
需要再算路之前设置。
锁屏后红绿灯不渲染？
可能调用了摩托车驾车，BaiduNaviManagerFactory.getMapManager().onPause()接口。
还有设置打开后台渲染的
<img width="2686" height="510" alt="image" src="https://github.com/user-attachments/assets/88a03b5f-d411-40b1-9961-0ed0287e569c" />

