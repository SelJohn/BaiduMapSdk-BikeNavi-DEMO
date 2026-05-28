package com.baidu.bikenavi_demo;

import android.app.Service;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBEngineInitListener;
import com.baidu.mapapi.bikenavi.adapter.IBRoutePlanListener;
import com.baidu.mapapi.bikenavi.model.BikeRoutePlanError;
import com.baidu.mapapi.bikenavi.params.BikeNaviLaunchParam;
import com.baidu.mapapi.bikenavi.params.BikeRouteNodeInfo;
import com.baidu.mapapi.common.auth.BWAuthFuncResult;
import com.baidu.mapapi.common.auth.BWAuthLicenseType;
import com.baidu.mapapi.common.auth.BWAuthResult;
import com.baidu.mapapi.common.auth.IBWAuthListener;
import com.baidu.mapapi.offscreen.BkgCustomDrawOptions;
import com.baidu.mapapi.offscreen.OffScreenMapNaviHelper;
import com.baidu.mapapi.offscreen.IBackgroundMapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWEngineInitListener;
import com.baidu.mapapi.walknavi.adapter.IWRoutePlanListener;
import com.baidu.mapapi.walknavi.model.WalkRoutePlanError;
import com.baidu.mapapi.walknavi.params.WalkNaviLaunchParam;
import com.baidu.mapapi.walknavi.params.WalkRouteNodeInfo;
import com.baidu.platform.comapi.license.AuthorizeServiceType;
import com.baidu.platform.comapi.license.LicenseCode;
import com.baidu.mapapi.offscreen.IBackgroundDrawLayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 步行纯后台导航功能验证
 * Created by hexiaojiang
 * on 2025/11/24
 */
