package vrsync.samsung.com.vrsync.Utils;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

import vrsync.samsung.com.vrsync.model.playList.PlayListInfoModel;

public enum PayloadUtil {

    INSTANCE;
    private final Date mDate;

    PayloadUtil() {
        mDate = Calendar.getInstance().getTime();
    }

    //Create Payload for SDK
    public String createLoad(PlayListInfoModel video) {
        String payload = null;
        if (video != null) {
            JSONObject jsonObject = new JSONObject();
            JSONObject dataObject = new JSONObject();
            String uri;
            try {
                // Prepare data part
                mDate.setTime(System.currentTimeMillis());
                // TODO always append file
                uri = video.getVideoFilePath().replaceAll(" ", "%20");
                jsonObject.put("cmd", "load");
                dataObject.put("url", "file://" + uri);
                dataObject.put("audio_type", "mono");
                dataObject.put("video_type", video.getVideoType());
                dataObject.put("title", video.getVideoTitle());
                dataObject.put("looping", "false");
                dataObject.put("position", "0");
                dataObject.put("timestamp", mDate.toString());
                jsonObject.put("data", dataObject);
                payload = jsonObject.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return payload;
    }

    //Create Pause State Payload
    public String createPause(PlayListInfoModel video) {
        //payload:{"cmd":"pause","data":"local-video:_storage_emulated_0_MilkVR_aaJaunt_TRex_cylinder_slice_2x25_3dv.mp4"}
        String payload = null;
        String uri;
        if (video != null) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("cmd", "pause");
                // Append data part
                uri = video.getVideoFilePath().replaceAll(" ", "%20");
                jsonObject.put("data", "local-video:" + uri);
                payload = jsonObject.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return payload;
    }

    //Create Play State Payload
    public String createPlay(PlayListInfoModel video) {
        String payload = null;
        String uri;
        if (video != null) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("cmd", "play");
                // Append data part
                uri = video.getVideoFilePath().replaceAll(" ", "%20");
                jsonObject.put("data", "local-video:" + uri);
                payload = jsonObject.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return payload;
    }

    //Create Seek bar Progress Payload
    public String createSeekTo(long position) {
        String payload = null;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("cmd", "seekTo");
            // Append data part
            jsonObject.put("data", String.valueOf(position));
            payload = jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }

    //Create Stop State Payload
    public String createStop(PlayListInfoModel video) {
        String payload = null;
        String uri;
        if (video != null) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("cmd", "stop");
                // Append data part
                uri = video.getVideoFilePath().replaceAll(" ", "%20");
                jsonObject.put("data", "local-video:" + uri);
                payload = jsonObject.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return payload;
    }

    //Create Payload for Device Status Condition
    public String createDeviceStatus() {
        String payload = null;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("cmd", "deviceStatus");
            payload = jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }

    //TODO -> Clear Proper Syntax of Filepath pass in "data" tag of JSON
    //Create Payload for Video File Exist
    public String createVideoFileExist(PlayListInfoModel video) {
        String payload = null;
        String uri;
        if (video != null) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("cmd", "fileExists");
                // Append data part
                uri = video.getVideoFilePath().replaceAll(" ", "%20");
                jsonObject.put("data", "local-video:" + uri);
                payload = jsonObject.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return payload;
    }
}