package vrsync.samsung.com.vrsync.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import vrsync.samsung.com.vrsync.Utils.ConnectedDeviceExt;
import vrsync.samsung.com.vrsync.Utils.NetworkUtil;


public class NetworkChangeReceiver extends BroadcastReceiver {

    private static EventBus bus = EventBus.getDefault();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String status = NetworkUtil.getConnectivityStatusString(context);
        if (status != null) {
            if (status.equals("Not connected to Internet")) {
                try {
                    bus.post("Disconnected");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    bus.post("Connected");
                    ArrayList<ConnectedDeviceExt> list=new ArrayList<>();
                    bus.post(list);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}