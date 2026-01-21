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


/**
 * 步行纯后台导航功能验证
 * Created by hexiaojiang
 * on 2025/11/24
 */
public class BackgroundNaviService extends Service {
    private static final String TAG = "BackgroundNaviService";
    private static IBackgroundMapView.IScreenShotCallback mScreenShotCallback;
    private static final boolean BIKE_NAVI = false;
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                if (BIKE_NAVI) {
                    initBikeEngine();
                } else {
                    initWalkEngine();
                }

            }
        }
    };

    private void initBikeEngine() {
        // 有权限了再去创建多实例地图 投屏地图
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
        LatLng startPt = new LatLng(40.056508, 116.307252);
        LatLng endPt = new LatLng(40.049742, 116.280516);

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
                Log.d(TAG, "BikeNavi onRoutePlanFail");
            }

        });
    }

    private void startBikeNavi() {
        // 步行后台导航，模拟导航
        boolean b ;
        if (!BNaviMainActivity.isFakeNavi) {
            b = BikeNavigateHelper.getInstance().startBkgNavi(null, this, BikeNavigateHelper.NaviMode.RealNavi);
        } else {
            b = BikeNavigateHelper.getInstance().startBkgNavi(null, this, BikeNavigateHelper.NaviMode.FakeNavi);
            BikeNavigateHelper.getInstance().setSimulateNaviSpeed(3);
        }

        if (!b) {
            Toast.makeText(this, "骑行导航启动失败", Toast.LENGTH_SHORT).show();
        } else {
            createBackgroundDrawMapView();
        }
    }

    private IBackgroundMapView mBackgroundDrawMapView;

    @Override
    public void onCreate() {
        super.onCreate();
        authWalk();
    }
    boolean aBoolean = true;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mBackgroundDrawMapView == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        int type = intent.getIntExtra("type", 0);
        if (type == 1) {
            // 全览
//            mBackgroundDrawMapView.setNaviMapMargin(0, 50, 0, 50);
//            mBackgroundDrawMapView.setNaviMapViewAllStatus(aBoolean);
            // 默认层级 4~22
//            mBackgroundDrawMapView.setDefaultLevel(19);
            // 设置缩放系数
//            mBackgroundDrawMapView.setMapDpiScale(2);
            // 设置帧率
//            mBackgroundDrawMapView.setFps(5);
            // 设置正北朝上
//            mBackgroundDrawMapView.setNorthMode(aBoolean);
            // 设置车图标偏移量 px
            mBackgroundDrawMapView.setCarOffset(0, 130);
        }
        aBoolean = !aBoolean;
        return super.onStartCommand(intent, flags, startId);
    }

    private void initWalkEngine() {
        // 有权限了再去创建多实例地图 投屏地图
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
        // 创建后台绘制地图
        mBackgroundDrawMapView = OffScreenMapNaviHelper.getInstance().createBackgroundDrawMapView(null, 800, 500);
        // 获取并添加导航图层
        IBackgroundDrawLayer naviLayer = OffScreenMapNaviHelper.getInstance().getNaviLayer();
        mBackgroundDrawMapView.addLayer(naviLayer);
        // 自定义绘制选项
        BkgCustomDrawOptions.Builder builder = new BkgCustomDrawOptions.Builder();
        // argb格式颜色值
        builder.backgroundColor(0x00000000);
        mBackgroundDrawMapView.setCustomDrawOption(builder.build());
        // 设置擦除效果
        naviLayer.setEraseEffect(IBackgroundDrawLayer.EraseEffect.ALREADY_PASSED_CHANGE_COLOR);
        // 设置地图大小
        mBackgroundDrawMapView.setScreenShotCallback(new IBackgroundMapView.IScreenShotCallback() {
            @Override
            public void onScreenShot(BitmapDrawable bitmap) {
                if (null != mScreenShotCallback) {
                    Log.i(TAG, "onScreenShot: bitmap " + bitmap.getBitmap().getByteCount());
                    mScreenShotCallback.onScreenShot(bitmap);
                }
            }
        });
        mBackgroundDrawMapView.openBackgroundMap();
    }

    private void routePlan() {
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
        // 步行后台导航，模拟导航
        boolean b ;
        if (!BNaviMainActivity.isFakeNavi) {
            b = WalkNavigateHelper.getInstance().startBkgNavi(null, this, WalkNavigateHelper.NaviMode.RealNavi);
        } else {
            b = WalkNavigateHelper.getInstance().startBkgNavi(null, this, WalkNavigateHelper.NaviMode.FakeNavi);
            WalkNavigateHelper.getInstance().setSimulateNaviSpeed(3);
        }

        if (!b) {
            Toast.makeText(this, "步行导航启动失败", Toast.LENGTH_SHORT).show();
        } else {
            createBackgroundDrawMapView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundDrawMapView) {
            mBackgroundDrawMapView.setScreenShotCallback(null);
            mBackgroundDrawMapView.onDestroy();
            mBackgroundDrawMapView = null;
        }
        WalkNavigateHelper.getInstance().getAuthManager().removeAuthListener(authListener);
        OffScreenMapNaviHelper.getInstance().destroyBkgNavi(null);
        if (BIKE_NAVI) {
            BikeNavigateHelper.getInstance().quit();
        } else {
            WalkNavigateHelper.getInstance().quit();
        }
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
    private void authWalk() {
        WalkNavigateHelper.getInstance().getAuthManager().addAuthListener(authListener);
        // 加载ak下所有的配额（目前有多实例和诱导数据透出）
        WalkNavigateHelper.getInstance().getAuthManager().loadAuth(getApplicationContext(), BWAuthLicenseType.AUTH_TYPE_MULTI_MAP, true);
    }

    public static void setScreenShotCallback(IBackgroundMapView.IScreenShotCallback onScreenShot) {
        mScreenShotCallback = onScreenShot;
    }
}
