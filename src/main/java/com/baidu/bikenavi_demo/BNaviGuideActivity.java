/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.bikenavi_demo;

import com.baidu.bikenavi_demo.floating.ComObservable;
import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBNaviStatusListener;
import com.baidu.mapapi.bikenavi.adapter.IBRouteGuidanceListener;
import com.baidu.mapapi.bikenavi.adapter.IBTTSPlayer;
import com.baidu.mapapi.bikenavi.model.BikeNaviDisplayOption;
import com.baidu.mapapi.bikenavi.model.BikeRouteDetailInfo;
import com.baidu.mapapi.bikenavi.model.IBRouteIconInfo;
import com.baidu.mapapi.bikenavi.params.BikeNaviLaunchParam;
import com.baidu.mapapi.map.LogoPosition;
import com.baidu.mapapi.tts.WNTTSManager;
import com.baidu.mapapi.walknavi.model.RouteGuideKind;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * 目前骑行导航支持四种自定义布局，使用时需指定view的类型以及tag，view的ID可自定义（避免与其它id重复），具体如下：
 *      1.setTopGuideLayout 顶部引导布局
 *          GPS提示布局：LinearLayout	 tag：BMSDK_LAYOUT_GPS
 *          GPS信号弱文案：TextView	tag：BMSDK_TEXT_BGPS_WEAK
 *          GPS信号提示引导（请步行到开阔地带）： TextView	tag：BMSDK_TEXT_BGPS_HINT
 *          引导图片：ImageView	tag：BMSDK_IMAGE_BGUIDANCE_ICON
 *          引导文字：TextView	tag：BMSDK_TEXT_BGUIDANCE_TV
 *      2.setSpeedLayout 速度布局
 *          速度数值： TextView	tag：BMSDK_TEXT_SPEED
 *          速度单位： TextView	tag：BMSDK_TEXT_SPEED_UNIT
 *      3.setCustomBottomView 最底部自定义view，自由设计，位于页面最底部，会被添加到sync_view中
 *      4.setBottomSettingLayout 底部布局
 *          左下角退出按钮： ImageView  tag：BMSDK_IMAGE_QUIT_ICON
 *          中间查看全览： TextView	tag：BMSDK_TEXT_LOOKOVER
 *          中间剩余信息：TextView	tag：BMSDK_TEXT_REMAIN
 *
 *     具体使用可参考custom_speed_layout布局文件，源码布局文件可参考wsdk_layout_bikenavi_ui_layout
 */
public class BNaviGuideActivity extends Activity {

    private final static String TAG = BNaviGuideActivity.class.getSimpleName();

    private BikeNavigateHelper mNaviHelper;
    public static boolean mIsRunning = false;
    BikeNaviLaunchParam param;

    private boolean isShowLight = false;

    /**
     * 导航监听, 注意生命周期需要用户维护
     * 注: 简易实现, 用户可以通过自己形式去更好的实现
     */
    private static final List<INaviListener> NAVI_LISTENERS = new ArrayList<>();

    public static void showActivity(Context context) {
        context.startActivity(new Intent(context, BNaviGuideActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mNaviHelper.isNaviStanding()) {
            mNaviHelper.quit();
        } else {
            mNaviHelper.onDestroy(false);
        }
        for (INaviListener listener : NAVI_LISTENERS) {
            listener.quit();
        }
        mIsRunning = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNaviHelper.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNaviHelper.pause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsRunning = true;
        mNaviHelper = BikeNavigateHelper.getInstance();
        BikeNaviDisplayOption bikeNaviDisplayOption = new BikeNaviDisplayOption()
                .showSpeedLayout(true) // 是否展示速度切换布局
                .showTopGuideLayout(true)  // 是否展示顶部引导布局
//                .setSpeedLayout(R.layout.custom_speed_layout)//展示自定义速度布局
                .showLocationImage(true);  // 是否展示视角切换资源

        mNaviHelper.setBikeNaviDisplayOption(bikeNaviDisplayOption);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setLayoutParams(params);
        View view = mNaviHelper.onCreate(BNaviGuideActivity.this);
        if (view != null) {
            frameLayout.addView(view);
            setContentView(frameLayout);
        }

        mNaviHelper.setBikeNaviStatusListener(new IBNaviStatusListener() {
            @Override
            public void onNaviExit() {
                Log.d(TAG, "onNaviExit");
            }
        });

        mNaviHelper.setTTsPlayer(new IBTTSPlayer() {
            @Override
            public int playTTSText(String s, boolean b) {
                Log.d("tts", s);
                return 0;
            }
        });
        if (BNaviMainActivity.isFakeNavi) {
            mNaviHelper.startBikeNavi(BNaviGuideActivity.this, BikeNavigateHelper.NaviMode.FakeNavi);
            mNaviHelper.setSimulateNaviSpeed(5);
        } else {
            mNaviHelper.startBikeNavi(BNaviGuideActivity.this);
        }
    }

    public static void addBikeNaviListener(INaviListener listener) {
        NAVI_LISTENERS.add(listener);
    }

    public static void removeBikeNaviListener(INaviListener listener) {
        NAVI_LISTENERS.remove(listener);
    }

    public interface INaviListener {
        void quit();
    }

}
