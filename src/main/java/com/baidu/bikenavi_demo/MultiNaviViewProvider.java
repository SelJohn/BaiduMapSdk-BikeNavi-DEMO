package com.baidu.bikenavi_demo;

import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.baidu.mapapi.common.model.MapCustomDrawOption;
import com.baidu.mapapi.common.model.NaviDrawElementType;
import com.baidu.mapapi.map.LogoPosition;
import com.baidu.platform.comapi.map.MapTextureView;
import com.baidu.platform.comapi.wnplatform.model.OverLookingMode;
import com.baidu.platform.comapi.wnplatform.mulitmap.IMultiNaviView;
import com.baidu.platform.comapi.wnplatform.mulitmap.MultiNaviView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author zhuxiaoan
 * @date 2024/1/14
 * @describe 多实例 view 的提供者
 */
public class MultiNaviViewProvider {

    public static final String TAG_DEFAULT = "default";
    private final Set<IMultiNaviViewProxy> sMultiNaviViewProxySet = new HashSet<>();

    private static MultiNaviViewProvider sProvider;

    private MultiNaviViewProvider() {
    }

    public static MultiNaviViewProvider getInstance() {
        if (sProvider == null) {
            synchronized (MultiNaviViewProvider.class) {
                if (sProvider == null) {
                    sProvider = new MultiNaviViewProvider();
                }
            }
        }
        return sProvider;
    }

    public IMultiNaviViewProxy createDefaultMultiNaviView(Context context) {
        return createMultiNaviView(context, TAG_DEFAULT);
    }

    public IMultiNaviViewProxy createMultiNaviView(Context context, String tag) {
        IMultiNaviViewProxy multiNaviView = getMultiNaviView(tag);
        if (multiNaviView != null) {
            return multiNaviView;
        }
        // 当前只支持一个多实例
        if (sMultiNaviViewProxySet.size() > 0) {
            return null;
        }

        return new MultiNaviViewProxy(context, tag);
    }

    public IMultiNaviViewProxy getDefaultMultiNaviView() {
        return getMultiNaviView(TAG_DEFAULT);
    }

    public IMultiNaviViewProxy getMultiNaviView(String tag) {
        if (TextUtils.isEmpty(tag)) {
            return null;
        }
        for (IMultiNaviViewProxy proxy : sMultiNaviViewProxySet) {
            if (!(proxy instanceof MultiNaviViewProxy)) {
                continue;
            }
            if (tag.equals(((MultiNaviViewProxy) proxy).mTag)) {
                return proxy;
            }
        }
        return null;
    }

    public interface IMultiNaviViewProxy extends IMultiNaviView {
        void injectMultiNaviView(ViewGroup parent);

        void injectMultiNaviView(ViewGroup parent, ViewGroup.LayoutParams params);
    }

    private class MultiNaviViewProxy implements IMultiNaviViewProxy {

        private final String mTag;
        private final MultiNaviView mMultiNaviView;

        public MultiNaviViewProxy(Context context, String tag) {
            this.mTag = tag;
            this.mMultiNaviView = new MultiNaviView(context);
            sMultiNaviViewProxySet.add(this);
        }

        @Override
        public void onResume() {
            this.mMultiNaviView.onResume();
        }

        @Override
        public void onPause() {
            this.mMultiNaviView.onPause();
        }

        @Override
        public void onDestroy() {
            this.mMultiNaviView.onDestroy();
            sMultiNaviViewProxySet.remove(this);
        }

        @Override
        public boolean setNaviType(int type) {
            return this.mMultiNaviView.setNaviType(type);
        }

        @Override
        public boolean setRotateMode(Map_Rotate_Mode model) {
            return this.mMultiNaviView.setRotateMode(model);
        }

        @Override
        public boolean setDefaultOverlooking(OverLookingMode mode) {
            return this.mMultiNaviView.setDefaultOverlooking(mode);
        }

        @Override
        public boolean setDefaultLevel(float level) {
            return this.mMultiNaviView.setDefaultLevel(level);
        }

        @Override
        public boolean setSupBackgroundDraw(boolean enable) {
            return this.mMultiNaviView.setSupBackgroundDraw(enable);
        }

        @Override
        public boolean isSetBackgroundDraw() {
            return this.mMultiNaviView.isSetBackgroundDraw();
        }

        @Override
        public void setMapPausedDraw(boolean enable) {
            this.mMultiNaviView.setMapPausedDraw(enable);
        }

        @Override
        public MapTextureView getMapTextureView() {
            return this.mMultiNaviView.getMapTextureView();
        }

        @Override
        public void screenshot(SnapshotReadyCallback callback, final boolean needLogo) {
            this.mMultiNaviView.screenshot(callback, needLogo);
        }

        @Override
        public void setMapDpiScale(float mapDpi, float logoScale) {
            this.mMultiNaviView.setMapDpiScale(mapDpi, logoScale);
        }

        @Override
        public boolean setMapCustomStylePath(String customStyleFilePath) {
            return this.mMultiNaviView.setMapCustomStylePath(customStyleFilePath);
        }

        @Override
        public void setPadding(int l, int t, int r, int b) {
            this.mMultiNaviView.setPadding(l, t, r, b);
        }

        @Override
        public void setLogoPosition(LogoPosition position) {
            this.mMultiNaviView.setLogoPosition(position);
        }

        @Override
        public boolean setNaviDrawElementsShow(boolean isShow, List<NaviDrawElementType> elements) {
            return this.mMultiNaviView.setNaviDrawElementsShow(isShow, elements);
        }

        @Override
        public int updateExtMapRenderCustomDrawOption(MapCustomDrawOption option) {
            return this.mMultiNaviView.updateExtMapRenderCustomDrawOption(option);
        }

        @Override
        public void setBuildingsEnabled(boolean enabled) {
            this.mMultiNaviView.setBuildingsEnabled(enabled);
        }

        @Override
        public void setMultiViewCreateCallback(OnMultiViewCreateCallback multiViewCreateCallback) {
            this.mMultiNaviView.setMultiViewCreateCallback(multiViewCreateCallback);
        }

        @Override
        public void showPoiMark(boolean isShow) {
            this.mMultiNaviView.showPoiMark(isShow);

        }
        //        @Override
//        public boolean setBackgroundDrawFps(int fps) {
//            return this.mMultiNaviView.setBackgroundDrawFps(fps);
//        }
//
//        @Override
//        public boolean openBackgroundMap() {
//            return this.mMultiNaviView.openBackgroundMap();
//        }
//
//        @Override
//        public boolean closeBackgroundMap() {
//            return this.mMultiNaviView.closeBackgroundMap();
//        }

        @Override
        public void injectMultiNaviView(ViewGroup parent) {
            injectMultiNaviView(parent, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }

        @Override
        public void injectMultiNaviView(ViewGroup parent, ViewGroup.LayoutParams params) {
            MultiNaviView multiNaviView = this.mMultiNaviView;
            ViewParent oldParent = multiNaviView.getParent();
            if (oldParent instanceof ViewGroup) {
                ((ViewGroup) oldParent).removeView(multiNaviView);
            }
            parent.addView(multiNaviView, params);
        }

        @Override
        public int hashCode() {
            return mTag == null ? super.hashCode() : mTag.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof MultiNaviViewProxy)) {
                return false;
            }

            return mTag == null ? super.equals(obj) : mTag.equals(((MultiNaviViewProxy) obj).mTag);
        }
    }
}
