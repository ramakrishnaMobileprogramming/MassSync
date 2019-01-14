package vrsync.samsung.com.vrsync.presenter.deviceList;

import java.util.ArrayList;

import vrsync.samsung.com.vrsync.Utils.ConnectedDeviceExt;
import vrsync.samsung.com.vrsync.Utils.ConstantSFValues;
import vrsync.samsung.com.vrsync.model.deviceList.DeviceListModel;
import vrsync.samsung.com.vrsync.model.deviceList.IDeviceListModel;
import vrsync.samsung.com.vrsync.view.mvpView.IDeviceListViewRow;

public class DeviceListPresenter implements IDeviceListPresenter {

    private ArrayList<ConnectedDeviceExt> list;

    public DeviceListPresenter(ArrayList<ConnectedDeviceExt> list) {
        this.list = list;
    }

    @Override
    public void setPlayList(ArrayList<ConnectedDeviceExt> list) {
        this.list = list;
    }

    @Override
    public void getData(IDeviceListViewRow v, int position) {
        IDeviceListModel listModel = new DeviceListModel(list);
        ConnectedDeviceExt model = listModel.getPlayListObject(position);
        v.getData(model);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public void resetPlayListAccToController(String action) {
        if (!list.isEmpty()) {
            if (action.equalsIgnoreCase(ConstantSFValues.PlayListControls.IDLE)) {
                resetPlayPauseState(false);
            } else if (action.equalsIgnoreCase(ConstantSFValues.PlayListControls.PLAY)) {
                resetPlayPauseState(true);
            } else if (action.equalsIgnoreCase(ConstantSFValues.PlayListControls.PAUSE)) {
                resetPlayPauseState(false);
            }
        }
    }

    private void resetPlayPauseState(boolean playPauseState) {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setPlayPauseState(playPauseState);
        }
    }
}