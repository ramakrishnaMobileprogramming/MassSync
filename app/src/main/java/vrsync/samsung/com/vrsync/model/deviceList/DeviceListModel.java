package vrsync.samsung.com.vrsync.model.deviceList;

import java.util.ArrayList;

import vrsync.samsung.com.vrsync.Utils.ConnectedDeviceExt;

public class DeviceListModel implements IDeviceListModel {

    private ArrayList<ConnectedDeviceExt> dataList;

    public DeviceListModel(ArrayList<ConnectedDeviceExt> dataList) {
        this.dataList = dataList;
    }

    @Override
    public ConnectedDeviceExt getPlayListObject(int position) {
        return dataList.get(position);
    }
}