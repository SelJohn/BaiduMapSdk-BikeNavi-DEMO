package com.baidu.bikenavi_demo;

import android.graphics.drawable.Drawable;
import android.os.Message;
import android.util.Log;

import com.baidu.bikenavi_demo.floating.ComObservable;
import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBRouteGuidanceListener;
import com.baidu.mapapi.bikenavi.model.BikeRouteDetailInfo;
import com.baidu.mapapi.bikenavi.model.BikeSimpleMapInfo;
import com.baidu.mapapi.bikenavi.model.IBRouteIconInfo;
import com.baidu.mapapi.walknavi.adapter.IWRouteGuidanceListener;
import com.baidu.mapapi.walknavi.model.IWRouteIconInfo;
import com.baidu.mapapi.walknavi.model.RouteGuideKind;
import com.baidu.mapapi.walknavi.model.WalkSimpleMapInfo;

/**
 * Created by hexiaojiang
 * on 2025/8/5
 */
public class CommonGuideListener {
    public static final IWRouteGuidanceListener WALK_GUIDE_LISTENER = new IWRouteGuidanceListener() {
        @Override
        public void onRouteGuideIconInfoUpdate(IWRouteIconInfo routeIconInfo) {

        }

        @Override
        public void onRouteGuideIconUpdate(Drawable icon) {
            ComObservable.getInstance().update(icon);
        }

        @Override
        public void onRouteGuideKind(RouteGuideKind routeGuideKind) {

        }

        @Override
        public void onRoadGuideTextUpdate(CharSequence guideOne, CharSequence guideTwo) {
            ComObservable.getInstance().update(guideOne.toString() + "\n" + guideTwo.toString());
        }

        @Override
        public void onRemainDistanceUpdate(CharSequence remainDistance) {

        }

        @Override
        public void onRemainDistanceUpdate(int remainDistance) {

        }

        @Override
        public void onRemainTimeUpdate(CharSequence remainTime) {

        }

        @Override
        public void onRemainTimeUpdate(int remainTime) {

        }

        @Override
        public void onGpsStatusChange(CharSequence gpsText, Drawable icon) {

        }

        @Override
        public void onRouteFarAway(CharSequence guideText, Drawable icon) {

        }

        @Override
        public void onRoutePlanYawing(CharSequence guideText, Drawable icon) {

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
        public void onSimpleMapInfoUpdate(WalkSimpleMapInfo info) {
            Log.i("GuideIconObjectUpdate", "WalkSimpleMapInfo: " + info);
        }
    };
    public static final IBRouteGuidanceListener BIKE_GUIDE_LISTENER = new IBRouteGuidanceListener() {

        @Override
        public void onRouteGuideIconInfoUpdate(IBRouteIconInfo routeIconInfo) {

        }

        @Override
        public void onRouteGuideIconUpdate(Drawable icon) {
            ComObservable.getInstance().update(icon);
        }

        @Override
        public void onRouteGuideKind(RouteGuideKind routeGuideKind) {

        }

        @Override
        public void onRoadGuideTextUpdate(CharSequence guideOne, CharSequence guideTwo) {
            ComObservable.getInstance().update(guideOne.toString() + "\n" + guideTwo.toString());
        }

        @Override
        public void onRemainDistanceUpdate(CharSequence remainDistance) {

        }

        @Override
        public void onRemainDistanceUpdate(int remainDistance) {
        }

        @Override
        public void onRemainTimeUpdate(CharSequence remainTime) {

        }

        @Override
        public void onRemainTimeUpdate(int remainTime) {
        }

        @Override
        public void onGpsStatusChange(CharSequence gpsText, Drawable icon) {

        }

        @Override
        public void onGpsStatusChange(int gpsSignalLevel) {
        }

        @Override
        public void onRouteFarAway(CharSequence guideText, Drawable icon) {

        }

        @Override
        public void onRoutePlanYawing(CharSequence guideText, Drawable icon) {

        }

        @Override
        public void onReRouteComplete() {

        }

        @Override
        public void onArriveDest() {
            BikeNavigateHelper.getInstance().quit();
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
        public void onSimpleMapInfoUpdate(BikeSimpleMapInfo info) {
            Log.i("GuideIconObjectUpdate", "BikeSimpleMapInfo: " + info);
        }

        @Override
        public void onRouteRemainTrafficLightCountUpdate(int remainCount) {
            Log.i("GuideIconObjectUpdate", "onRouteRemainTrafficLightCountUpdate: " + remainCount);
        }
    };
}
