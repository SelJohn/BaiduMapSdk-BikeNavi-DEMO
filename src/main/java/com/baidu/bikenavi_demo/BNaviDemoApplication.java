/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.bikenavi_demo;

import android.app.Application;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.baidu.mapapi.CommonInfo;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.common.BaiduMapSDKException;
import com.baidu.mapapi.tts.WNTTSManager;
import com.baidu.mapapi.tts.WNTTsInitConfig;

import java.io.File;


public class BNaviDemoApplication extends Application {

    public static final String TAG = "BNaviDemo";
    public static final String APP_FOLDER_NAME = TAG;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            CommonInfo commonInfo = new CommonInfo.Builder()
                    .shareDeviceId("123456789")
                    .build();
            SDKInitializer.setCommonInfo(commonInfo);
            SDKInitializer.setAgreePrivacy(this, true);
            SDKInitializer.initialize(this);
            SDKInitializer.setCoordType(CoordType.BD09LL);
        } catch (BaiduMapSDKException e) {
            e.getMessage();
        }

        initTTS();
    }

    private void initTTS() {
//        // 使用内置TTS
        WNTTsInitConfig config = new WNTTsInitConfig.Builder()
                .context(getApplicationContext())
                .appKey("*************************")
                .authSn("*****************")
                .build();

        WNTTSManager.getInstance().initTTS(config);
        WNTTSManager.getInstance().setOnTTSStateChangedListener(new WNTTSManager.IOnTTSPlayStateChangedListener() {
            @Override
            public void onPlayEnd(String s) {
                Log.e(TAG, "onPlayEnd");
            }

            @Override
            public void onPlayError(int errCode, String error) {
                Log.e(TAG, "onPlayError-" + errCode + "-" + error);
            }

            @Override
            public void onPlayStart() {
                Log.e(TAG, "onPlayStart");
            }
        });

//        WNTTSManager.getInstance().initTTS(new WNTTSManager.IWNOuterTTSPlayerCallback() {
//            @Override
//            public int playTTSText(String speech, int bPreempt, int type) {
//                Log.e(TAG, "speech：" + speech + " bPreempt：" + bPreempt + " type：" + type);
//                return 0;
//            }
//
//            @Override
//            public int getTTSState() {
//                return 0;
//            }
//        });
    }

    private String getSdcardDir() {
        if (Build.VERSION.SDK_INT >= 29) {
            // 如果外部储存可用 ,获得外部存储路径
            File file = getExternalFilesDir(null);
            if (file != null && file.exists()) {
                return file.getPath();
            } else {
                return getFilesDir().getPath();
            }
        } else {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
    }
}
