/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.bikenavi_demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.baidu.bikenavi_demo.func.DEMOFuncBean;
import com.baidu.bikenavi_demo.func.DEMOFuncList;
import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.model.BikeNaviDisplayOption;
import com.baidu.mapapi.bikenavi.model.BikeNaviRotateMode;
import com.baidu.mapapi.bikenavi.params.BikeNaviLaunchParam;
import com.baidu.mapapi.common.eaglemap.EagleMiniMap;
import com.baidu.mapapi.common.model.MapCustomDrawOption;
import com.baidu.mapapi.common.model.NaviDrawElementType;
import com.baidu.platform.comapi.wnplatform.model.OverLookingMode;
import com.baidu.platform.comapi.wnplatform.mulitmap.IMultiNaviView;
import com.baidu.platform.comapi.wnplatform.mulitmap.MultiNaviViewProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BikeNaviMultiActivity extends Activity {
    private static final String TAG = BikeNaviMultiActivity.class.getSimpleName();
    private BikeNavigateHelper mNaviHelper;
    private boolean aBool = true;
    BikeNaviLaunchParam param;

    /**
     * 多实例地图view集合
     */
    private Map<Integer, MultiNaviViewProvider.IMultiNaviViewProxy> multiNaviViewMap;
    private final List<ImageView> mProjectionImageViews = new ArrayList<>();
    private List<DEMOFuncBean> mFuncBeans;
    private PopupWindow mFuncPopup;
    private PopupWindow mTargetPopup;
    // 0:骑行主实例；1/2/3：骑行多实例1/2/3
    private int mFuncTarget = 0;
    private final List<MultiNaviViewProvider.IMultiNaviViewProxy> mOrderedMultiProxies = new ArrayList<>();
    private final List<PopupWindow> mProjectionPopups = new ArrayList<>();
    private final List<Integer> mProjectionPosX = new ArrayList<>();
    private final List<Integer> mProjectionPosY = new ArrayList<>();
    private FrameLayout mMapHost;
    private Button mLaneModeBtn;
    private PopupWindow mLaneModePopup;
    // 0: 主实例；1/2/3: 多实例1/2/3
    private final boolean[] mLaneModeStates = new boolean[4];
    // 1/2/3 -> 对应的多实例 proxy，避免按列表下标映射错位
    private final Map<Integer, MultiNaviViewProvider.IMultiNaviViewProxy> mLaneTargetProxyMap = new java.util.HashMap<>();
    private FrameLayout fmMiniMap;
    private EagleMiniMap eagleMiniMap;

    public static void showActivity(Context context) {
        context.startActivity(new Intent(context, BikeNaviMultiActivity.class));
    }

    @Override
    protected void onDestroy() {
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

        hideAllProjection();
        if (mLaneModePopup != null && mLaneModePopup.isShowing()) {
            mLaneModePopup.dismiss();
        }
        try {
            // 1. 退出导航
            if (mNaviHelper != null) {
                mNaviHelper.quit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNaviHelper.resume();
        if (multiNaviViewMap != null) {
            for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : multiNaviViewMap.values()) {
                proxy.onResume();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNaviHelper.pause();
        if (multiNaviViewMap != null) {
            for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : multiNaviViewMap.values()) {
                proxy.onPause();
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_navi);
        // 地图渲染层可能覆盖普通View，这里用弹出层承载功能列表
        initFuncData();
        FrameLayout frameLayout = findViewById(R.id.mapVg);
        fmMiniMap = findViewById(R.id.fm_mini_map);
        mMapHost = frameLayout;
        View xmlFuncList = findViewById(R.id.funcListView);
        if (xmlFuncList != null) {
            xmlFuncList.setVisibility(View.GONE);
        }
        mNaviHelper = BikeNavigateHelper.getInstance();

        BikeNaviDisplayOption bikeNaviDisplayOption = new BikeNaviDisplayOption()
                .showSpeedLayout(true) // 是否展示速度切换布局
                .showTopGuideLayout(true)  // 是否展示顶部引导布局
                .showLocationImage(true) // 是否展示视角切换资源
                .runInFragment(false);
        mNaviHelper.setBikeNaviDisplayOption(bikeNaviDisplayOption);
        View view = mNaviHelper.onCreate(BikeNaviMultiActivity.this);
        if (view != null) {
            frameLayout.addView(view);
        }
        multiNaviViewMap = MultiNaviViewProvider.getInstance().getAllMultiNaviView();
        mOrderedMultiProxies.clear();
        mLaneTargetProxyMap.clear();
        mProjectionImageViews.clear();
        mProjectionPopups.clear();
        mProjectionPosX.clear();
        mProjectionPosY.clear();
        if (multiNaviViewMap != null && !multiNaviViewMap.isEmpty()) {
            List<Integer> tags = new ArrayList<>(multiNaviViewMap.keySet());
            Collections.sort(tags, Collections.reverseOrder());
            int screenW = getResources().getDisplayMetrics().widthPixels;
            // 用户侧尺寸偏好：高度再小些，宽度可以大点
            int containerW = (int) (400 * 1.7f); // 宽度略放大
            int containerH = (int) (600 * 1.25f); // 高度缩小
            int gap = dp2px(5); // 小窗间隔，避免重叠但尽量紧凑
            int topStart = 200; // 与原始代码的垂直起点保持一致
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
                multiNaviView.setNaviType(IMultiNaviView.TYPE_NAVI_RIDE, tag); // 设置导航类型

                int top = topStart + index * (containerH + gap);
                FrameLayout.LayoutParams multiLp = new FrameLayout.LayoutParams(containerW, containerH);
                multiLp.leftMargin = left;
                multiLp.topMargin = top;
                multiNaviView.injectMultiNaviView(frameLayout, multiLp); // 注入多实例地图view

                ImageView projectionIv = new ImageView(this);
                FrameLayout.LayoutParams projLp = new FrameLayout.LayoutParams(containerW, containerH);
                int projectionLeft = left - containerW - dp2px(10);
                if (projectionLeft < dp2px(10)) {
                    projectionLeft = dp2px(10);
                }
                projLp.leftMargin = projectionLeft;
                projLp.topMargin = top;
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

                mOrderedMultiProxies.add(multiNaviView);
                // 固定当前页面展示顺序：多实例1/2/3
                mLaneTargetProxyMap.put(index + 1, multiNaviView);
                index++;
            }
        }
        addFuncFloatingButton(frameLayout);
        addLaneModeToggleButton(frameLayout);
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
        mNaviHelper.enterLaneNaviMode(true);
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
        child.add(new DEMOFuncBean.DEMOFuncBeanChild("添加鹰眼小图"));
        beans.add(new DEMOFuncBean(child, "骑行主实例"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例车标罗盘路线宽度缩放"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例车标偏移"));
        childExt.add(new DEMOFuncBean.DEMOFuncBeanChild("多实例截屏"));
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
        beans.add(new DEMOFuncBean(childExt, "骑行多实例"));
        mFuncBeans = beans;
    }

    private void addFuncFloatingButton(FrameLayout host) {
        Button btn = new Button(this);
        btn.setText("导航功能列表");
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        // 页面中间顶左的近似实现：顶部居中（不遮挡投屏按钮）
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.topMargin = dp2px(10);
        host.addView(btn, lp);
        btn.setOnClickListener(v -> showTargetPopup(btn));
    }

    private void addLaneModeToggleButton(FrameLayout host) {
        mLaneModeBtn = new Button(this);
        mLaneModeBtn.setText("切换导航模式");
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.topMargin = dp2px(56);
        host.addView(mLaneModeBtn, lp);
        mLaneModeBtn.setOnClickListener(v -> showLaneModePopup(v));
    }

    private void showLaneModePopup(View anchor) {
        if (mLaneModePopup != null && mLaneModePopup.isShowing()) {
            mLaneModePopup.dismiss();
        }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp2px(8);
        root.setPadding(pad, pad, pad, pad);

        String[] items = new String[]{"主进程", "多实例1", "多实例2", "多实例3"};
        for (int i = 0; i < items.length; i++) {
            final int target = i;
            Button b = new Button(this);
            b.setText(getLaneModeItemText(items[target], target));
            root.addView(b);
            b.setOnClickListener(v -> {
                toggleSingleLaneMode(target);
                b.setText(getLaneModeItemText(items[target], target));
            });
        }

        mLaneModePopup = new PopupWindow(
                root,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        mLaneModePopup.setOutsideTouchable(true);
        mLaneModePopup.setTouchable(true);
        mLaneModePopup.showAsDropDown(anchor);
    }

    private String getLaneModeItemText(String itemName, int target) {
        return itemName + (mLaneModeStates[target] ? "（退出车道级）" : "（进入车道级）");
    }

    private void toggleSingleLaneMode(int target) {
        boolean enableLaneMode = !mLaneModeStates[target];
        if (target == 0) {
            // 主实例：车道级与全览/2D3D互斥，切换前关闭全览
            mNaviHelper.setViewAllStatus(false);
            mNaviHelper.enterLaneNaviMode(enableLaneMode);
            mLaneModeStates[target] = enableLaneMode;
            toast("主进程已" + (enableLaneMode ? "进入" : "退出") + "车道级模式");
            return;
        }
        int multiIndex = target - 1;
        if (multiIndex < 0 || multiIndex >= mOrderedMultiProxies.size()) {
            toast("多实例" + target + "不存在");
            return;
        }
        MultiNaviViewProvider.IMultiNaviViewProxy proxy = mLaneTargetProxyMap.get(target);
        if (proxy == null) {
            // 兜底：兼容极端情况下映射表缺失
            proxy = mOrderedMultiProxies.get(multiIndex);
        }
        if (proxy == null) {
            toast("多实例" + target + "不可用");
            return;
        }
        // 多实例：车道级与全览/2D3D互斥，切换前关闭全览
        proxy.setNaviMapViewAllStatus(false);
        proxy.enterLaneNaviMode(enableLaneMode);
        mLaneModeStates[target] = enableLaneMode;
        Log.d(TAG, "toggleLaneMode target=" + target + ", tag=" + proxy.getViewTag()
                + ", enable=" + enableLaneMode);
        toast("多实例" + target + "已" + (enableLaneMode ? "进入" : "退出") + "车道级模式");
    }

    private void showTargetPopup(View anchor) {
        if (mTargetPopup != null && mTargetPopup.isShowing()) {
            mTargetPopup.dismiss();
        }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp2px(8);
        root.setPadding(pad, pad, pad, pad);

        String[] items = new String[]{"骑行主实例", "骑行多实例1", "骑行多实例2", "骑行多实例3"};
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
                    // mFuncBeans[0]=骑行主实例；mFuncBeans[1]=骑行多实例
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
            case "骑行朝向":
                aBool = !aBool;
                BikeNaviRotateMode mode = BikeNaviRotateMode.EN_Rotate_Mode_Car;
                if (aBool) {
                    mode = BikeNaviRotateMode.EN_Rotate_Mode_Map;
                }
                mNaviHelper.setRotateMode(mode);
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
            case "主图车标罗盘路线宽度缩放": {
                BikeNaviDisplayOption.BikeMapCustomDrawOption bikeNaviDisplayOption = new BikeNaviDisplayOption.BikeMapCustomDrawOption();
                bikeNaviDisplayOption.setCarPointCustomScale(2f);
                bikeNaviDisplayOption.setCompassCustomScale(2f);
                mNaviHelper.updateMapRenderCustomDrawOption(bikeNaviDisplayOption);
            }
            break;
            case "路况开关":
                aBool = !aBool;
                mNaviHelper.getNaviMap().getMap().setTrafficEnabled(aBool);
                break;
            case "路线全览":
                aBool = !aBool;
                mNaviHelper.setRouteMargin(100, 600, 0, 0);
                mNaviHelper.setViewAllStatus(aBool);
                break;
            case "模拟导航速度":
                mNaviHelper.setSimulateNaviSpeed(15); // m/s
                break;
            case "添加鹰眼小图":
                aBool = !aBool;
                createMiniMap(aBool);
                break;
            case "浏览态导航态":
                aBool = !aBool;
                mNaviHelper.setBrowseStatus(aBool);
                break;
            case "多实例默认地图等级":
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                        proxy.setDefaultLevel(10);
                }
                break;
            case "主地图默认地图等级":
                mNaviHelper.setDefaultNaviMapScale(10);
                break;
            case "多实例是否显示建筑物":
                aBool = !aBool;
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                        proxy.setBuildingsEnabled(aBool);
                }
                break;
            case "底图元素隐藏": {
                List<NaviDrawElementType> elements = new ArrayList<>();
                elements.add(NaviDrawElementType.ROAD_NAME_POP);
                elements.add(NaviDrawElementType.CAR_TO_END_RED_LINE);
                mNaviHelper.setNaviDrawElementsShow(false, elements);
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
            case "车标偏移":
                BikeNavigateHelper.getInstance().setCarPosOffset(-0.3f, 0.3f, 0, 0);
                break;
            case "多实例车标偏移":
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                        int tag = proxy.getViewTag();
                        BikeNavigateHelper.getInstance().setCarPosOffset(0.3f, 0.3f, 1, tag);
                }
                break;
            case "多实例车标路线等资源替换": {
                MapCustomDrawOption mapCustomDrawOption = new MapCustomDrawOption();
                mapCustomDrawOption.setCompassCustomBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.bn_start_blue));
                mapCustomDrawOption.setEndPointCustomBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.bn_dest_blue)
                );
                mapCustomDrawOption.setCarPointCustomRes(
                        BitmapFactory.decodeResource(getResources(), R.drawable.wsdk_drawable_rg_ic_car3d)
                );
                mapCustomDrawOption.setRouteNormalCustomBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.wsdk_drawable_rg_ic_north_walk_bike2d)
                );
                mapCustomDrawOption.setRoutePassedCustomBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.wb_voice_view_close)
                );
                mapCustomDrawOption.setRouteCustomWidth(20);
                mapCustomDrawOption.setCarPointCustomScale(2);
                mapCustomDrawOption.setEndPointCustomScale(2);
                mapCustomDrawOption.setCompassCustomScale(2);
                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                        proxy.updateExtMapRenderCustomDrawOption(mapCustomDrawOption);
                }
            }
            break;
            case "车标路线等资源替换": {
                BikeNaviDisplayOption.BikeMapCustomDrawOption mapCustomDrawOption = new BikeNaviDisplayOption.BikeMapCustomDrawOption();
                mapCustomDrawOption.setCompassCustomBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.bn_start_blue));
                mapCustomDrawOption.setEndPointCustomBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.bn_dest_blue)
                );
                mapCustomDrawOption.setCarPointCustomRes(
                        BitmapFactory.decodeResource(getResources(), R.drawable.wsdk_drawable_rg_ic_car3d)
                );
                mapCustomDrawOption.setRouteNormalCustomBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.wsdk_drawable_rg_ic_north_walk_bike2d)
                );
                mapCustomDrawOption.setRoutePassedCustomBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.wb_voice_view_close)
                );
                mapCustomDrawOption.setRouteCustomWidth(20);
                mapCustomDrawOption.setCarPointCustomScale(2);
                mapCustomDrawOption.setEndPointCustomScale(2);
                mapCustomDrawOption.setCompassCustomScale(2);
                mNaviHelper.updateMapRenderCustomDrawOption(mapCustomDrawOption);
            }
            break;
            case "多实例自定义车标": {
                // 仅替换车标点位图标（其它不动）
                MapCustomDrawOption mapCustomDrawOption = new MapCustomDrawOption();
                mapCustomDrawOption.setCarPointCustomRes(
                        BitmapFactory.decodeResource(getResources(), R.drawable.wn_dest_white));
                mapCustomDrawOption.setCarPointCustomScale(0.5f);

                for (MultiNaviViewProvider.IMultiNaviViewProxy proxy : getTargetProxies()) {
                    proxy.updateExtMapRenderCustomDrawOption(mapCustomDrawOption);
                }
                break;
            }
            case "自定义车标": {
                // 仅替换车标点位图标（其它不动）
                BikeNaviDisplayOption.BikeMapCustomDrawOption builder = new BikeNaviDisplayOption.BikeMapCustomDrawOption();
                builder.setCarPointCustomRes(
                        BitmapFactory.decodeResource(getResources(), R.drawable.wn_dest_white));
                BikeNavigateHelper.getInstance().updateMapRenderCustomDrawOption(builder);
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
            case "多实例截屏":
                getTargetProxies().get(0).screenshot(new IMultiNaviView.SnapshotReadyCallback() {
                    @Override
                    public void onSnapshotReady(Bitmap snapshot) {
                        // 保存图片到本地
                        File file = new File(getExternalCacheDir(), "screenshot" + System.currentTimeMillis() + ".png");
                        try {
                            FileOutputStream fos = new FileOutputStream(file);
                            snapshot.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            fos.flush();
                            fos.close();
                            toast("截图已保存到" + file.getPath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, true);
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
            case "路线名称气泡样式修改": {
                BikeNaviDisplayOption.BikeMapCustomDrawOption mapCustomDrawOption = new BikeNaviDisplayOption.BikeMapCustomDrawOption();
                mapCustomDrawOption.setRouteCustomWidth(1);
                mapCustomDrawOption.setCarPointCustomScale(1);
                mapCustomDrawOption.setEndPointCustomScale(1);
                mapCustomDrawOption.setCompassCustomScale(1);
                mapCustomDrawOption.setRouteNameFontSize(50);
                int color1 = Color.parseColor("#FF0000");
                mapCustomDrawOption.setRouteNameFontColor(color1);
                mapCustomDrawOption.setRouteNamePopFontSize(50);
                mapCustomDrawOption.setRouteNamePopFontColor(Color.YELLOW);
                int colorRgb = Color.rgb(255, 0, 0);
                mapCustomDrawOption.setRouteNamePopBgColor(colorRgb);
                mNaviHelper.updateMapRenderCustomDrawOption(mapCustomDrawOption);
            }
            break;
        }
    }

    private void createMiniMap(boolean isShow) {
        if (isShow) {
            if (eagleMiniMap == null) {
                eagleMiniMap = new EagleMiniMap(this);
                fmMiniMap.addView(eagleMiniMap);
            }
            eagleMiniMap.setVisibility(View.VISIBLE);
        } else if (eagleMiniMap != null) {
            eagleMiniMap.setVisibility(View.GONE);
        }
    }

    private int dp2px(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void hideAllProjection() {
        for (PopupWindow popup : mProjectionPopups) {
            if (popup != null && popup.isShowing()) {
                popup.dismiss();
            }
        }
    }

}
