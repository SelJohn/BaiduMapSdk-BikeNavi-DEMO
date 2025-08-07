package com.baidu.bikenavi_demo.floating.data;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by hexiaojiang
 * on 2025/8/1
 */
public abstract class RGDataHolder<T> {
    private volatile T mData;
    private final List<RGDataOBS<T>> mObservers = new CopyOnWriteArrayList<>();
    public RGDataHolder(T data) {
        mData = data;
    }
    public RGDataHolder() {
    }

    public void setDataAndUpdate(T data) {
        this.mData = data;
        notifyDataUpdate();
    }

    public synchronized void setData(T data) {
        this.mData = data;
    }

    public T getData() {
        return mData;
    }

    public void addObserver(RGDataOBS<T> observer) {
        if (observer == null) {
            return;
        }
        if (mObservers.contains(observer)) {
            return;
        }
        mObservers.add(observer);
        observer.onDataUpdate(mData);
    }
    public void removeObserver(RGDataOBS<T> observer) {
        mObservers.remove(observer);
    }

    public void notifyDataUpdate() {
        for (RGDataOBS<T> observer : mObservers) {
            observer.onDataUpdate(mData);
        }
    }
}
