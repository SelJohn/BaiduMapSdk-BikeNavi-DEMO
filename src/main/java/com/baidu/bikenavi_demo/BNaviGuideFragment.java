package com.baidu.bikenavi_demo;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBNaviStatusListener;
import com.baidu.mapapi.bikenavi.adapter.IBTTSPlayer;
import com.baidu.mapapi.bikenavi.model.BikeNaviDisplayOption;

public class BNaviGuideFragment extends Fragment {
    private static final String TAG = BNaviGuideFragment.class.getSimpleName();

    private BikeNavigateHelper mNaviHelper;
    public static boolean mIsRunning = false;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (!mNaviHelper.isNaviStanding()) {
            mNaviHelper.quit();
        } else {
            mNaviHelper.onDestroy(false);
        }
        mIsRunning = false;
    }
    @Override
    public void onResume() {
        super.onResume();
        mNaviHelper.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mNaviHelper.pause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        FrameLayout frameLayout = new FrameLayout(getActivity());
        frameLayout.setLayoutParams(params);
        mNaviHelper = BikeNavigateHelper.getInstance();
        View view = mNaviHelper.onCreate(getActivity());
        if (view != null) {
            frameLayout.addView(view);
        }
        return frameLayout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mIsRunning = true;

        BikeNaviDisplayOption bikeNaviDisplayOption = new BikeNaviDisplayOption()
                .showSpeedLayout(true) // 是否展示速度切换布局
                .showTopGuideLayout(true)  // 是否展示顶部引导布局
                .runInFragment(true)  // 是否展示顶部引导布局
//                .setSpeedLayout(R.layout.custom_speed_layout)//展示自定义速度布局
                .showLocationImage(true);  // 是否展示视角切换资源

        mNaviHelper.setBikeNaviDisplayOption(bikeNaviDisplayOption);



        mNaviHelper.setBikeNaviStatusListener(new IBNaviStatusListener() {
            @Override
            public void onNaviExit() {
                Log.d(TAG, "onNaviExit");
            }
        });
        // 设置语音诱导监听, 获取语音诱导文本信息
        // 有内存风险，页面 退出 调用quit
        mNaviHelper.setTTsPlayer(new IBTTSPlayer() {
            @Override
            public int playTTSText(String s, boolean b) {
                Log.d("tts", s);
                return 0;
            }
        });
        if (BNaviMainActivity.isFakeNavi) {
            mNaviHelper.startBikeNavi(getActivity(), BikeNavigateHelper.NaviMode.FakeNavi);
            mNaviHelper.setSimulateNaviSpeed(5);
        } else {
            mNaviHelper.startBikeNavi(getActivity());
        }
    }
}
