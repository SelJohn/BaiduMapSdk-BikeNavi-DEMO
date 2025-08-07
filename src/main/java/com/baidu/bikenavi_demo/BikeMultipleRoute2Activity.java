package com.baidu.bikenavi_demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBEngineInitListener;
import com.baidu.mapapi.bikenavi.adapter.IBNaviCalcRouteListener;
import com.baidu.mapapi.bikenavi.adapter.IBRoutePlanListener;
import com.baidu.mapapi.bikenavi.model.BikeRoutePlanError;
import com.baidu.mapapi.bikenavi.params.BikeNaviLaunchParam;
import com.baidu.mapapi.bikenavi.params.BikeRouteNodeInfo;
import com.baidu.mapapi.bikenavi.params.BikeRouteNodeType;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.model.LatLng;

import java.util.List;

/**
 * @author zhuxiaoan
 * @date 2024/8/20
 * @describe
 */
public class BikeMultipleRoute2Activity extends Activity implements BNaviGuideActivity.INaviListener {

    private MapView mapview;
    private BikeNaviLaunchParam bikeSearchParam;
    private List<Polyline> polylines;

    public static void showActivity(Context context) {
        context.startActivity(new Intent(context, BikeMultipleRoute2Activity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_mltiple_route_2);

        initView(savedInstanceState);
        registerListener();
        bindData();
    }

    private void initView(Bundle savedInstanceState) {
        this.mapview = findViewById(R.id.mapview);

        BikeRouteNodeInfo bikeStartNode1 = new BikeRouteNodeInfo();
        bikeStartNode1.setType(BikeRouteNodeType.KEYWORD);
        bikeStartNode1.setLocation(new LatLng(40.057038, 116.307899));
        bikeStartNode1.setKeyword("百度大厦");
        BikeRouteNodeInfo bikeEndNode1 = new BikeRouteNodeInfo();
        bikeEndNode1.setType(BikeRouteNodeType.KEYWORD);
        bikeEndNode1.setKeyword("清河万象汇");
        bikeEndNode1.setLocation(new LatLng(40.035916, 116.340722));
        bikeSearchParam = new BikeNaviLaunchParam()
                .extraNaviMode(0)
                .startNodeInfo(bikeStartNode1)
                .endNodeInfo(bikeEndNode1);
    }

    private void registerListener() {
        mapview.getMap().setOnMapLoadedCallback(new BaiduMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                // 地图加载完成后, 显示路线
                showBikeRoutes();
            }
        });

        mapview.getMap().setOnPolylineClickListener(new BaiduMap.OnPolylineClickListener() {
            @Override
            public boolean onPolylineClick(Polyline polyline) {
                Bundle extraInfo = polyline.getExtraInfo();
                if (extraInfo == null) {
                    return false;
                }
                int routeIndex = extraInfo.getInt("routeIndex", 0);
                if (polylines != null) {
                    for (Overlay overlay : polylines) {
                        overlay.remove();
                    }

                    polylines = null;
                }

                BikeNavigateHelper.getInstance().naviCalcRoute(routeIndex, new IBNaviCalcRouteListener() {
                    @Override
                    public void onNaviCalcRouteSuccess() {
                        BNaviGuideActivity.showActivity(BikeMultipleRoute2Activity.this);
                    }

                    @Override
                    public void onNaviCalcRouteFail(BikeRoutePlanError error) {
                    }

                });

                return false;
            }
        });

        BNaviGuideActivity.addBikeNaviListener(this);
    }

    private void bindData() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapview.onResume();
        if (!BikeNavigateHelper.getInstance().isNavigating()) {
            View viewById = findViewById(R.id.resume_navi);
            viewById.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapview.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BNaviGuideActivity.removeBikeNaviListener(this);
        mapview.onDestroy();
    }

    private void showBikeRoutes() {
        if (!BikeNavigateHelper.getInstance().isInitEngine()) {
            BikeNavigateHelper.getInstance().initNaviEngine(this, new IBEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    if (polylines != null) {
                        for (Overlay overlay : polylines) {
                            overlay.remove();
                        }
                        polylines = null;
                    }
                    routePlanMultiNaviWithBikeParam();
                }

                @Override
                public void engineInitFail() {
                    BikeNavigateHelper.getInstance().unInitNaviEngine();
                }
            });
        } else {
            if (BikeNavigateHelper.getInstance().isNavigating()) {
                View viewById = findViewById(R.id.resume_navi);
                viewById.setVisibility(View.VISIBLE);
                viewById.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        BNaviGuideActivity.showActivity(BikeMultipleRoute2Activity.this);
                    }
                });
            }
        }
    }

    private void routePlanMultiNaviWithBikeParam() {
        polylines = null;
        BikeNavigateHelper.getInstance().routePlanWithRouteNode(bikeSearchParam, new IBRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
            }

            @Override
            public void onRoutePlanSuccess() {
                polylines = BikeNavigateHelper.getInstance().displayRoutePlanResult(mapview, null);
            }

            @Override
            public void onRoutePlanFail(BikeRoutePlanError error) {
            }

        });
    }

    @Override
    public void quit() {
        showBikeRoutes();
    }
}
