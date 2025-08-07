/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.bikenavi_demo;

import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWNaviStatusListener;
import com.baidu.mapapi.walknavi.adapter.IWTTSPlayer;
import com.baidu.mapapi.walknavi.model.WalkNaviDisplayOption;
import com.baidu.platform.comapi.walknavi.WalkNaviModeSwitchListener;
import com.baidu.platform.comapi.walknavi.widget.ArCameraView;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * 目前步行导航支持三种自定义布局，使用时需指定view的类型以及tag，view的ID可自定义（避免与其它id重复），具体如下：
 *      1.setTopGuideLayout 顶部引导布局
 *          顶部根布局容器View  tag：BMSDK_LAYOUT_GUIDE
 *          GPS提示布局：TextView	 tag：BMSDK_LAYOUT_GPS_WEAK
 *          GPS信号弱文案：TextView	tag：BMSDK_TEXT_GPS_WEAK
 *          GPS信号提示引导（请步行到开阔地带）：TextView	 tag：BMSDK_TEXT_GPS_HINT
 *          顶部引导-左侧图片：ImageView	tag：BMSDK_IMAGE_IVICON
 *          剩余信息（AR模式下使用）TextView	tag：BMSDK_TEXT_GUIDE_REMAIN
 *          顶部导航前进信息：TextView	 tag：BMSDK_TEXT_GUIDE
 *      2.setCustomBottomView 最底部自定义view，自由设计，位于页面最底部，会被添加到sync_view中
 *      3.setBottomSettingLayout 底部布局
 *          左下角整体退出布局： View（容器view） tag：BMSDK_LAYOUT_BOTSET_QUIT
 *          左下角退出x号：ImageView	tag：BMSDK_IMAGE_QUIT_ICON
 *          左下角退出文案：TextView	tag：BMSDK_TEXT_QUIT_TV
 *          中间查看全览：TextView	tag：BMSDK_TEXT_LOOKOVER
 *          中间剩余信息：TextView	tag：BMSDK_TEXT_REMAIN_CONTENT
 *
 *     具体使用可参考custom_speed_layout布局文件，源码布局文件可参考wsdk_layout_rg_ui_layoutndof
 */
public class WNaviGuideActivity extends Activity {

    private final static String TAG = WNaviGuideActivity.class.getSimpleName();

    private WalkNavigateHelper mNaviHelper;
    private boolean isViewAll = false;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mNaviHelper.isNaviStanding()) {
            mNaviHelper.onDestroy(false);
        } else {
            mNaviHelper.quit();
        }
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

        mNaviHelper = WalkNavigateHelper.getInstance();
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
            View view = mNaviHelper.onCreate(WNaviGuideActivity.this);
            if (view != null) {
                // 添加导航地图
                frameLayout.addView(view);
//                Button button = new Button(this);
//                button.setText("全览");
//                button.setOnClickListener(v -> {
//                    isViewAll = !isViewAll;
//                    if (isViewAll) {
//                        button.setText("取消全览");
//                    } else {
//                        button.setText("全览");
//                    }
//                    mNaviHelper.setViewAllStatus(isViewAll);
//                });
//                frameLayout.addView(button, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                // 设置界面布局
                setContentView(frameLayout);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        mNaviHelper.setWalkNaviStatusListener(new IWNaviStatusListener() {
            @Override
            public void onWalkNaviModeChange(int mode, WalkNaviModeSwitchListener listener) {
                Log.d(TAG, "onWalkNaviModeChange : " + mode);
                mNaviHelper.switchWalkNaviMode(WNaviGuideActivity.this, mode, listener);
            }

            @Override
            public void onNaviExit() {
                Log.d(TAG, "onNaviExit");
            }
        });

        mNaviHelper.setTTsPlayer(new IWTTSPlayer() {
            @Override
            public int playTTSText(final String s, boolean b) {
                Log.d(TAG, "tts: " + s);
                return 0;
            }
        });
        if (BNaviMainActivity.isFakeNavi) {
            mNaviHelper.startWalkNavi(this, WalkNavigateHelper.NaviMode.FakeNavi);
            mNaviHelper.setSimulateNaviSpeed(2);
        } else {
            mNaviHelper.startWalkNavi(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ArCameraView.WALK_AR_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(WNaviGuideActivity.this, "没有相机权限,请打开后重试", Toast.LENGTH_SHORT).show();
            } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mNaviHelper.startCameraAndSetMapView(WNaviGuideActivity.this);
            }
        }
    }
}
