package vrsync.samsung.com.vrsync.listeners;

import java.util.ArrayList;

import vrsync.samsung.com.vrsync.Utils.ConnectedDeviceExt;

public interface IDeviceListListener {
    void getDeviceList(ArrayList<ConnectedDeviceExt> list);
}