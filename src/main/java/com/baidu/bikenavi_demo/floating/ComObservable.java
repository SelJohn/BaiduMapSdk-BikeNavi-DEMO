package com.baidu.bikenavi_demo.floating;


import android.graphics.drawable.Drawable;

import com.baidu.bikenavi_demo.floating.data.RGDataHolder;


/**
 * Created by hexiaojiang
 * on 2025/7/18
 */
public final class ComObservable extends RGDataHolder<ComNaviBean> {
    private static ComObservable instance;

    private ComObservable() {
        setData(new ComNaviBean());
    }

    public static synchronized ComObservable getInstance() {
        if (instance == null) {
            instance = new ComObservable();
        }
        return instance;
    }

    @Override
    public ComNaviBean getData() {
        return super.getData();
    }

    public void update(String data) {
        getData().inductionMsg = data;
        notifyDataUpdate();
    }

    public void update(Drawable data) {
        getData().inductionIcon = data;
        notifyDataUpdate();
    }
    public void update(boolean data) {
        getData().destroyed = data;
        notifyDataUpdate();
    }

    public void update(ComNaviBean data) {
        setDataAndUpdate(data);
    }

}
