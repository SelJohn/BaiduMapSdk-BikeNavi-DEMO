package com.baidu.bikenavi_demo;

import java.util.ArrayList;
import java.util.List;

import com.baidu.bikenavi_demo.floating.ComNaviBean;
import com.baidu.bikenavi_demo.floating.ComObservable;
import com.baidu.bikenavi_demo.floating.FloatingWindowService;
import com.baidu.bikenavi_demo.floating.data.RGDataOBS;
import com.baidu.bikenavi_demo.util.ITrace;
import com.baidu.bikenavi_demo.util.WalkBikeTrackFileUtil;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBEngineInitListener;
import com.baidu.mapapi.bikenavi.adapter.IBNaviCalcRouteListener;
import com.baidu.mapapi.bikenavi.adapter.IBRouteGuidanceListener;
import com.baidu.mapapi.bikenavi.adapter.IBRoutePlanListener;
import com.baidu.mapapi.bikenavi.model.BikeRouteDetailInfo;
import com.baidu.mapapi.bikenavi.model.BikeRoutePlanError;
import com.baidu.mapapi.bikenavi.model.BikeSimpleMapInfo;
import com.baidu.mapapi.bikenavi.model.IBRouteIconInfo;
import com.baidu.mapapi.bikenavi.params.BikeNaviLaunchParam;
import com.baidu.mapapi.bikenavi.params.BikeRouteNodeInfo;
import com.baidu.mapapi.bikenavi.params.BikeRouteNodeType;
import com.baidu.mapapi.common.auth.BWAuthFuncResult;
import com.baidu.mapapi.common.auth.BWAuthLicenseType;
import com.baidu.mapapi.common.auth.BWAuthResult;
import com.baidu.mapapi.common.auth.IBWAuthListener;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapLanguage;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.Polygon;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWEngineInitListener;
import com.baidu.mapapi.walknavi.adapter.IWNaviCalcRouteListener;
import com.baidu.mapapi.walknavi.adapter.IWRoutePlanListener;
import com.baidu.mapapi.walknavi.model.RouteGuideKind;
import com.baidu.mapapi.walknavi.model.WalkRoutePlanError;
import com.baidu.mapapi.walknavi.params.RouteNodeType;
import com.baidu.mapapi.walknavi.params.WalkNaviLaunchParam;
import com.baidu.mapapi.walknavi.params.WalkRouteNodeInfo;
import com.baidu.platform.comapi.basestruct.Point;
import com.baidu.platform.comapi.location.CoordinateUtil;

import com.baidu.platform.comapi.license.LicenseCode;
import com.baidu.platform.comapi.license.AuthorizeServiceType;
import com.baidu.platform.comapi.walknavi.widget.ArCameraView;
import com.baidu.platform.comapi.wnplatform.mulitmap.IMultiNaviView;
import com.baidu.platform.comapi.wnplatform.option.EngineOptions;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

public class BNaviMainActivity extends Activity implements ITrace {

    private final static String TAG = BNaviMainActivity.class.getSimpleName();
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    public static boolean isFakeNavi = false;
    /*导航起终点Marker，可拖动改变起终点的坐标*/
    private Marker mStartMarker;
    private Marker mEndMarker;

    private LatLng startPt;
    private LatLng endPt;
    private Button bikeBtn;
    private Button bikeBtn1;

    private enum naviType {
        NONE,
        WALKNAVI,
        BIKENAVI
    }

    private naviType mNaviType = naviType.NONE; // 1:步行导航，2：骑行导航

    private BikeNaviLaunchParam mBikeParam;
    private WalkNaviLaunchParam mWalkParam;

    private WalkNaviLaunchParam mWalkSearchParam;

    private BikeNaviLaunchParam mBikeSearchParam;

    private static boolean isPermissionRequested = false;

    private BitmapDescriptor bdStart = BitmapDescriptorFactory
            .fromResource(R.drawable.icon_start);
    private BitmapDescriptor bdEnd = BitmapDescriptorFactory
            .fromResource(R.drawable.icon_end);

