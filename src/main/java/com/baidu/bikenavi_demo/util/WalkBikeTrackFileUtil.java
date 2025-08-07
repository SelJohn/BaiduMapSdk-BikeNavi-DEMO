package com.baidu.bikenavi_demo.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;

import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.platform.comapi.wnplatform.model.datastruct.WCoordType;
import com.baidu.platform.comapi.wnplatform.model.datastruct.WLocData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 轨迹记录 https://ku.baidu-int.com/knowledge/HFVrC7hq1Q/o5e1KKgbaR/4QNgPPb1ex/uHg2fQvmeWBxO0
 * 轨迹生成 https://ku.baidu-int.com/knowledge/HFVrC7hq1Q/o5e1KKgbaR/4QNgPPb1ex/q-z8SsJbuSGGku
 * 轨迹地址 /Android/data/com.baidu.bikenavi_demo/files/trace
 */
public class WalkBikeTrackFileUtil {

    private static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/Android/data/com.baidu.bikenavi_demo" + "/files/trace";
    private Timer refreshTimer;
    private TimerTask refreshTimerTask;
    private String planData;
    private String[] trackList;
    private int trackIndex;
    private ITrace listener;
    private static WalkBikeTrackFileUtil instance;

    public static synchronized WalkBikeTrackFileUtil getInstance() {
        if (instance == null) {
            instance = new WalkBikeTrackFileUtil();
        }
        return instance;
    }

    public WalkBikeTrackFileUtil() {

    }

    public void selectFile(Context context, ITrace listener) {
        this.listener = listener;
        File trackPath = new File(ROOT_PATH);
        if (!trackPath.exists() || !trackPath.isDirectory()) {
            Toast.makeText(context, "目录不存在:" + ROOT_PATH, Toast.LENGTH_SHORT).show();
            return ;
        }
        File[] trackFiles = trackPath.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".txt");
            }
        });
        if (trackFiles == null || 0 == trackFiles.length) {
            Toast.makeText(context, "目录下不存在轨迹文件", Toast.LENGTH_SHORT).show();
            return ;
        }

        final String[] fileList = new String[trackFiles.length];
        for (int i = 0; i < trackFiles.length; i++) {
            fileList[i] = trackFiles[i].getName();
        }

        new AlertDialog.Builder(context)
                .setTitle("选择轨迹")
                .setItems(fileList, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String filename = fileList[which];
                        readFileData(filename);
                    }
                })
                .create()
                .show();
    }

    private void readFileData(String filename) {
        String pathName = ROOT_PATH + "/" + filename;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            File urlFile = new File(pathName);
            InputStreamReader isr = new InputStreamReader(new FileInputStream(urlFile), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);

            String mimeTypeLine = null ;
            while ((mimeTypeLine = br.readLine()) != null) {
                stringBuilder.append(mimeTypeLine);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String trackData = stringBuilder.toString();
        // 根据---分隔路线和轨迹
        String[] trackSplitList = trackData.split("---");
        planData = trackSplitList[0];
        // 再根据===分隔每一条轨迹
        trackList = trackSplitList[1].split("===");
        trackIndex = 0;
        listener.readTraceFileFinish();
    }

    public void playTrack() {
        if (refreshTimerTask != null) {
            refreshTimerTask.cancel();
        }
        refreshTimerTask = new TimerTask() {
            @Override
            public void run() {
                buildAndTrigger();
            }
        };
        if (refreshTimer == null) {
            refreshTimer = new Timer();
        }
        refreshTimer.schedule(refreshTimerTask, 0, 1000);
    }

    private void buildAndTrigger() {
        if (trackIndex >= trackList.length) {
            if (refreshTimerTask != null) {
                refreshTimerTask.cancel();
            }

            return;
        }
        String itemData = trackList[trackIndex];
        String[] itemList = itemData.split(",");
        WLocData locData = new WLocData();
        if (itemList.length > 0) {
            locData.latitude = !TextUtils.isEmpty(itemList[0]) ? Double.parseDouble(itemList[0]) : 0;
            locData.longitude = !TextUtils.isEmpty(itemList[1]) ? Double.parseDouble(itemList[1]) : 0;
            locData.speed = !TextUtils.isEmpty(itemList[2]) ? Float.parseFloat(itemList[2]) : 0;
            locData.direction = !TextUtils.isEmpty(itemList[3]) ? Float.parseFloat(itemList[3]) : 0;
            locData.accuracy = !TextUtils.isEmpty(itemList[4]) ? Float.parseFloat(itemList[4]) : 0;
            locData.altitude = !TextUtils.isEmpty(itemList[14]) ? Double.parseDouble(itemList[14]) : 0;
            locData.buildingId = itemList[8];
            locData.floorId = itemList[9];
            locData.coordType = WCoordType.BD09LL;
            locData.indoorState = !TextUtils.isEmpty(itemList[13]) ? Integer.parseInt(itemList[13]) : 0;
            locData.type = !TextUtils.isEmpty(itemList[6]) ? Integer.parseInt(itemList[6]) : 0;
            locData.networkLocType = itemList[10];
            locData.satellitesNum = !TextUtils.isEmpty(itemList[5]) ? Integer.parseInt(itemList[5]) : 0;
        }
        WalkNavigateHelper.getInstance().triggerLocation(locData);
        trackIndex++;
    }

    public String getPlanData() {
        return planData;
    }
}
