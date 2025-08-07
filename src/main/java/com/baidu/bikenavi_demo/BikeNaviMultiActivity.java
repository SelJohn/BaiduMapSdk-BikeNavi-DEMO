/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.bikenavi_demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.baidu.bikenavi_demo.floating.ComObservable;
import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBRouteGuidanceListener;
import com.baidu.mapapi.bikenavi.model.BikeNaviDisplayOption;
import com.baidu.mapapi.bikenavi.model.BikeRouteDetailInfo;
import com.baidu.mapapi.bikenavi.model.IBRouteIconInfo;
import com.baidu.mapapi.bikenavi.params.BikeNaviLaunchParam;
import com.baidu.mapapi.walknavi.model.RouteGuideKind;
import com.baidu.platform.comapi.wnplatform.mulitmap.IMultiNaviView;

public class BikeNaviMultiActivity extends Activity {
    private final static String TAG = BikeNaviMultiActivity.class.getSimpleName();
    private BikeNavigateHelper mNaviHelper;
    BikeNaviLaunchParam param;

    private MultiNaviViewProvider.IMultiNaviViewProxy multiNaviView;

    public static void showActivity(Context context) {
        context.startActivity(new Intent(context, BikeNaviMultiActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNaviHelper.quit();
        if (multiNaviView != null) {
            multiNaviView.onDestroy();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNaviHelper.resume();
        if (multiNaviView != null) {
            multiNaviView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNaviHelper.pause();
        if (multiNaviView != null) {
            multiNaviView.onPause();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNaviHelper = BikeNavigateHelper.getInstance();
        multiNaviView = MultiNaviViewProvider.getInstance().getDefaultMultiNaviView();
        multiNaviView.setNaviType(IMultiNaviView.TYPE_NAVI_RIDE);

        BikeNaviDisplayOption bikeNaviDisplayOption = new BikeNaviDisplayOption()
                .showSpeedLayout(true) // 是否展示速度切换布局
                .showTopGuideLayout(true)  // 是否展示顶部引导布局
                .showLocationImage(true);  // 是否展示视角切换资源
        mNaviHelper.setBikeNaviDisplayOption(bikeNaviDisplayOption);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setLayoutParams(params);
        View view = mNaviHelper.onCreate(BikeNaviMultiActivity.this);
        if (view != null) {
            frameLayout.addView(view);
            if (multiNaviView != null) {
                multiNaviView.injectMultiNaviView(frameLayout, new FrameLayout.LayoutParams(400, 600));
            }

            setContentView(frameLayout);
        }
//        multiNaviView.getMapTextureView().post(new Runnable() {
//            @Override
//            public void run() {
//                multiNaviView.setMapCustomStylePath(getExternalFilesDir(null).getAbsolutePath() + "/map123.sty");
//                multiNaviView.showPoiMark(false);
//                multiNaviView.getMapTextureView().setTraffic(true);
//            }
//        });
        mNaviHelper.setBikeNaviStatusListener(() -> Log.d(TAG, "onNaviExit"));

        mNaviHelper.setTTsPlayer((s, b) -> {
            Log.d("tts", s);
            return 0;
        });
        if (BNaviMainActivity.isFakeNavi) {
            mNaviHelper.startBikeNavi(BikeNaviMultiActivity.this, BikeNavigateHelper.NaviMode.FakeNavi);
            mNaviHelper.setSimulateNaviSpeed(5);
        } else {
            mNaviHelper.startBikeNavi(BikeNaviMultiActivity.this);
        }


        mNaviHelper.setRouteGuidanceListener(this, new IBRouteGuidanceListener() {
            @Override
            public void onRouteGuideIconInfoUpdate(IBRouteIconInfo routeIconInfo) {
                if (routeIconInfo != null) {
                    Log.d("GuideIconObjectUpdate", "onRoadGuideTextUpdate   Drawable=: " + routeIconInfo.getIconDrawable()
                            + " Name=: " + routeIconInfo.getIconName());
                }
            }

            @Override
            public void onRouteGuideIconUpdate(Drawable icon) {

            }

            @Override
            public void onRouteGuideKind(RouteGuideKind routeGuideKind) {

            }

            @Override
            public void onRoadGuideTextUpdate(CharSequence charSequence, CharSequence charSequence1) {

            }

            @Override
            public void onRemainDistanceUpdate(CharSequence charSequence) {

            }

            @Override
            public void onRemainTimeUpdate(CharSequence charSequence) {

            }

            @Override
            public void onGpsStatusChange(CharSequence charSequence, Drawable drawable) {

            }

            @Override
            public void onRouteFarAway(CharSequence charSequence, Drawable drawable) {

            }

            @Override
            public void onRoutePlanYawing(CharSequence charSequence, Drawable drawable) {

            }

            @Override
            public void onReRouteComplete() {

            }

            @Override
            public void onArriveDest() {

            }

            @Override
            public void onVibrate() {

            }

            @Override
            public void onGetRouteDetailInfo(BikeRouteDetailInfo bikeRouteDetailInfo) {

            }

            @Override
            public void onNaviLocationUpdate() {

            }

            @Override
            public void onRemainDistanceUpdate(int remainDistance) {
                IBRouteGuidanceListener.super.onRemainDistanceUpdate(remainDistance);
            }

            @Override
            public void onRemainTimeUpdate(int remainTime) {
                IBRouteGuidanceListener.super.onRemainTimeUpdate(remainTime);

            }

            @Override
            public void onRouteRemainTrafficLightCountUpdate(int remainCount) {
//                ComObservable.getInstance().update(new ComNaviBean(String.valueOf(remainCount), null));
                Log.i(TAG, "onRouteRemainTrafficLightCountUpdate: " + remainCount);
            }
        });
    }

}