    private List<Polyline> mPolylines;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_main);
        requestPermission();
        mMapView = (MapView) findViewById(R.id.mapview);

        CheckBox simulation = findViewById(R.id.cb_simulation);
        simulation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isFakeNavi = isChecked;
            }
        });
        CheckBox naviStanding = findViewById(R.id.cb_navi_standing);
        naviStanding.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                BikeNavigateHelper.getInstance().setIfNaviStanding(isChecked);
            }
        });
        ComObservable.getInstance().addObserver(new RGDataOBS<ComNaviBean>() {
            @Override
            public void onDataUpdate(ComNaviBean comNaviBean) {
                if (comNaviBean.destroyed) {
                    naviStanding.setChecked(false);
                }
            }
        });
        /*骑行导航入口*/
        bikeBtn1 = (Button) findViewById(R.id.btn_bikenavi);
        bikeBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryClearPolylines();
                startBikeNavi();
            }
        });

        /*普通步行导航入口*/
        Button walkBtn = (Button) findViewById(R.id.btn_walknavi_normal);
        walkBtn.setOnClickListener(v -> {
            tryClearPolylines();
            mWalkParam.extraNaviMode(0);
            startWalkNavi();
        });

        /*普通步行导航入口(轨迹)*/
        Button walkTraceBtn = (Button) findViewById(R.id.btn_walknavi_trace);
        walkTraceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryClearPolylines();
                WalkBikeTrackFileUtil.getInstance().selectFile(BNaviMainActivity.this, BNaviMainActivity.this);
            }
        });

        /*AR步行导航入口*/
        Button arWalkBtn = (Button) findViewById(R.id.btn_walknavi_ar);
        arWalkBtn.setOnClickListener(v -> {
            tryClearPolylines();

            // 判断下权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA},
                            ArCameraView.WALK_AR_PERMISSION);
                    return;
                }
            }

            mWalkParam.extraNaviMode(1);
            startWalkNavi();
        });

        // 多实例骑行导航
        Button btnMultiBikeNavi = (Button) findViewById(R.id.btnMultiBikeNavi);
        btnMultiBikeNavi.setOnClickListener(v -> {
            tryClearPolylines();
            handleMultiBikeNaviClicked();
        });

        // 多实例步行导航
        Button btnMultiWalkNavi = (Button) findViewById(R.id.btnMultiWalkNavi);
        btnMultiWalkNavi.setOnClickListener(v -> {
            tryClearPolylines();
            mWalkParam.extraNaviMode(0);
            handleMultiWalkNaviClicked();
        });

        // 步行多路线
        findViewById(R.id.btnWalkSearchNavi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryClearPolylines();
                handleMultiWalkSearchNaviClicked();
            }
        });

        // 骑行多路线
        findViewById(R.id.btnBikeMultipleRoute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryClearPolylines();
                handleMultiNaviBikeClicked();
            }
        });

        // 骑行多路线2
        findViewById(R.id.btnBikeMultipleRoute2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BikeNavigateHelper.getInstance().isInitEngine() &&
                        !BikeNavigateHelper.getInstance().isNavigating()) {
                    BikeNavigateHelper.getInstance().unInitNaviEngine();
                }
                if (WalkNavigateHelper.getInstance().isInitEngine()) {
                    WalkNavigateHelper.getInstance().unInitNaviEngine();
                }
                BikeNavigateHelper.getInstance().setRouteGuidanceListener(BNaviMainActivity.this,
                        CommonGuideListener.BIKE_GUIDE_LISTENER);
                BikeMultipleRoute2Activity.showActivity(BNaviMainActivity.this);
            }
        });

        // 悬浮窗功能
        findViewById(R.id.btn_floating_window).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                } else {
                    startFloatingWindowService();
                }
            } else {
                startFloatingWindowService();
            }
        });

        startPt = new LatLng(34.381347, 108.987089);
        endPt = new LatLng(34.229479, 108.970481);
        initMapStatus();
        /*构造导航起终点参数对象*/
        BikeRouteNodeInfo bikeStartNode = new BikeRouteNodeInfo();
        bikeStartNode.setLocation(startPt);
        BikeRouteNodeInfo bikeEndNode = new BikeRouteNodeInfo();
        bikeEndNode.setLocation(endPt);
        mBikeParam = new BikeNaviLaunchParam().startNodeInfo(bikeStartNode).endNodeInfo(bikeEndNode);

        WalkRouteNodeInfo walkStartNode = new WalkRouteNodeInfo();
        walkStartNode.setLocation(startPt);
        WalkRouteNodeInfo walkEndNode = new WalkRouteNodeInfo();
        walkEndNode.setLocation(endPt);
        mWalkParam = new WalkNaviLaunchParam()
                .startNodeInfo(walkStartNode)
                .endNodeInfo(walkEndNode);

        WalkRouteNodeInfo walkStartNode1 = new WalkRouteNodeInfo();
        walkStartNode1.setType(RouteNodeType.KEYWORD);
        walkStartNode1.setLocation(startPt);
        walkStartNode1.setKeyword("--");
        WalkRouteNodeInfo walkEndNode1 = new WalkRouteNodeInfo();
        walkEndNode1.setType(RouteNodeType.KEYWORD);
        walkEndNode1.setKeyword("终点");
        walkEndNode1.setLocation(endPt);
        mWalkSearchParam = new WalkNaviLaunchParam()
                .extraNaviMode(0)
                .startNodeInfo(walkStartNode1)
                .endNodeInfo(walkEndNode1);

        BikeRouteNodeInfo bikeStartNode1 = new BikeRouteNodeInfo();
        bikeStartNode1.setType(BikeRouteNodeType.KEYWORD);
        bikeStartNode1.setLocation(startPt);
        bikeStartNode1.setKeyword("--");
        BikeRouteNodeInfo bikeEndNode1 = new BikeRouteNodeInfo();
        bikeEndNode1.setType(BikeRouteNodeType.KEYWORD);
        bikeEndNode1.setKeyword("终点");
        bikeEndNode1.setLocation(endPt);
        mBikeSearchParam = new BikeNaviLaunchParam()
                .extraNaviMode(0)
                .startNodeInfo(bikeStartNode1)
                .endNodeInfo(bikeEndNode1);

        /* 初始化起终点Marker */
        initOverlay();

        registerListener();
    }

    private void registerListener() {
        mMapView.getMap().setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                Log.e(TAG, "onMapClick -> " + point);
            }

            @Override
            public void onMapPoiClick(MapPoi poi) {
                Log.e(TAG, "onMapPoiClick -> " + poi);
            }
        });

        mMapView.getMap().setOnPolygonClickListener(new BaiduMap.OnPolygonClickListener() {
            @Override
            public boolean onPolygonClick(Polygon polygon) {
                Log.e(TAG, "onPolygonClick -> " + polygon);
                return false;
            }
        });

        mMapView.getMap().setOnPolylineClickListener(new BaiduMap.OnPolylineClickListener() {
            @Override
            public boolean onPolylineClick(Polyline polyline) {
                Log.e(TAG, "onPolylineClick -> " + polyline);
                Bundle extraInfo = polyline.getExtraInfo();
                int routeIndex = extraInfo.getInt("routeIndex", 0);
                if (mNaviType == naviType.WALKNAVI) {
                    // 步行
                    WalkNavigateHelper.getInstance().naviCalcRoute(routeIndex, new IWNaviCalcRouteListener() {
                        @Override
                        public void onNaviCalcRouteSuccess() {
                            WalkMultiActivity.showActivity(BNaviMainActivity.this);

//                            List<Overlay> overlays = new ArrayList<>(1);
//                            overlays.add(polyline);
//                            if (mPolylines != null) {
//                                overlays.addAll(mPolylines);
//                            }
//
//                            for (Overlay overlay : overlays) {
//                                overlay.remove();
//                            }
                        }

                        @Override
                        public void onNaviCalcRouteFail(WalkRoutePlanError error) {
                        }
                    });

                    if (mPolylines != null) {
                        for (Overlay overlay : mPolylines) {
                            overlay.remove();
                        }
                    }

                } else if (mNaviType == naviType.BIKENAVI) {
                    // 骑行
                    BikeNavigateHelper.getInstance().naviCalcRoute(routeIndex, new IBNaviCalcRouteListener() {
                        @Override
                        public void onNaviCalcRouteSuccess() {
//                            List<Overlay> overlays = new ArrayList<>(1);
//                            overlays.add(polyline);
//                            if (mPolylines != null) {
//                                overlays.addAll(mPolylines);
//                            }
//
//                            for (Overlay overlay : overlays) {
//                                overlay.remove();
//                            }

                            BikeNaviMultiActivity.showActivity(BNaviMainActivity.this);
                        }

                        @Override
                        public void onNaviCalcRouteFail(BikeRoutePlanError error) {

                        }
                    });
                    if (mPolylines != null) {
                        for (Overlay overlay : mPolylines) {
                            overlay.remove();
                        }
                    }
                }

                return false;
            }
        });

    }

    @Override
    public void readTraceFileFinish() {
        try {
            if (mNaviType == naviType.BIKENAVI && BikeNavigateHelper.getInstance().isInitEngine()) {
                BikeNavigateHelper.getInstance().unInitNaviEngine();
            }
            mNaviType = naviType.WALKNAVI;
            WalkNavigateHelper.getInstance().initNaviEngine(this, new IWEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    String planData = WalkBikeTrackFileUtil.getInstance().getPlanData();
                    if (planData == null) {
                        return;
                    }
                    String[] list = planData.split(",");
                    // 起点
                    WalkRouteNodeInfo walkStartNode = new WalkRouteNodeInfo();
                    Point startPoint = CoordinateUtil.bd09mcTobd09ll(Integer.parseInt(list[4]), Integer.parseInt(list[5]));
                    walkStartNode.setLocation(new LatLng(startPoint.getDoubleY(), startPoint.getDoubleX()));
                    walkStartNode.setType(RouteNodeType.LOCATION);
                    String startBuildingId = list[2];
                    String startFloorId = list[3];
                    if (!TextUtils.isEmpty(startBuildingId) && !TextUtils.isEmpty(startFloorId)) {
                        walkStartNode.setBuildingID(startBuildingId);
                        walkStartNode.setFloorID(startFloorId);
                    }
                    // 终点
                    WalkRouteNodeInfo walkEndNode = new WalkRouteNodeInfo();
                    Point endPoint = CoordinateUtil.bd09mcTobd09ll(Integer.parseInt(list[10]), Integer.parseInt(list[11]));
                    walkEndNode.setLocation(new LatLng(endPoint.getDoubleY(), endPoint.getDoubleX()));
                    walkEndNode.setType(RouteNodeType.LOCATION);
                    String endBuildingId = list[8];
                    String endFloorId = list[9];
                    if (!TextUtils.isEmpty(endBuildingId) && !TextUtils.isEmpty(endFloorId)) {
                        walkEndNode.setBuildingID(endBuildingId);
                        walkEndNode.setFloorID(endFloorId);
                    }
                    // params
                    WalkNaviLaunchParam walkParam = new WalkNaviLaunchParam().startNodeInfo(walkStartNode).endNodeInfo(walkEndNode);
                    walkParam.extraNaviMode(0);

                    WalkNavigateHelper.getInstance().routePlanWithRouteNode(walkParam, new IWRoutePlanListener() {
                        @Override
                        public void onRoutePlanStart() {
                        }

                        @Override
                        public void onRoutePlanSuccess() {
                            Intent intent = new Intent();
                            intent.setClass(BNaviMainActivity.this, WNaviGuideActivity.class);
                            startActivity(intent);
                            // 播放轨迹
                            WalkBikeTrackFileUtil.getInstance().playTrack();
                        }

                        @Override
                        public void onRoutePlanFail(WalkRoutePlanError error) {
                        }
                    });
                }

                @Override
                public void engineInitFail() {
                    WalkNavigateHelper.getInstance().unInitNaviEngine();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化地图状态
     */
    private void initMapStatus() {
        mBaiduMap = mMapView.getMap();
        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(startPt).zoom(15);
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }

    /**
     * 初始化导航起终点Marker
     */
    public void initOverlay() {

        MarkerOptions ooA = new MarkerOptions().position(startPt).icon(bdStart)
                .zIndex(9).draggable(true);

        mStartMarker = (Marker) (mBaiduMap.addOverlay(ooA));
        mStartMarker.setDraggable(true);
        MarkerOptions ooB = new MarkerOptions().position(endPt).icon(bdEnd)
                .zIndex(5);
        mEndMarker = (Marker) (mBaiduMap.addOverlay(ooB));
        mEndMarker.setDraggable(true);

        mBaiduMap.setOnMarkerDragListener(new BaiduMap.OnMarkerDragListener() {
            public void onMarkerDrag(Marker marker) {
            }

            public void onMarkerDragEnd(Marker marker) {
                if (marker == mStartMarker) {
                    startPt = marker.getPosition();
                } else if (marker == mEndMarker) {
                    endPt = marker.getPosition();
                }
                BikeRouteNodeInfo bikeStartNode = new BikeRouteNodeInfo();
                bikeStartNode.setLocation(startPt);
                BikeRouteNodeInfo bikeEndNode = new BikeRouteNodeInfo();
                bikeEndNode.setLocation(endPt);
                mBikeParam = new BikeNaviLaunchParam().startNodeInfo(bikeStartNode).endNodeInfo(bikeEndNode);

                WalkRouteNodeInfo walkStartNode = new WalkRouteNodeInfo();
                walkStartNode.setLocation(startPt);
                WalkRouteNodeInfo walkEndNode = new WalkRouteNodeInfo();
                walkEndNode.setLocation(endPt);
                mWalkParam = new WalkNaviLaunchParam().startNodeInfo(walkStartNode).endNodeInfo(walkEndNode);

                WalkRouteNodeInfo walkStartNode1 = new WalkRouteNodeInfo();
                walkStartNode1.setType(RouteNodeType.KEYWORD);
                walkStartNode1.setLocation(startPt);
                walkStartNode1.setKeyword("--");
                WalkRouteNodeInfo walkEndNode1 = new WalkRouteNodeInfo();
                walkEndNode1.setType(RouteNodeType.KEYWORD);
                walkEndNode1.setKeyword("终点");
                walkEndNode1.setLocation(endPt);
                mWalkSearchParam = new WalkNaviLaunchParam()
                        .extraNaviMode(0)
                        .startNodeInfo(walkStartNode1)
                        .endNodeInfo(walkEndNode1);

                BikeRouteNodeInfo bikeStartNode1 = new BikeRouteNodeInfo();
                bikeStartNode1.setType(BikeRouteNodeType.KEYWORD);
                bikeStartNode1.setLocation(startPt);
                bikeStartNode1.setKeyword("--");
                BikeRouteNodeInfo bikeEndNode1 = new BikeRouteNodeInfo();
                bikeEndNode1.setType(BikeRouteNodeType.KEYWORD);
                bikeEndNode1.setKeyword("终点");
                bikeEndNode1.setLocation(endPt);
                mBikeSearchParam = new BikeNaviLaunchParam()
                        .extraNaviMode(0)
                        .vehicle(1)
                        .startNodeInfo(bikeStartNode1)
                        .endNodeInfo(bikeEndNode1);
            }

            public void onMarkerDragStart(Marker marker) {
            }
        });
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    /**
     * 开始骑行导航
     */
    private void startBikeNavi() {
        Log.d(TAG, "startBikeNavi");
        try {
            if (mNaviType == naviType.WALKNAVI && WalkNavigateHelper.getInstance().isInitEngine()) {
                WalkNavigateHelper.getInstance().unInitNaviEngine();
            }
            mNaviType = naviType.BIKENAVI;
            BikeNavigateHelper.getInstance().initNaviEngine(this, new EngineOptions.Builder()
                    .setLanguageType(MapLanguage.ENGLISH)
                    .build(), new IBEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    Log.d(TAG, "BikeNavi engineInitSuccess");
                    routePlanWithBikeParam();
                }

                @Override
                public void engineInitFail() {
                    Log.d(TAG, "BikeNavi engineInitFail");
                    BikeNavigateHelper.getInstance().unInitNaviEngine();
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "startBikeNavi Exception");
            e.printStackTrace();
        }
    }

    /**
     * 开始步行导航
     */
    private void startWalkNavi() {
        Log.d(TAG, "startWalkNavi");
        try {
            if (mNaviType == naviType.BIKENAVI && BikeNavigateHelper.getInstance().isInitEngine()) {
                BikeNavigateHelper.getInstance().unInitNaviEngine();
            }
            mNaviType = naviType.WALKNAVI;
            authWalk();
            WalkNavigateHelper.getInstance().initNaviEngine(getApplicationContext(), new EngineOptions.Builder()
                    .setLanguageType(MapLanguage.ENGLISH)
                    .build(), new IWEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    Log.d(TAG, "WalkNavi engineInitSuccess");
                    // 步骑行多实例地图创建成功
                    routePlanWithWalkParam();
                }

                @Override
                public void engineInitFail() {
                    Log.d(TAG, "WalkNavi engineInitFail");
                    WalkNavigateHelper.getInstance().unInitNaviEngine();
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "startBikeNavi Exception");
            e.printStackTrace();
        }
    }

    /**
     * 发起骑行导航算路
     */
    private void routePlanWithBikeParam() {
        if (BikeNavigateHelper.getInstance().isNavigating()) {
            Intent intent = new Intent();
            intent.setClass(BNaviMainActivity.this, BNaviGuideActivity.class);
            startActivity(intent);
            return;
        }
        BikeNavigateHelper.getInstance().setRouteGuidanceListener(BNaviMainActivity.this,
                CommonGuideListener.BIKE_GUIDE_LISTENER);
        BikeNavigateHelper.getInstance().routePlanWithRouteNode(mBikeParam, new IBRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d(TAG, "BikeNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d(TAG, "BikeNavi onRoutePlanSuccess");
                Intent intent = new Intent();
                intent.setClass(BNaviMainActivity.this, BNaviGuideActivity.class);
                startActivity(intent);
            }

            @Override
            public void onRoutePlanFail(BikeRoutePlanError error) {
                Log.d(TAG, "BikeNavi onRoutePlanFail");
            }

        });
    }

    /**
     * 发起步行导航算路
     */
    private void routePlanWithWalkParam() {
        WalkNavigateHelper.getInstance().routePlanWithRouteNode(mWalkParam, new IWRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d(TAG, "WalkNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {

                Log.d(TAG, "onRoutePlanSuccess");

                Intent intent = new Intent();
                intent.setClass(BNaviMainActivity.this, WNaviGuideActivity.class);
                startActivity(intent);

            }

            @Override
            public void onRoutePlanFail(WalkRoutePlanError error) {
                Log.d(TAG, "WalkNavi onRoutePlanFail");
            }

        });
    }

    private void handleMultiBikeNaviClicked() {
        Log.d(TAG, "startBikeNavi");
        try {
            if (mNaviType == naviType.WALKNAVI && WalkNavigateHelper.getInstance().isInitEngine()) {
                WalkNavigateHelper.getInstance().unInitNaviEngine();
            }
            mNaviType = naviType.BIKENAVI;
            authBike();
            EngineOptions options = new EngineOptions.Builder()
                    .setLanguageType(MapLanguage.CHINESE)
                    .setRouteCustomWidthExt(11)
                    .build();
            BikeNavigateHelper.getInstance().initNaviEngine(this.getApplicationContext(), options, new IBEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    BikeNavigateHelper.getInstance().setShowLight(true);
                    Log.d(TAG, "BikeNavi engineInitSuccess");
                    MultiNaviViewProvider.getInstance().createDefaultMultiNaviView(BNaviMainActivity.this);
                    routePlanMultiWithBikeParam();
                }

                @Override
                public void engineInitFail() {
                    Log.d(TAG, "BikeNavi engineInitFail");
                    BikeNavigateHelper.getInstance().unInitNaviEngine();
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "startBikeNavi Exception");
            e.printStackTrace();
        }
    }

    private void handleMultiWalkNaviClicked() {
        Log.d(TAG, "startWalkNavi");
        try {
            if (mNaviType == naviType.BIKENAVI && BikeNavigateHelper.getInstance().isInitEngine()) {
                BikeNavigateHelper.getInstance().unInitNaviEngine();
            }
            mNaviType = naviType.WALKNAVI;
            authWalk();
            WalkNavigateHelper.getInstance().initNaviEngine(this.getApplicationContext(), new IWEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    Log.d(TAG, "WalkNavi engineInitSuccess");
                    // 创建多实例窗口并设置类型
                    MultiNaviViewProvider.IMultiNaviViewProxy multiNaviView = MultiNaviViewProvider.getInstance().createDefaultMultiNaviView(BNaviMainActivity.this);
                    multiNaviView.setNaviType(IMultiNaviView.TYPE_NAVI_WALK);

                    routePlanMultiWithWalkParam();
                }

                @Override
                public void engineInitFail() {
                    Log.d(TAG, "WalkNavi engineInitFail");
                    WalkNavigateHelper.getInstance().unInitNaviEngine();
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "startBikeNavi Exception");
            e.printStackTrace();
        }
    }

    /**
     * 发起步行导航算路
     */
    private void routePlanMultiWithWalkParam() {
        WalkNavigateHelper.getInstance().routePlanWithRouteNode(mWalkParam, new IWRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d(TAG, "WalkNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d(TAG, "onRoutePlanSuccess");
                WalkMultiActivity.showActivity(BNaviMainActivity.this);
            }

            @Override
            public void onRoutePlanFail(WalkRoutePlanError error) {
                Log.d(TAG, "WalkNavi onRoutePlanFail");
            }

        });
    }

    /**
     * 步行多路线
     */
    private void handleMultiWalkSearchNaviClicked() {
        Log.d(TAG, "startWalkNavi");
        try {
            if (mNaviType == naviType.BIKENAVI && BikeNavigateHelper.getInstance().isInitEngine()) {
                BikeNavigateHelper.getInstance().unInitNaviEngine();
            }
            mNaviType = naviType.WALKNAVI;
            authWalk();
            WalkNavigateHelper.getInstance().initNaviEngine(this.getApplicationContext(),
                    new EngineOptions.Builder()
                            .setLanguageType(MapLanguage.ENGLISH)
                            .build(),
                    new IWEngineInitListener() {
                        @Override
                        public void engineInitSuccess() {
                            Log.d(TAG, "WalkNavi engineInitSuccess");
                            // 创建多实例窗口并设置类型
                            MultiNaviViewProvider.IMultiNaviViewProxy multiNaviView = MultiNaviViewProvider.getInstance().createDefaultMultiNaviView(BNaviMainActivity.this);
                            multiNaviView.setNaviType(IMultiNaviView.TYPE_NAVI_WALK);
                            routePlanMultiSearchNaviWithWalkParam();
                        }

                        @Override
                        public void engineInitFail() {
                            Log.d(TAG, "WalkNavi engineInitFail");
                            WalkNavigateHelper.getInstance().unInitNaviEngine();
                        }
                    });
        } catch (Exception e) {
            Log.d(TAG, "startBikeNavi Exception");
            e.printStackTrace();
        }
    }

    /**
     * 发起步行导航算路
     */
    private void routePlanMultiSearchNaviWithWalkParam() {
        mPolylines = null;
        WalkNavigateHelper.getInstance().routePlanWithRouteNode(mWalkSearchParam, new IWRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d(TAG, "WalkNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d(TAG, "onRoutePlanSuccess");
                mPolylines = WalkNavigateHelper.getInstance().displayRoutePlanResult(mMapView, null);
            }

            @Override
            public void onRoutePlanFail(WalkRoutePlanError error) {
                Log.d(TAG, "WalkNavi onRoutePlanFail");
            }

        });
    }

    /**
     * 骑行多路线
     */
    private void handleMultiNaviBikeClicked() {
        Log.d(TAG, "startBikeNavi");
        try {
            if (mNaviType == naviType.WALKNAVI && WalkNavigateHelper.getInstance().isInitEngine()) {
                WalkNavigateHelper.getInstance().unInitNaviEngine();
            }
            mNaviType = naviType.BIKENAVI;
            BikeNavigateHelper.getInstance().initNaviEngine(this.getApplicationContext(), new EngineOptions.Builder()
                    .setLanguageType(MapLanguage.ENGLISH)
                    .enableLog(true)
                    .build(), new IBEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    BikeNavigateHelper.getInstance().setShowLight(true);
                    MultiNaviViewProvider.getInstance().createDefaultMultiNaviView(BNaviMainActivity.this);
                    Log.d(TAG, "BikeNavi engineInitSuccess");
                    routePlanMultiSearchNaviWithBikeParam();
                }

                @Override
                public void engineInitFail() {
                    Log.d(TAG, "BikeNavi engineInitFail");
                    BikeNavigateHelper.getInstance().unInitNaviEngine();
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "startBikeNavi Exception");
            e.printStackTrace();
        }
    }

    private void routePlanMultiSearchNaviWithBikeParam() {
        mPolylines = null;
        BikeNavigateHelper.getInstance().setRouteGuidanceListener(BNaviMainActivity.this,
                CommonGuideListener.BIKE_GUIDE_LISTENER);
        BikeNavigateHelper.getInstance().routePlanWithRouteNode(mBikeSearchParam, new IBRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d(TAG, "BikeNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d(TAG, "BikeNavi onRoutePlanSuccess");
//                BikeNaviMultiActivity.showActivity(BNaviMainActivity.this);
                mPolylines = BikeNavigateHelper.getInstance().displayRoutePlanResult(mMapView, null);
            }

            @Override
            public void onRoutePlanFail(BikeRoutePlanError error) {
                Log.d(TAG, "BikeNavi onRoutePlanFail");
            }

        });
    }

    /**
     * 发起骑行导航算路
     */
    private void routePlanMultiWithBikeParam() {
        BikeNavigateHelper.getInstance().setRouteGuidanceListener(BNaviMainActivity.this,
                CommonGuideListener.BIKE_GUIDE_LISTENER);
        BikeNavigateHelper.getInstance().routePlanWithRouteNode(mBikeParam, new IBRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d(TAG, "BikeNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d(TAG, "BikeNavi onRoutePlanSuccess");
                BikeNaviMultiActivity.showActivity(BNaviMainActivity.this);
            }

            @Override
            public void onRoutePlanFail(BikeRoutePlanError error) {
                Log.d(TAG, "BikeNavi onRoutePlanFail");
            }

        });
    }

    private void authTip(int code) {
        switch (code) {
            case LicenseCode.CODE_LICENSE_SERVICE_NO_ERROR:
                Toast.makeText(BNaviMainActivity.this, "授权成功！", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "授权成功！");
                break;
            case LicenseCode.CODE_LICENSE_SERVICE_INNER_ERROR:
            case LicenseCode.CODE_LICENSE_SERVICE_NETWORK_ERROR:
            case LicenseCode.CODE_LICENSE_SERVICE_NETWORK_TIMEOUT:
            case LicenseCode.CODE_LICENSE_SERVICE_SERVER_ERROR:
                Toast.makeText(BNaviMainActivity.this, "网络和服务内部相关错误将不影响本次多实例同步显示导航信息", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "网络和服务内部相关错误将不影响本次多实例同步显示导航信息");
                break;
            case LicenseCode.CODE_LICENSE_SERVICE_NO_PERMISSION:
                Toast.makeText(BNaviMainActivity.this, "授权失败！无相关高级权限,请联系商务或PM开通", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "授权失败！无相关高级权限,请联系商务或PM开通");
                break;
            case LicenseCode.CODE_LICENSE_SERVICE_LICENSE_STATUS_ERROR:
                Toast.makeText(BNaviMainActivity.this, "授权失败！lincese验签失败", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "授权失败！lincese验签失败");
                break;

            case LicenseCode.CODE_LICENSE_SERVICE_PARAMETER_ERROR:
            case LicenseCode.CODE_LICENSE_SERVICE_MODE_ERROR:
                Toast.makeText(BNaviMainActivity.this, "授权失败！参数异常", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "授权失败！参数异常");
                break;

            case LicenseCode.CODE_LICENSE_SERVICE_NO_QUOTA:
            case LicenseCode.CODE_LICENSE_SERVICE_QUOTA_NO_ENOUGH:
            case LicenseCode.CODE_LICENSE_SERVICE_QUOTA_INVALID:
                Toast.makeText(BNaviMainActivity.this, "授权失败！设备lincese配额异常", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "授权失败！设备lincese配额异常");
                break;
            default:
                Toast.makeText(BNaviMainActivity.this, "未知异常", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "未知异常");
                break;
        }
    }

    /**
     * Android6.0之后需要动态申请权限
     */
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !isPermissionRequested) {

            isPermissionRequested = true;

            ArrayList<String> permissionsList = new ArrayList<>();

            String[] permissions = {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    Manifest.permission.WRITE_SETTINGS,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_MULTICAST_STATE


            };

            for (String perm : permissions) {
                if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(perm)) {
                    permissionsList.add(perm);
                    // 进入到这里代表没有权限.
                }
            }

            if (permissionsList.isEmpty()) {
                return;
            } else {
                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]), 0);
            }
        }
    }

    protected void onPause() {
        super.onPause();
        mMapView.onPause();
        tryClearPolylines();
    }

    private void tryClearPolylines() {
        if (mPolylines != null) {
            for (Polyline polyline : mPolylines) {
                polyline.remove();
            }
        }
    }

    private void startFloatingWindowService() {
        Intent serviceIntent = new Intent(this, FloatingWindowService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        mBaiduMap.setMapLanguage(mBaiduMap.getMapLanguage());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        bdStart.recycle();
        bdEnd.recycle();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ArCameraView.WALK_AR_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "没有相机权限,请打开后重试", Toast.LENGTH_SHORT).show();
            } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mWalkParam.extraNaviMode(1);
                startWalkNavi();
            }
        }
    }

    /**
     * 骑行license鉴权
     */
    private void authBike() {
        BikeNavigateHelper.getInstance().getAuthManager().addAuthListener(new IBWAuthListener() {
            @Override
            public void auth(BWAuthResult result) {
                authTip(result.getErrorCode());
                Log.i(TAG, "BWAuthResult: " + result);

                for (BWAuthFuncResult bwAuthFuncResult :
                        result.getBWAuthFuncResults()) {
                    switch (bwAuthFuncResult.getFuncType()) {
                        case AuthorizeServiceType.TYPE_AUTHORIZE_SERVICE_RIDING_NAVI_MULTI:
                            if (bwAuthFuncResult.getActiveStatus() == LicenseCode.CODE_LICENSE_SERVICE_NO_ERROR) {
                                // 表示多实例有权限
                                Toast.makeText(BNaviMainActivity.this, "多实例有权限", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case AuthorizeServiceType.TYPE_AUTHORIZE_SERVICE_RIDING_GUIDE_INFO:
                            if (bwAuthFuncResult.getActiveStatus() == LicenseCode.CODE_LICENSE_SERVICE_NO_ERROR) {
                                // 表示行中诱导数据有权限
                                Toast.makeText(BNaviMainActivity.this, "诱导数据有权限", Toast.LENGTH_SHORT).show();

                            }
                            break;
                    }
                }
            }
        });
        // 激活ak下所有的配额（目前有多实例和诱导数据透出）
        BikeNavigateHelper.getInstance().getAuthManager().loadAuth(getApplicationContext(), BWAuthLicenseType.AUTH_TYPE_MULTI_MAP, false);

    }

    /**
     * 步行license鉴权
     */
    private void authWalk() {
        WalkNavigateHelper.getInstance().getAuthManager().addAuthListener(new IBWAuthListener() {
            @Override
            public void auth(BWAuthResult result) {
                authTip(result.getErrorCode());
                Log.i(TAG, "BWAuthResult: " + result);
                for (BWAuthFuncResult bwAuthFuncResult :
                        result.getBWAuthFuncResults()) {
                    switch (bwAuthFuncResult.getFuncType()) {
                        case AuthorizeServiceType.TYPE_AUTHORIZE_SERVICE_RIDING_NAVI_MULTI:
                            if (bwAuthFuncResult.getActiveStatus() == LicenseCode.CODE_LICENSE_SERVICE_NO_ERROR) {
                                // 表示多实例有权限
                                Toast.makeText(BNaviMainActivity.this, "多实例有权限", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case AuthorizeServiceType.TYPE_AUTHORIZE_SERVICE_RIDING_GUIDE_INFO:
                            if (bwAuthFuncResult.getActiveStatus() == LicenseCode.CODE_LICENSE_SERVICE_NO_ERROR) {
                                // 表示行中诱导数据有权限
                                Toast.makeText(BNaviMainActivity.this, "诱导数据有权限", Toast.LENGTH_SHORT).show();

                            }
                            break;
                    }
                }
            }
        });
        // 加载ak下所有的配额（目前有多实例和诱导数据透出）
        WalkNavigateHelper.getInstance().getAuthManager().loadAuth(getApplicationContext(), BWAuthLicenseType.AUTH_TYPE_ALL, false);

    }
}
