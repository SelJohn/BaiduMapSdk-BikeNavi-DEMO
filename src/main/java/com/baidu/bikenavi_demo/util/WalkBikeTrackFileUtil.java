package com.baidu.bikenavi_demo.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;

import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.platform.comapi.wnplatform.model.datastruct.WCoordType;
import com.baidu.platform.comapi.wnplatform.model.datastruct.WLocData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;
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
    private boolean isBike = false;
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
                return file.getName().endsWith(".txt") || file.getName().endsWith(".json");
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
        if (pathName.endsWith(".txt")) {
            // 根据---分隔路线和轨迹
            String[] trackSplitList = trackData.split("---");
            planData = trackSplitList[0];
            // 再根据===分隔每一条轨迹
            trackList = trackSplitList[1].split("===");
            trackIndex = 0;
        } else if (pathName.endsWith(".json")) {
            isBike = true;
            // 解析Json格式轨迹：将data数组转换为逗号分隔的字符串列表，索引顺序：Lng,Lat,Speed,Angle,Accuracy,...,Altitude,...
            try {
                JSONObject jsonObject = new JSONObject(trackData);
                JSONArray dataArray = jsonObject.optJSONArray("data");
                if (dataArray != null) {
                    String[] parsedTracks = new String[dataArray.length()];
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject pointObj = dataArray.getJSONObject(i);
                        // 按照txt格式字段顺序拼接：Lat,Lng,Speed,Direction(mapped from Angle),Accuracy,SatellitesNum,
                        // Type,BuildingId,FloorId,NetworkLocType,IndoorState,CoordType(empty for now),
                        // X,Y,Altitude
                        String senserAngle = pointObj.optString("SenserAngle", "");
                        String angleStr = !TextUtils.isEmpty(senserAngle) ? senserAngle :
                                (!TextUtils.isEmpty(pointObj.optString("Angle")) ? pointObj.optString("Angle") : "0");
                        
                        double latitude = parseDouble(pointObj.optString("Lat"));
                        double longitude = parseDouble(pointObj.optString("Lng"));
                        float speed = TextUtils.isEmpty(pointObj.optString("Speed")) ? 0f : Float.parseFloat(pointObj.optString("Speed"));
                        float direction = TextUtils.isEmpty(angleStr) ? 0f : Float.parseFloat(angleStr);
                        String accuracyStr = pointObj.optString("Accuracy");
                        float accuracy = TextUtils.isEmpty(accuracyStr) ? 0f : Float.parseFloat(accuracyStr);
                        int satellitesNum = 0; // Json无此字段，默认0

                        int type = Integer.parseInt(TextUtils.isEmpty(pointObj.optString("LocationKind")) ? "0" : pointObj.optString("LocationKind"));
                        String buildingId = "-"; // Json无buildingId，使用默认值
                        String floorId = "-";   // Json无floorId，使用默认值
                        String networkLocType = ""; // 默认空
                        int indoorState = -1;     // 表示未知室内外状态
                        double altitude = TextUtils.isEmpty(pointObj.optString("Altitude", "0").trim()) || "null".equalsIgnoreCase(pointObj.optString("Altitude")) 
                                ? 0.0 : Double.parseDouble(pointObj.optString("Altitude"));

                        StringBuilder sb = new StringBuilder();
                        sb.append(String.valueOf(latitude)).append(",")
                          .append(String.valueOf(longitude)).append(",")
                          .append(String.valueOf(speed)).append(",")
                          .append(String.valueOf(direction)).append(",")
                          .append(String.valueOf(accuracy)).append(",")
                          .append("0").append(",")
                          .append("0").append(",") // reserved empty field
                          .append("0").append(",")
                          .append("0").append(",")
                          .append("0").append(",")
                          .append("0").append(",")
                          .append("0").append(",")
                          .append("0").append(",")
                          .append("0").append(",")
                          .append(altitude).append(",")
                          .append("0").append(",")
                          .append("0").append(",")
                          .append("0").append(",")
                          .append("0").append(",");
                        parsedTracks[i] = sb.toString();
                    }
                    trackList = parsedTracks;
                }
                JSONObject ori = jsonObject.getJSONObject("startendInfo");
                String s1 = ori.getJSONObject("startPoint").optString("lat");
                String s2 = ori.getJSONObject("startPoint").optString("lng");
                String e1 = ori.getJSONObject("endPoint").optString("lat");
                String e2 = ori.getJSONObject("endPoint").optString("lng");
                planData = s1 + "," + s2 + "," + e1 + "," + e2;
                trackIndex = 0;
            } catch (Exception e) {
                throw new RuntimeException("parse json failed", e);
            }
        }

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
        if (isBike) {
            BikeNavigateHelper.getInstance().triggerLocation(locData);
        } else {
            WalkNavigateHelper.getInstance().triggerLocation(locData);
        }
        trackIndex++;
    }

    public String getPlanData() {
        return planData;
    }

    private static double parseDouble(String value) {
        if (TextUtils.isEmpty(value)) return 0;
        try {
            String trimmed = value.trim();
            if ("null".equalsIgnoreCase(trimmed)) return 0;
            // Handle string values like "12117103" or "34.214058"
            if (TextUtils.isDigitsOnly(trimmed)) {
                return Long.parseLong(trimmed);
            }
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean isBike() {
        return isBike;
    }
}
