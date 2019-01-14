package vrsync.samsung.com.vrsync.Utils;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

public class ConstantSValues {

    public static boolean isDevicesAvailable = false;

    static String getDeviceName(Context context) {
        if (context != null) {
            final BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
            if (myDevice == null) {
                return null;
            }
            return myDevice.getName();
        }
        return null;
    }

    //Return Duration of Audio in XX:XX format
    public static String getDurationString(Long duration) {
        int seconds = (int) ((duration / 1000) % 60);
        int minutes = (int) ((duration / 1000) / 60);
        String secondsString = Integer.toString(seconds);
        String minutesString = Integer.toString(minutes);
        if (seconds < 10) {
            secondsString = "0" + secondsString;
        }
        if (minutes < 10) {
            minutesString = "0" + minutesString;
        }
        return minutesString + ":" + secondsString;
    }

    //Convert Time to Progress percentage
    public static int convertTimeToProgress(Long totalTime, Long currentTime) {
        return (int) ((currentTime * 100) / totalTime);
    }
}