public class BackgroundNaviService extends Service {
    private static final String TAG = "BackgroundNaviService";
    private static volatile boolean sBikeNavi = false;
    private static final Map<Integer, IBackgroundMapView.IScreenShotCallback> SCREEN_SHOT_CALLBACK_MAP = new HashMap<>();
    private static final Map<Integer, BkgCustomDrawOptions> CUSTOM_DRAW_OPTIONS_MAP = new HashMap<>();
    private static BackgroundNaviService sServiceInstance;
    private final ArrayList<Integer>mBkgTagArray = new ArrayList();
    private final ArrayList<IBackgroundMapView> mBackgroundDrawMapViewArray = new ArrayList<>();
    private boolean mBkgStartedOrStarting = false;

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                Log.d(TAG, "handler start, sBikeNavi=" + sBikeNavi);
                if (mBkgStartedOrStarting) {
                    Log.d(TAG, "handler ignored, already started/starting");
                    return;
                }
                mBkgStartedOrStarting = true;
                if (sBikeNavi) {
                    initBikeEngine();
                } else {
                    initWalkEngine();
                }

            }
        }
    };

    private void initBikeEngine() {
        // 有权限了再去创建多实例地图 投屏地图
        Log.d(TAG, "initBikeEngine");
        // 若前台已经在骑行导航中，直接复用现有引擎启动后台投屏，避免二次init/算路导致NAVI_STATUS_ERROR
        try {
            if (BikeNavigateHelper.getInstance().isInitEngine()
                    && BikeNavigateHelper.getInstance().isNavigating()) {
                Log.d(TAG, "BikeNavi already navigating, reuse engine to start background projection");
                startBikeNavi();
                return;
            }
        } catch (Throwable t) {
            Log.w(TAG, "BikeNavi navigating check failed: " + t);
        }
        BikeNavigateHelper.getInstance().initNaviEngine(this.getApplicationContext(), new IBEngineInitListener() {
            @Override
            public void engineInitSuccess() {
                Log.d(TAG, "BikeNavi engineInitSuccess");
                // 设置导航常驻
                BikeNavigateHelper.getInstance().setIfNaviStanding(true);
                // 创建后台绘制多实例地图
                routePlan4BikeNavi();
            }

            @Override
            public void engineInitFail() {
                Log.d(TAG, "BikeNavi engineInitFail");
                BikeNavigateHelper.getInstance().unInitNaviEngine();
            }
        });
    }

    private void routePlan4BikeNavi() {
        Log.d(TAG, "routePlan4BikeNavi");
        // 对齐前台示例（BNaviMainActivity）使用的起终点，避免后台算路失败导致无投屏截图
        LatLng startPt = new LatLng(34.381347, 108.987089);
        LatLng endPt = new LatLng(34.229479, 108.970481);

        BikeRouteNodeInfo walkStartNode = new BikeRouteNodeInfo();
        walkStartNode.setLocation(startPt);
        BikeRouteNodeInfo walkEndNode = new BikeRouteNodeInfo();
        walkEndNode.setLocation(endPt);
        BikeNaviLaunchParam walkParam = new BikeNaviLaunchParam().startNodeInfo(walkStartNode).endNodeInfo(walkEndNode);

        BikeNavigateHelper.getInstance().routePlanWithRouteNode(walkParam, new IBRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d(TAG, "BikeNavi onRoutePlanStart");
            }
            @Override
            public void onRoutePlanSuccess() {
                Log.d(TAG, "onRoutePlanSuccess");
//                OffScreenMapNaviHelper.getInstance().setLocationDirectionFollowPhone(true);
                startBikeNavi();
            }

            @Override
            public void onRoutePlanFail(BikeRoutePlanError error) {
                Log.d(TAG, "BikeNavi onRoutePlanFail: " + error);
            }

        });
    }

    private void startBikeNavi() {
        Log.d(TAG, "startBikeNavi isFake=" + BNaviMainActivity.isFakeNavi);
        resetBackgroundProjectionIfNeeded();
        // 骑行后台导航，模拟导航
        if (!BNaviMainActivity.isFakeNavi) {
            startBkgBikeNaviMulti(1, BikeNavigateHelper.NaviMode.RealNavi);
        } else {
            // 开启3个多实例骑行导航
            startBkgBikeNaviMulti(1, BikeNavigateHelper.NaviMode.FakeNavi);
            BikeNavigateHelper.getInstance().setSimulateNaviSpeed(13);
        }

        if (mBkgTagArray.isEmpty()) {
            Toast.makeText(this, "骑行导航启动失败", Toast.LENGTH_SHORT).show();
        } else {
            createBackgroundDrawMapView();
        }
    }

    /**
     * 开启多个后台骑行导航
     * @param num 个数
     * @param naviMode 导航模式
     */
    private void startBkgBikeNaviMulti(int num, BikeNavigateHelper.NaviMode naviMode) {
        mBkgTagArray.clear();
        for (int i = 0; i < num; i++) {
            int tag = BikeNavigateHelper.getInstance().startBkgNavi(this, naviMode, i, num);
            if (tag > 0) {
                mBkgTagArray.add(tag);
            } else  {
                Log.d(TAG, "第" + (i + 1) + "个骑行导航启动失败");
            }
        }
        Log.d(TAG, "bike mBkgTagArray = " + mBkgTagArray);
    }

    /**
     * 开启多个后台步行导航
     * @param num 个数
     * @param naviMode 导航模式
     */
    private void startBkgWalkNaviMulti(int num, WalkNavigateHelper.NaviMode naviMode) {
        mBkgTagArray.clear();
        for (int i = 0; i < num; i++) {
            int tag = WalkNavigateHelper.getInstance().startBkgNavi(this, naviMode, i, num);
            if (tag > 0) {
                mBkgTagArray.add(tag);
            } else  {
                Log.d(TAG, "第" + (i + 1) + "个步行导航启动失败");
            }
        }
        Log.d(TAG, "walk mBkgTagArray = " + mBkgTagArray);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sServiceInstance = this;
        Log.d(TAG, "onCreate, sBikeNavi=" + sBikeNavi);
        authAll();
    }
    boolean aBoolean = true;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mBackgroundDrawMapViewArray.isEmpty()) {
            return super.onStartCommand(intent, flags, startId);
        }
        int type = intent.getIntExtra("type", 0);
        if (type == 1) {
            for (IBackgroundMapView mBackgroundDrawMapView : mBackgroundDrawMapViewArray) {
                // 全览
//            mBackgroundDrawMapView.setNaviMapMargin(0, 50, 0, 50);
                mBackgroundDrawMapView.setNaviMapViewAllStatus(aBoolean);
                // 默认层级 4~22
//            mBackgroundDrawMapView.setDefaultLevel(19);
                // 设置缩放系数
//            mBackgroundDrawMapView.setMapDpiScale(2);
                // 设置帧率
//            mBackgroundDrawMapView.setFps(5);
                // 设置正北朝上
//            mBackgroundDrawMapView.setNorthMode(aBoolean);
                // 设置车图标偏移量 px
//            mBackgroundDrawMapView.setCarOffset(0, 130);
            }
        }
        aBoolean = !aBoolean;
        return START_STICKY;
    }

    private void initWalkEngine() {
        // 有权限了再去创建多实例地图 投屏地图
        Log.d(TAG, "initWalkEngine");
        // 若前台已经在步行导航中，直接复用现有引擎启动后台投屏，避免二次init/算路导致状态错误
        try {
            if (WalkNavigateHelper.getInstance().isInitEngine()
                    && WalkNavigateHelper.getInstance().isNavigating()) {
                Log.d(TAG, "WalkNavi already navigating, reuse engine to start background projection");
                startNavi();
                return;
            }
        } catch (Throwable t) {
            Log.w(TAG, "WalkNavi navigating check failed: " + t);
        }
        WalkNavigateHelper.getInstance().initNaviEngine(this.getApplicationContext(), new IWEngineInitListener() {
            @Override
            public void engineInitSuccess() {
                Log.d(TAG, "WalkNavi engineInitSuccess");
                // 设置导航常驻
                WalkNavigateHelper.getInstance().setIfNaviStanding(true);
                // 创建后台绘制多实例地图
                routePlan();
            }

            @Override
            public void engineInitFail() {
                Log.d(TAG, "WalkNavi engineInitFail");
                WalkNavigateHelper.getInstance().unInitNaviEngine();
            }
        });
    }

    private void createBackgroundDrawMapView() {
        Log.d(TAG, "createBackgroundDrawMapView tags=" + mBkgTagArray);
        for (int i = 0; i < mBkgTagArray.size(); i ++) {
            int tag = mBkgTagArray.get(i);
            final int index = i;
            // 创建后台绘制地图
            IBackgroundMapView bkgDrawMapView = OffScreenMapNaviHelper.getInstance().createBackgroundDrawMapView(tag, 800, 500);
            // 获取并添加导航图层
            IBackgroundDrawLayer naviLayer = OffScreenMapNaviHelper.getInstance().getNaviLayer(tag);
            bkgDrawMapView.addLayer(naviLayer);
            // 默认自定义绘制选项（若主页面没有设置过）
            BkgCustomDrawOptions customDrawOptions = CUSTOM_DRAW_OPTIONS_MAP.get(index);
            if (customDrawOptions == null) {
                BkgCustomDrawOptions.Builder builder = new BkgCustomDrawOptions.Builder();
                if (i == 0) {
                    builder.backgroundColor(0x20FF0000);
                } else if (i == 1) {
                    builder.backgroundColor(0x2000FF00);
                } else if (i == 2) {
                    builder.backgroundColor(0x200000FF);
                }
                customDrawOptions = builder.build();
                CUSTOM_DRAW_OPTIONS_MAP.put(index, customDrawOptions);
            }
            bkgDrawMapView.setCustomDrawOption(customDrawOptions);
            // 设置擦除效果
            naviLayer.setEraseEffect(IBackgroundDrawLayer.EraseEffect.ALREADY_PASSED_CHANGE_COLOR);
            // 设置地图大小
            bkgDrawMapView.setScreenShotCallback(new IBackgroundMapView.IScreenShotCallback() {
                @Override
                public void onScreenShot(BitmapDrawable bitmap) {
                    int bytes = bitmap != null && bitmap.getBitmap() != null ? bitmap.getBitmap().getByteCount() : -1;
                    Log.i(TAG, "onScreenShot index=" + index + ", bytes=" + bytes);
                    IBackgroundMapView.IScreenShotCallback callback = SCREEN_SHOT_CALLBACK_MAP.get(index);
                    if (callback != null) {
                        callback.onScreenShot(bitmap);
                    } else {
                        Log.w(TAG, "onScreenShot no callback for index=" + index);
                    }
                }
            });
            bkgDrawMapView.openBackgroundMap();
            mBackgroundDrawMapViewArray.add(bkgDrawMapView);
        }
    }

    private void routePlan() {
        Log.d(TAG, "routePlan");
        LatLng startPt = new LatLng(40.056508, 116.307252);
        LatLng endPt = new LatLng(40.049742, 116.280516);
        WalkRouteNodeInfo walkStartNode = new WalkRouteNodeInfo();
        walkStartNode.setLocation(startPt);
        WalkRouteNodeInfo walkEndNode = new WalkRouteNodeInfo();
        walkEndNode.setLocation(endPt);
        WalkNaviLaunchParam walkParam = new WalkNaviLaunchParam().startNodeInfo(walkStartNode).endNodeInfo(walkEndNode);

        WalkNavigateHelper.getInstance().routePlanWithRouteNode(walkParam, new IWRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d(TAG, "WalkNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d(TAG, "onRoutePlanSuccess");
//                OffScreenMapNaviHelper.getInstance().setLocationDirectionFollowPhone(true);
                startNavi();
            }

            @Override
            public void onRoutePlanFail(WalkRoutePlanError error) {
                Log.d(TAG, "WalkNavi onRoutePlanFail");
            }

        });
    }

    private void startNavi() {
        Log.d(TAG, "startNavi isFake=" + BNaviMainActivity.isFakeNavi);
        resetBackgroundProjectionIfNeeded();
        // 步行后台导航，模拟导航
        if (!BNaviMainActivity.isFakeNavi) {
            startBkgWalkNaviMulti(1, WalkNavigateHelper.NaviMode.RealNavi);
        } else {
            startBkgWalkNaviMulti(1, WalkNavigateHelper.NaviMode.FakeNavi);
            WalkNavigateHelper.getInstance().setSimulateNaviSpeed(13);
        }

        if (mBkgTagArray.isEmpty()) {
            Toast.makeText(this, "步行导航启动失败", Toast.LENGTH_SHORT).show();
        } else {
            createBackgroundDrawMapView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sServiceInstance = null;
        mBkgStartedOrStarting = false;
        if (!mBackgroundDrawMapViewArray.isEmpty()) {
            for (IBackgroundMapView mBackgroundDrawMapView : mBackgroundDrawMapViewArray) {
                mBackgroundDrawMapView.setScreenShotCallback(null);
                mBackgroundDrawMapView.onDestroy();
//                mBackgroundDrawMapView = null;
            }
        }
        mBackgroundDrawMapViewArray.clear();
        SCREEN_SHOT_CALLBACK_MAP.clear();
        mBkgTagArray.clear();

        WalkNavigateHelper.getInstance().getAuthManager().removeAuthListener(authListener);
        OffScreenMapNaviHelper.getInstance().destoryAllBkgNavi();
        if (sBikeNavi) {
            BikeNavigateHelper.getInstance().quit();
        } else {
            WalkNavigateHelper.getInstance().quit();
        }
    }

    private void resetBackgroundProjectionIfNeeded() {
        // 若之前已经创建过后台离屏投屏实例，先销毁再重建，避免“多实例超过最大数量”
        if (!mBackgroundDrawMapViewArray.isEmpty() || !mBkgTagArray.isEmpty()) {
            Log.d(TAG, "resetBackgroundProjectionIfNeeded: destroy previous bkg navi, views="
                    + mBackgroundDrawMapViewArray.size() + ", tags=" + mBkgTagArray.size());
        }
        try {
            for (IBackgroundMapView v : mBackgroundDrawMapViewArray) {
                if (v != null) {
                    v.setScreenShotCallback(null);
                    v.onDestroy();
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "resetBackgroundProjectionIfNeeded onDestroy failed: " + t);
        }
        mBackgroundDrawMapViewArray.clear();
        mBkgTagArray.clear();
        // 清理 SDK 内部后台多实例占用
        OffScreenMapNaviHelper.getInstance().destoryAllBkgNavi();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final IBWAuthListener authListener = new IBWAuthListener() {
        @Override
        public void auth(BWAuthResult result) {
            Log.i(TAG, "BWAuthResult: " + result);
            for (BWAuthFuncResult bwAuthFuncResult :
                    result.getBWAuthFuncResults()) {
                if (bwAuthFuncResult.getFuncType() == AuthorizeServiceType.TYPE_AUTHORIZE_SERVICE_RIDING_NAVI_MULTI) {
                    if (bwAuthFuncResult.getActiveStatus() == LicenseCode.CODE_LICENSE_SERVICE_NO_ERROR) {
                        handler.sendEmptyMessage(0);
                        // 表示多实例有权限
                        Toast.makeText(getApplicationContext(), "多实例有权限", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "多实例无权限", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };
    /**
     * 步行license鉴权
     */
    private void authAll() {
        WalkNavigateHelper.getInstance().getAuthManager().addAuthListener(authListener);
        // 加载ak下多实例鉴权
        WalkNavigateHelper.getInstance().getAuthManager().loadAuth(getApplicationContext(), BWAuthLicenseType.AUTH_TYPE_MULTI_MAP, true);
    }

    public static void setScreenShotCallback(int index, IBackgroundMapView.IScreenShotCallback onScreenShot) {
        if (onScreenShot != null) {
            SCREEN_SHOT_CALLBACK_MAP.put(index, onScreenShot);
        }
    }

    public static void updateCustomDrawOption(int index, BkgCustomDrawOptions option) {
        if (index < 0 || option == null) {
            return;
        }
        CUSTOM_DRAW_OPTIONS_MAP.put(index, option);
        if (sServiceInstance != null) {
            sServiceInstance.applyCustomDrawOption(index, option);
        }
    }
    public static BkgCustomDrawOptions getCustomDrawOption(int index) {
       return CUSTOM_DRAW_OPTIONS_MAP.get(index);
    }

    private void applyCustomDrawOption(int index, BkgCustomDrawOptions option) {
        if (index < 0 || option == null) {
            return;
        }
        if (index < mBackgroundDrawMapViewArray.size()) {
            IBackgroundMapView mapView = mBackgroundDrawMapViewArray.get(index);
            if (mapView != null) {
                mapView.setCustomDrawOption(option);
            }
        }
    }

    public static void setUseBikeNavi(boolean useBikeNavi) {
        sBikeNavi = useBikeNavi;
        Log.d(TAG, "setUseBikeNavi: " + sBikeNavi);
    }
}
