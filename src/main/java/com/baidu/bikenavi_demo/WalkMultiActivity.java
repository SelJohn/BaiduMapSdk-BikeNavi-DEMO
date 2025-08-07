/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.bikenavi_demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.baidu.mapapi.map.AbsBackgroundDrawNaviLayer;
import com.baidu.mapapi.map.BackgroundDrawMapView;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWNaviStatusListener;
import com.baidu.mapapi.walknavi.adapter.IWRouteGuidanceListener;
import com.baidu.mapapi.walknavi.model.IWRouteIconInfo;
import com.baidu.mapapi.walknavi.model.RouteGuideKind;
import com.baidu.mapapi.walknavi.model.WalkNaviDisplayOption;
import com.baidu.mapapi.walknavi.model.WalkSimpleMapInfo;
import com.baidu.platform.comapi.walknavi.WalkNaviModeSwitchListener;
import com.baidu.platform.comapi.walknavi.widget.ArCameraView;


public class WalkMultiActivity extends Activity {
    private final static String TAG = WalkMultiActivity.class.getSimpleName();
    private WalkNavigateHelper mNaviHelper;
    private MultiNaviViewProvider.IMultiNaviViewProxy mMultiNaviView;

    private BackgroundDrawMapView mBackgroundDrawMapView;

    private ImageView mIvScreenshot;

    private boolean isInBackgroundDrawing = false;

    public static void showActivity(Context context) {
        context.startActivity(new Intent(context, WalkMultiActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNaviHelper = WalkNavigateHelper.getInstance();

        mMultiNaviView = MultiNaviViewProvider.getInstance().getDefaultMultiNaviView();

        WalkNaviDisplayOption walkNaviDisplayOption = new WalkNaviDisplayOption()
                .showImageToAr(true) // 是否展示AR图片
                .showCalorieLayoutEnable(true) // 是否展示热量消耗布局
                .showLocationImage(true);  // 是否展示视角切换资源
        mNaviHelper.setWalkNaviDisplayOption(walkNaviDisplayOption);
        try {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            FrameLayout frameLayout = new FrameLayout(this);
            frameLayout.setLayoutParams(params);

            View view = mNaviHelper.onCreate(WalkMultiActivity.this);
            if (view != null) {
                // 添加导航地图
                frameLayout.addView(view);
                if (mMultiNaviView != null) {
                    // 添加小地图
                    mMultiNaviView.injectMultiNaviView(frameLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600));
                    mMultiNaviView.setSupBackgroundDraw(true);
                    mMultiNaviView.setDefaultLevel(18f);
                }

                // 添加后台绘制地图
                mBackgroundDrawMapView = new BackgroundDrawMapView(this);
                frameLayout.addView(mBackgroundDrawMapView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600));
                // 绑定 view
                mBackgroundDrawMapView.setVisibility(View.GONE);
                if (mMultiNaviView != null) {
                    mBackgroundDrawMapView.bindView(mMultiNaviView.getMapTextureView());
//                    mBackgroundDrawMapView.bindView(mNaviHelper.getNaviMap().getMap().getGLMapView());
                }

                LinearLayout linearLayout = new LinearLayout(WalkMultiActivity.this);
                linearLayout.setOrientation(LinearLayout.VERTICAL);

                Button btnTest1 = new Button(WalkMultiActivity.this);
                btnTest1.setText("测试进入后台导航");
                linearLayout.addView(btnTest1, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                btnTest1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mMultiNaviView != null) {
                            mMultiNaviView.onPause();
//                            mNaviHelper.pause();
                            mBackgroundDrawMapView.openBackgroundMap();
//                            mNaviHelper.getNaviMap().getMap().getGLMapView().setSupBackgroundDraw(true);
//                            mNaviHelper.getNaviMap().getMap().getGLMapView().onBackground();
                            mBackgroundDrawMapView.setVisibility(View.VISIBLE);
                            mNaviHelper.openBackgroundDrawNavi(WalkMultiActivity.this);
                            AbsBackgroundDrawNaviLayer backgroundDrawNaviLayer = mNaviHelper.getBackgroundDrawNaviLayer();
//                            backgroundDrawNaviLayer.setIsLocationDirectionFollowPhone(true);
                            mBackgroundDrawMapView.addLayer(backgroundDrawNaviLayer);
                            backgroundDrawNaviLayer.setEraseEffect(AbsBackgroundDrawNaviLayer.EraseEffect.ALREADY_PASSED_CHANGE_COLOR);

                            Bitmap bitmap = mMultiNaviView.getMapTextureView().getBitmap();
                            mIvScreenshot.setImageBitmap(bitmap);
                            isInBackgroundDrawing = true;
                        }
                    }
                });

