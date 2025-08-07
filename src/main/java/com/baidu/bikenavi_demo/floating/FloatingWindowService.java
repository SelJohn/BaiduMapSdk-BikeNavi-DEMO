package com.baidu.bikenavi_demo.floating;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.bikenavi_demo.BNaviGuideActivity;
import com.baidu.bikenavi_demo.R;
import com.baidu.bikenavi_demo.floating.data.RGDataOBS;
import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;

import java.util.Observable;
import java.util.Observer;

public class FloatingWindowService extends Service implements RGDataOBS<ComNaviBean> {
    private WindowManager mWindowManager;
    private View mFloatingView;
    private TextView textView;
    private ImageView inductionIcon;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ComObservable.getInstance().addObserver(this);
        ComObservable.getInstance().update(false);
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;
        startForegroundService();
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);
        textView = mFloatingView.findViewById(R.id.title_text);
        inductionIcon = mFloatingView.findViewById(R.id.induction_icon);

        ImageView closeButton = mFloatingView.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BikeNavigateHelper.getInstance().quit();
                WalkNavigateHelper.getInstance().quit();
                stopSelf();
            }
        });

        mFloatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long lastClickTime = 0;
            private static final int CLICK_TIMEOUT = 500;
            private static final int REQUIRED_CLICKS = 2;
            private int clickCount = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastClickTime <= CLICK_TIMEOUT) {
                            clickCount++;
                        } else {
                            clickCount = 1; // 重置计数
                        }
                        lastClickTime = currentTime;

                        if (clickCount == REQUIRED_CLICKS) {
                            if (BikeNavigateHelper.getInstance().isNavigating()) {
                                if (BNaviGuideActivity.mIsRunning) {
                                    return true;
                                }
                                Intent intent = new Intent();
                                intent.setClass(FloatingWindowService.this, BNaviGuideActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            } else {
                                Toast.makeText(FloatingWindowService.this, "请先开启导航", Toast.LENGTH_SHORT).show();
                            }
                            clickCount = 0; // 重置
                        }
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ComObservable.getInstance().update(true);
        ComObservable.getInstance().removeObserver(this);
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }

    private void startForegroundService() {
        createNotificationChannel();
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, "悬浮窗服务通道")
                    .setContentTitle("悬浮窗服务")
                    .setContentText("正在运行中")
                    .setSmallIcon(R.drawable.ic_close)
                    .build();
        }

        startForeground(121, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "悬浮窗服务通道",
                    "悬浮窗服务通道",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDataUpdate(ComNaviBean comNaviBean) {
        if (textView != null && comNaviBean.inductionMsg != null) {
            textView.setText(comNaviBean.inductionMsg);
        }
        if (inductionIcon != null && comNaviBean.inductionIcon != null) {
            inductionIcon.setImageDrawable(comNaviBean.inductionIcon);
        }
    }
}