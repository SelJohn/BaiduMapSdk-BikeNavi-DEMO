/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.bikenavi_demo.location;

import android.location.Location;
import android.os.Bundle;

/**
 * @author by liuhongjian01 on 16/8/17.
 */
public interface BDLocationListener {
    void onLocationChanged(Location location);
    void onStatusChanged(String provider, int status, Bundle extras);
    void onProviderEnabled(String provider);
    void onProviderDisabled(String provider);
}
