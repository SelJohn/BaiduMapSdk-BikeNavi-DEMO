/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.bikenavi_demo.location;

import com.baidu.mapapi.JNIInitializer;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * @author by liuhongjian01 on 16/8/17.
 */
public class BDLocationManager {

    private LocationManager mSysLocManager;
    private LocationListener mSysLocListener = new MLocationListener();
    private BDLocationListener listener;

    private static class Holder {
        private static BDLocationManager INSTANCE = new BDLocationManager();
    }

    private BDLocationManager() {
        initGPS(JNIInitializer.getCachedContext());

    }

    public static BDLocationManager getInstance() {
        return Holder.INSTANCE;
    }

    public void addListener(BDLocationListener listener) {
//        if (null != listener && !listeners.contains(listener)) {
//            listeners.add(listener);
//        }
        this.listener = listener;
    }

    public void removeListener() {
//        if (null != listener && listeners.contains(listener)) {
//            listeners.remove(listener);
//        }
        this.listener = null;
    }

    private void initGPS(Context context) {
        try {
            if (mSysLocManager == null) {
                /** 获取系统的定位服务管理类*/
                mSysLocManager = (LocationManager) context
                        .getSystemService(Context.LOCATION_SERVICE);
            }
            // mSysLocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mSysLocListener);
        } catch (Exception e) {
        }
    }

    public void startLoc() {
        mSysLocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mSysLocListener);
    }

    public void stopLoc() {
        mSysLocManager.removeUpdates(mSysLocListener);
    }

    private class MLocationListener implements LocationListener{

        @Override
        public void onLocationChanged(Location location) {
            if (null != listener) {
                listener.onLocationChanged(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (null != listener) {
                listener.onStatusChanged(provider, status, extras);
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            if (null != listener) {
                listener.onProviderEnabled(provider);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (null != listener) {
                listener.onProviderDisabled(provider);
            }
        }
    }
}
