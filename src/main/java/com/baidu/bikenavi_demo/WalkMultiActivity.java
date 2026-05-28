/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.bikenavi_demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.baidu.bikenavi_demo.func.DEMOFuncBean;
import com.baidu.bikenavi_demo.func.DEMOFuncList;
import com.baidu.mapapi.common.model.traffic.TrafficLightOutData;
import com.baidu.mapapi.common.model.MapCustomDrawOption;
import com.baidu.mapapi.common.model.NaviDrawElementType;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWNaviStatusListener;
import com.baidu.mapapi.walknavi.adapter.IWRouteGuidanceListener;
import com.baidu.mapapi.walknavi.model.IWRouteIconInfo;
import com.baidu.mapapi.walknavi.model.RouteGuideKind;
import com.baidu.mapapi.walknavi.model.WalkNaviDisplayOption;
import com.baidu.mapapi.walknavi.model.WalkSimpleMapInfo;
import com.baidu.platform.comapi.walknavi.WalkNaviModeSwitchListener;
import com.baidu.platform.comapi.walknavi.widget.ArCameraView;
import com.baidu.platform.comapi.wnplatform.mulitmap.IMultiNaviView;
import com.baidu.platform.comapi.wnplatform.mulitmap.MultiNaviViewProvider;
import com.baidu.platform.comapi.wnplatform.model.OverLookingMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class WalkMultiActivity extends Activity {
    private final static String TAG = WalkMultiActivity.class.getSimpleName();
    private WalkNavigateHelper mNaviHelper;
    private Map<Integer, MultiNaviViewProvider.IMultiNaviViewProxy> multiNaviViewMap;

    // 供“投屏”按钮展示离屏渲染截图的画面（与后台服务回调 index 对应）
    private final List<ImageView> mProjectionImageViews = new ArrayList<>();
    private boolean mBackgroundServiceStarted = false;
    private boolean aBool = true;

    private List<DEMOFuncBean> mFuncBeans;
    private PopupWindow mFuncPopup;
    private PopupWindow mTargetPopup;
    private int mFuncTarget = 0; // 0=主实例，1/2/3=多实例
    private final List<MultiNaviViewProvider.IMultiNaviViewProxy> mOrderedMultiProxies = new ArrayList<>();
    private final List<PopupWindow> mProjectionPopups = new ArrayList<>();
    private final List<Integer> mProjectionPosX = new ArrayList<>();
    private final List<Integer> mProjectionPosY = new ArrayList<>();
    private FrameLayout mMapHost;

    public static void showActivity(Context context) {
        context.startActivity(new Intent(context, WalkMultiActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_navi);
        initFuncData();
        View funcList = findViewById(R.id.funcListView);
        if (funcList != null) {
            funcList.setVisibility(View.GONE);
        }
        FrameLayout frameLayout = findViewById(R.id.mapVg);
        mMapHost = frameLayout;

        mNaviHelper = WalkNavigateHelper.getInstance();

        multiNaviViewMap = MultiNaviViewProvider.getInstance().getAllMultiNaviView();

        WalkNaviDisplayOption walkNaviDisplayOption = new WalkNaviDisplayOption()
                .showImageToAr(true) // 是否展示AR图片
                .showCalorieLayoutEnable(true) // 是否展示热量消耗布局
                .showLocationImage(true);  // 是否展示视角切换资源
        mNaviHelper.setWalkNaviDisplayOption(walkNaviDisplayOption);
        try {
            View view = mNaviHelper.onCreate(WalkMultiActivity.this);
            if (view != null) {
                // 添加导航地图
                frameLayout.addView(view);
                mProjectionImageViews.clear();
                mProjectionPopups.clear();
                mProjectionPosX.clear();
                mProjectionPosY.clear();
                if (multiNaviViewMap != null && !multiNaviViewMap.isEmpty()) {
                    mOrderedMultiProxies.clear();
                    List<Integer> tags = new ArrayList<>(multiNaviViewMap.keySet());
                    // 取最新创建的3个实例，避免混入历史残留实例
                    Collections.sort(tags, Collections.reverseOrder());
                    int screenW = getResources().getDisplayMetrics().widthPixels;
                    // 对齐骑行模块：高度更小、宽度可以大点
                    int containerW = (int) (400 * 1.7f);
                    int containerH = (int) (600 * 1.25f);
                    int gap = dp2px(5);
                    int topStart = 200;
                    int left = screenW - containerW - dp2px(10);
                    if (left < dp2px(10)) {
                        left = dp2px(10);
                    }
                    int index = 0;
                    for (Integer tag : tags) {
                        if (index >= 3) {
                            break;
                        }
                        MultiNaviViewProvider.IMultiNaviViewProxy multiNaviView = multiNaviViewMap.get(tag);
                        if (multiNaviView == null) {
                            continue;
                        }
                        multiNaviView.setNaviType(IMultiNaviView.TYPE_NAVI_WALK, tag);
                        // 对齐骑行多实例结构：前台展示时不启用后台绘制模式，避免透明占位
                        multiNaviView.setSupBackgroundDraw(false);
                        multiNaviView.setDefaultLevel(18f);

                        int top = topStart + index * (containerH + gap);
                        FrameLayout.LayoutParams multiLp = new FrameLayout.LayoutParams(containerW, containerH);
                        multiLp.setMargins(left, top, 0, 0);
                        multiNaviView.injectMultiNaviView(frameLayout, multiLp);

                        ImageView projectionIv = new ImageView(this);
                        FrameLayout.LayoutParams projLp = new FrameLayout.LayoutParams(containerW, containerH);
                        int projectionLeft = left - containerW - dp2px(10);
                        if (projectionLeft < dp2px(10)) {
                            projectionLeft = dp2px(10);
                        }
                        projLp.setMargins(projectionLeft, top, 0, 0);
                        projectionIv.setLayoutParams(projLp);
                        projectionIv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        mProjectionImageViews.add(projectionIv);
                        PopupWindow projectionPopup = new PopupWindow(
                                projectionIv,
                                containerW,
                                containerH,
                                false
                        );
                        projectionPopup.setTouchable(false);
                        projectionPopup.setOutsideTouchable(false);
                        mProjectionPopups.add(projectionPopup);
                        mProjectionPosX.add(projectionLeft);
                        mProjectionPosY.add(top);

                        Button btnProjection = new Button(this);
                        btnProjection.setText("投屏");
                        FrameLayout.LayoutParams btnLp = new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.WRAP_CONTENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT
                        );
                        btnLp.setMargins(left + containerW - dp2px(80), top + dp2px(10), 0, 0);
                        frameLayout.addView(btnProjection, btnLp);

                        final MultiNaviViewProvider.IMultiNaviViewProxy currentProxy = multiNaviView;
                        final int projectionIndex = index;
                        btnProjection.setOnClickListener(v -> {
                            Log.d(TAG, "[projection] click index=" + projectionIndex);
                            toggleProjection(projectionIndex);
                        });

                        // 单实例生命周期控制：暂停/销毁/恢复（布局与投屏按钮保持一致）
                        int btnX = left + containerW - dp2px(80);
                        int btnTop = top + dp2px(10);
                        int dy = dp2px(28);

                        Button btnPause = new Button(this);
                        btnPause.setText("暂停");
                        FrameLayout.LayoutParams pauseLp = new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.WRAP_CONTENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT
                        );
                        pauseLp.leftMargin = btnX;
                        pauseLp.topMargin = btnTop + dy;
                        frameLayout.addView(btnPause, pauseLp);
                        btnPause.setOnClickListener(v -> currentProxy.onPause());

                        Button btnDestroy = new Button(this);
                        btnDestroy.setText("销毁");
                        FrameLayout.LayoutParams destroyLp = new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.WRAP_CONTENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT
                        );
                        destroyLp.leftMargin = btnX;
                        destroyLp.topMargin = btnTop + dy * 2;
                        frameLayout.addView(btnDestroy, destroyLp);
                        btnDestroy.setOnClickListener(v -> currentProxy.onDestroy());

                        Button btnResume = new Button(this);
                        btnResume.setText("恢复");
                        FrameLayout.LayoutParams resumeLp = new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.WRAP_CONTENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT
                        );
                        resumeLp.leftMargin = btnX;
                        resumeLp.topMargin = btnTop + dy * 3;
                        frameLayout.addView(btnResume, resumeLp);
                        btnResume.setOnClickListener(v -> currentProxy.onResume());

                        mOrderedMultiProxies.add(multiNaviView);
                        index++;
                    }
                    addFuncFloatingButton(frameLayout);
                }

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

    private void ensureBackgroundProjectionStarted() {
        // 把后台离屏截图分别回填到三个投屏画面上
        Log.d(TAG, "[projection] ensure start, viewCount=" + mProjectionImageViews.size()
                + ", popupCount=" + mProjectionPopups.size());
        BackgroundNaviService.setUseBikeNavi(false);
        for (int i = 0; i < mProjectionImageViews.size(); i++) {
            final int index = i;
            final ImageView projectionIv = mProjectionImageViews.get(i);
            BackgroundNaviService.setScreenShotCallback(i, drawable -> {
                int size = -1;
                if (drawable instanceof BitmapDrawable && ((BitmapDrawable) drawable).getBitmap() != null) {
                    size = ((BitmapDrawable) drawable).getBitmap().getByteCount();
                }
                Log.d(TAG, "[projection] screenshot callback index=" + index + ", byteCount=" + size);
                projectionIv.setBackground(drawable);
            });
            Log.d(TAG, "[projection] callback bound index=" + i);
        }
        startService(new Intent(this, BackgroundNaviService.class));
        mBackgroundServiceStarted = true;
        Log.d(TAG, "[projection] service started");
    }

    private void showProjection(int projectionIndex) {
        for (int i = 0; i < mProjectionImageViews.size(); i++) {
            mProjectionImageViews.get(i).setVisibility(i == projectionIndex ? View.VISIBLE : View.GONE);
        }
    }

    private void hideAllProjection() {
        for (PopupWindow popup : mProjectionPopups) {
            if (popup != null && popup.isShowing()) {
                popup.dismiss();
            }
        }
    }

    private void toggleProjection(int projectionIndex) {
        if (projectionIndex < 0
                || projectionIndex >= mProjectionImageViews.size()
                || projectionIndex >= mProjectionPopups.size()
                || projectionIndex >= mProjectionPosX.size()
                || projectionIndex >= mProjectionPosY.size()) {
            Log.w(TAG, "[projection] invalid index=" + projectionIndex
                    + ", viewCount=" + mProjectionImageViews.size()
                    + ", popupCount=" + mProjectionPopups.size());
            return;
        }
        PopupWindow popup = mProjectionPopups.get(projectionIndex);
        if (popup != null && popup.isShowing()) {
            // 二次点击只关闭当前实例投屏，不影响主导航/其它实例
            Log.d(TAG, "[projection] dismiss popup index=" + projectionIndex);
            popup.dismiss();
            return;
        }
        ensureBackgroundProjectionStarted();
        if (mMapHost != null && popup != null) {
            Log.d(TAG, "[projection] show popup index=" + projectionIndex
                    + ", x=" + mProjectionPosX.get(projectionIndex)
                    + ", y=" + mProjectionPosY.get(projectionIndex));
            popup.showAtLocation(
                    mMapHost,
                    Gravity.NO_GRAVITY,
                    mProjectionPosX.get(projectionIndex),
                    mProjectionPosY.get(projectionIndex)
            );
            Log.d(TAG, "[projection] popup isShowing=" + popup.isShowing());
        } else {
            Log.w(TAG, "[projection] cannot show popup, mapHost=" + (mMapHost != null)
                    + ", popup=" + (popup != null));
        }
    }

    private void initFuncData() {
        List<DEMOFuncBean> beans = new ArrayList<>();
        List<DEMOFuncBean.DEMOFuncBeanChild> childExt = new ArrayList<>();
        List<DEMOFuncBean.DEMOFuncBeanChild> child = new ArrayList<>();
        child.add(new DEMOFuncBean.DEMOFuncBeanChild("骑行朝向"));
        child.add(new DEMOFuncBean.DEMOFuncBeanChild("路况开关"));
        child.add(new DEMOFuncBean.DEMOFuncBeanChild("路线全览"));
        child.add(new DEMOFuncBean.DEMOFuncBeanChild("主地图默认地图等级"));
        child.add(new DEMOFuncBean.DEMOFuncBeanChild("底图元素隐藏"));
        child.add(new DEMOFuncBean.DEMOFuncBeanChild("模拟导航速度"));
        child.add(new DEMOFuncBean.DEMOFuncBeanChild("浏览态导航态"));
        child.add(new DEMOFuncBean.DEMOFuncBeanChild("车标偏移"));
        child.add(new DEMOFuncBean.DEMOFuncBeanChild("车标路线等资源替换"));
        child.add(new DEMOFuncBean.DEMOFuncBeanChild("自定义车标"));
        child.add(new DEMOFuncBean.DEMOFuncBeanChild("主图车标罗盘路线宽度缩放"));
        child.add(new DEMOFuncBean.DEMOFuncBeanChild("路线名称气泡样式修改"));
        beans.add(new DEMOFuncBean(child, "步行主实例"));

        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例车标罗盘路线宽度缩放"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例车标偏移"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例setMapDpiScale"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例默认地图等级"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例是否显示建筑物"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例是否显示POI"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例底图元素隐藏"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例车标路线等资源替换"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例自定义车标"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例骑行朝向"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例设置路线全览"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例2D/3D视角"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例浏览态导航态"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例路线名称气泡样式修改"));
        beans.add(new DEMOFuncBean(childExt, "步行多实例"));
        mFuncBeans = beans;
    }

    private void addFuncFloatingButton(FrameLayout host) {
        Button btn = new Button(this);
        btn.setText("多实例自定义样式");
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.topMargin = dp2px(10);
        host.addView(btn, lp);
        btn.setOnClickListener(v -> showTargetPopup(btn));
    }

    private void showTargetPopup(View anchor) {
        if (mTargetPopup != null && mTargetPopup.isShowing()) {
            mTargetPopup.dismiss();
        }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp2px(8);
        root.setPadding(pad, pad, pad, pad);

        String[] items = new String[]{"步行主实例", "步行多实例1", "步行多实例2", "步行多实例3"};
        for (int i = 0; i < items.length; i++) {
            int target = i; // 0..3
            Button b = new Button(this);
            b.setText(items[i]);
            root.addView(b);
            b.setOnClickListener(v -> {
                mFuncTarget = target;
                if (mTargetPopup != null) {
                    mTargetPopup.dismiss();
                }
                List<DEMOFuncBean> beansToShow = new ArrayList<>();
                if (mFuncBeans != null && !mFuncBeans.isEmpty()) {
                    if (mFuncTarget == 0) {
                        if (mFuncBeans.size() > 0) {
                            beansToShow.add(mFuncBeans.get(0));
                        }
                    } else {
                        if (mFuncBeans.size() > 1) {
                            beansToShow.add(mFuncBeans.get(1));
                        }
                    }
                }
                showFuncPopup(anchor, beansToShow);
            });
        }

        mTargetPopup = new PopupWindow(
                root,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        mTargetPopup.setOutsideTouchable(true);
        mTargetPopup.setTouchable(true);
        mTargetPopup.showAsDropDown(anchor);
    }

    private void showFuncPopup(View anchor, List<DEMOFuncBean> beansToShow) {
        if (mFuncPopup != null && mFuncPopup.isShowing()) {
            mFuncPopup.dismiss();
        }
        DEMOFuncList funcList = new DEMOFuncList(this);
        funcList.setData(beansToShow != null ? beansToShow : new ArrayList<>());
        funcList.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            String name = funcList.getData().get(groupPosition).childList.get(childPosition).getFuncName();
            handleFuncClick(name);
            if (mFuncPopup != null) {
                mFuncPopup.dismiss();
            }
            return true;
        });

        mFuncPopup = new PopupWindow(
                funcList,
                dp2px(320),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        mFuncPopup.setOutsideTouchable(true);
        mFuncPopup.setTouchable(true);
        mFuncPopup.showAsDropDown(anchor);
    }

    private List<MultiNaviViewProvider.IMultiNaviViewProxy> getTargetProxies() {
        if (mOrderedMultiProxies == null || mOrderedMultiProxies.isEmpty()) {
            return new ArrayList<>();
        }
        if (mFuncTarget <= 0) {
            // 主实例：如果操作属于“多实例样式”，则作用于所有多实例窗
            return new ArrayList<>(mOrderedMultiProxies);
        }
        int idx = mFuncTarget - 1;
        if (idx >= 0 && idx < mOrderedMultiProxies.size()) {
            return Collections.singletonList(mOrderedMultiProxies.get(idx));
        }
        return new ArrayList<>(mOrderedMultiProxies);
    }

    private void handleFuncClick(String funcName) {
        switch (funcName) {
            case "模拟导航速度":
                // 步行也支持模拟速度调整（与现有代码一致）
                mNaviHelper.setSimulateNaviSpeed(15);
                break;

            case "多实例车标罗盘路线宽度缩放": {
                MapCustomDrawOption mapCustomDrawOption = new MapCustomDrawOption();
                mapCustomDrawOption.setCarPointCustomScale(2f);
                mapCustomDrawOption.setCompassCustomScale(2f);
                mapCustomDrawOption.setRouteCustomWidth(10);
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                    proxy.updateExtMapRenderCustomDrawOption(mapCustomDrawOption);
                }
            }
                break;

            case "多实例默认地图等级":
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                    proxy.setDefaultLevel(20);
                }
                break;

            case "多实例是否显示建筑物":
                aBool = !aBool;
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                    proxy.setBuildingsEnabled(aBool);
                }
                break;

            case "多实例底图元素隐藏": {
                List<NaviDrawElementType> elements = new ArrayList<>();
                elements.add(NaviDrawElementType.ROAD_NAME_POP);
                elements.add(NaviDrawElementType.CAR_TO_END_RED_LINE);
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                    proxy.setNaviDrawElementsShow(false, elements);
                }
            }
                break;

            case "多实例setMapDpiScale":
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                    proxy.setMapDpiScale(0.5f, 0.5f);
                }
                break;

            case "多实例是否显示POI":
                aBool = !aBool;
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                    proxy.showPoiMark(aBool);
                }
                break;

            case "多实例车标路线等资源替换": {
                // 这里复用骑行模块的资源，保证能跑通（步行细粒度资源可后续再替换）
                MapCustomDrawOption mapCustomDrawOption = new MapCustomDrawOption();
                mapCustomDrawOption.setCompassCustomBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.bn_start_blue));
                mapCustomDrawOption.setEndPointCustomBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.bn_dest_blue));
                mapCustomDrawOption.setCarPointCustomRes(
                        BitmapFactory.decodeResource(getResources(), R.drawable.wsdk_drawable_rg_ic_car3d));
                mapCustomDrawOption.setRouteNormalCustomBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.wsdk_drawable_rg_ic_north_walk_bike2d));
                mapCustomDrawOption.setRoutePassedCustomBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.wb_voice_view_close));
                mapCustomDrawOption.setRouteCustomWidth(20);
                mapCustomDrawOption.setCarPointCustomScale(2);
                mapCustomDrawOption.setEndPointCustomScale(2);
                mapCustomDrawOption.setCompassCustomScale(2);
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                    proxy.updateExtMapRenderCustomDrawOption(mapCustomDrawOption);
                }
            }
                break;
            case "多实例自定义车标": {
                // 仅替换车标点位图标（其它不动）
                MapCustomDrawOption mapCustomDrawOption = new MapCustomDrawOption();
                mapCustomDrawOption.setCarPointCustomRes(
                        BitmapFactory.decodeResource(getResources(), R.drawable.pubtravel_home_icon_logo));
                mapCustomDrawOption.setCarPointCustomScale(0.05f);
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                    proxy.updateExtMapRenderCustomDrawOption(mapCustomDrawOption);
                }
                break;
            }

            case "多实例骑行朝向":
                aBool = !aBool;
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                    proxy.setRotateMode(aBool
                            ? IMultiNaviView.Map_Rotate_Mode.Map_Rotate_Route
                            : IMultiNaviView.Map_Rotate_Mode.Map_Rotate_North);
                }
                break;

            case "多实例设置路线全览":
                aBool = !aBool;
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                    if (aBool) {
                        proxy.setNaviMapMargin(0, 0, 0, 0);
                    }
                    proxy.setNaviMapViewAllStatus(aBool);
                }
                break;

            case "多实例2D/3D视角":
                aBool = !aBool;
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                    proxy.setDefaultOverlooking(aBool ? OverLookingMode.OverLooking_3D : OverLookingMode.OverLooking_2D);
                }
                break;

            case "多实例浏览态导航态":
                aBool = !aBool;
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                    proxy.setExtBrowseStatus(aBool);
                }
                break;

            case "多实例路线名称气泡样式修改": {
                MapCustomDrawOption mapCustomDrawOption = new MapCustomDrawOption();
                mapCustomDrawOption.setRouteCustomWidth(1);
                mapCustomDrawOption.setRouteNameFontSize(50);
                mapCustomDrawOption.setRouteNameFontColor(Color.RED);
                mapCustomDrawOption.setRouteNamePopFontSize(50);
                mapCustomDrawOption.setRouteNamePopFontColor(Color.RED);
                mapCustomDrawOption.setRouteNamePopBgColor(Color.YELLOW);
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                    proxy.updateExtMapRenderCustomDrawOption(mapCustomDrawOption);
                }
            }
                break;

            // 主实例/不适配步行的功能，先提示避免误以为“没反应”
            case "骑行朝向":
            case "路况开关":
            case "路线全览":
            case "主地图默认地图等级":
            case "底图元素隐藏":
            case "浏览态导航态":
            case "车标偏移":
            case "车标路线等资源替换":
            case "主图车标罗盘路线宽度缩放":
            case "路线名称气泡样式修改":
            case "多实例车标偏移":
                toast("该功能当前仅多实例样式可用");
                break;
        }
    }

    private int dp2px(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
            public void onTrafficLightOutDataUpdate(TrafficLightOutData trafficLightOutData) {

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
        if (multiNaviViewMap != null) {
            for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : multiNaviViewMap.values()) {
                proxy.onResume();
            }
        }
        mNaviHelper.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (multiNaviViewMap != null) {
            for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : multiNaviViewMap.values()) {
                proxy.onPause();
            }
        }
        mNaviHelper.pause();
    }

    @Override
    protected void onDestroy() {
        try {
            // 1. 退出导航
            if (mNaviHelper != null) {
                mNaviHelper.quit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. 销毁多地图视图
        if (multiNaviViewMap != null) {
            for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : multiNaviViewMap.values()) {
                try {
                    if (proxy != null) {
                        proxy.onDestroy();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 3. 停止后台服务
        if (mBackgroundServiceStarted) {
            stopService(new Intent(this, BackgroundNaviService.class));
            mBackgroundServiceStarted = false;
        }

        hideAllProjection();
        super.onDestroy();
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