                Button btnTest2 = new Button(WalkMultiActivity.this);
                btnTest2.setText("测试退出后台导航");
                linearLayout.addView(btnTest2, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                btnTest2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mMultiNaviView != null) {
                            mMultiNaviView.onResume();
//                            mNaviHelper.resume();
                            mBackgroundDrawMapView.closeBackgroundMap();
//                            mNaviHelper.getNaviMap().getMap().getGLMapView().setSupBackgroundDraw(false);
//                            mNaviHelper.getNaviMap().getMap().getGLMapView().onForeground();
                            mBackgroundDrawMapView.setVisibility(View.GONE);
                            AbsBackgroundDrawNaviLayer backgroundDrawNaviLayer = mNaviHelper.getBackgroundDrawNaviLayer();
                            mBackgroundDrawMapView.removeLayer(backgroundDrawNaviLayer);
                            mNaviHelper.closeBackgroundDrawNavi();
                            isInBackgroundDrawing = false;
                        }
                    }
                });

                FrameLayout.LayoutParams linearLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                linearLayoutParams.gravity = Gravity.CENTER;
                frameLayout.addView(linearLayout, linearLayoutParams);


                mIvScreenshot = new ImageView(this);
                FrameLayout.LayoutParams linearLayoutParams1 = new FrameLayout.LayoutParams(300, 300);
                linearLayoutParams1.gravity = Gravity.CENTER_HORIZONTAL;
                frameLayout.addView(mIvScreenshot, linearLayoutParams1);
                mIvScreenshot.setVisibility(View.GONE);

                // 设置界面布局
                setContentView(frameLayout);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        registerListener();
        if (BNaviMainActivity.isFakeNavi) {
            mNaviHelper.startWalkNavi(this, WalkNavigateHelper.NaviMode.FakeNavi);
            mNaviHelper.setSimulateNaviSpeed(3);
        } else {
            mNaviHelper.startWalkNavi(this);
        }
    }

    private void registerListener() {

        mNaviHelper.setWalkNaviStatusListener(new IWNaviStatusListener() {
            @Override
            public void onWalkNaviModeChange(int mode, WalkNaviModeSwitchListener listener) {
                Log.d(TAG, "onWalkNaviModeChange : " + mode);
                mNaviHelper.switchWalkNaviMode(WalkMultiActivity.this, mode, listener);
            }

            @Override
            public void onNaviExit() {
                Log.d(TAG, "onNaviExit");
            }
        });

        mNaviHelper.setTTsPlayer((s, b) -> {
            Log.d(TAG, "tts: " + s);
            return 0;
        });

        mNaviHelper.setRouteGuidanceListener(this, new IWRouteGuidanceListener() {
            @Override
            public void onRouteGuideIconInfoUpdate(IWRouteIconInfo routeIconInfo) {
                if (routeIconInfo != null) {
                    Log.d(TAG, "onRoadGuideTextUpdate   Drawable=: " + routeIconInfo.getIconDrawable()
                            + " Name=: " + routeIconInfo.getIconName());
                }
            }

            @Override
            public void onRouteGuideIconUpdate(Drawable icon) {
                Log.d(TAG, "onRoadGuideTextUpdate   Drawable=: " + icon);
            }

            @Override
            public void onRouteGuideKind(RouteGuideKind routeGuideKind) {
                Log.d(TAG, "onRouteGuideKind: " + routeGuideKind);
            }

            @Override
            public void onRoadGuideTextUpdate(CharSequence charSequence, CharSequence charSequence1) {
                Log.d(TAG, "onRoadGuideTextUpdate   charSequence=: " + charSequence + "   charSequence1 = : " +
                        charSequence1);
            }

            @Override
            public void onRemainDistanceUpdate(CharSequence charSequence) {
                Log.d(TAG, "onRemainDistanceUpdate: charSequence = :" + charSequence);

            }

            @Override
            public void onRemainTimeUpdate(CharSequence charSequence) {
                Log.d(TAG, "onRemainTimeUpdate: charSequence = :" + charSequence);

            }

            @Override
            public void onGpsStatusChange(CharSequence charSequence, Drawable drawable) {
                Log.d(TAG, "onGpsStatusChange: charSequence = :" + charSequence);

            }

            @Override
            public void onRouteFarAway(CharSequence charSequence, Drawable drawable) {
                Log.d(TAG, "onRouteFarAway: charSequence = :" + charSequence);

            }

            @Override
            public void onRoutePlanYawing(CharSequence charSequence, Drawable drawable) {
                Log.d(TAG, "onRoutePlanYawing: charSequence = :" + charSequence);

            }

            @Override
            public void onReRouteComplete() {

            }

            @Override
            public void onArriveDest() {

            }

            @Override
            public void onIndoorEnd(Message msg) {

            }

            @Override
            public void onFinalEnd(Message msg) {

            }

            @Override
            public void onVibrate() {

            }

            @Override
            public void onNaviLocationUpdate() {

            }

            @Override
            public void onRemainTimeUpdate(int remainTime) {
            }

            @Override
            public void onRemainDistanceUpdate(int remainDistance) {
            }

            @Override
            public void onSimpleMapInfoUpdate(WalkSimpleMapInfo info) {

            }
            @Override
            public void onRouteRemainTrafficLightCountUpdate(int remainCount) {
                Log.i(TAG, "onRouteRemainTrafficLightCountUpdate: " + remainCount);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isInBackgroundDrawing && mMultiNaviView != null) {
            mMultiNaviView.onResume();
        }
        mNaviHelper.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isInBackgroundDrawing && mMultiNaviView != null) {
            mMultiNaviView.onPause();
        }
        mNaviHelper.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMultiNaviView != null) {
            mMultiNaviView.onDestroy();
        }
        mNaviHelper.quit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ArCameraView.WALK_AR_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(WalkMultiActivity.this, "没有相机权限,请打开后重试", Toast.LENGTH_SHORT).show();
            } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mNaviHelper.startCameraAndSetMapView(WalkMultiActivity.this);
            }
        }
    }
}
