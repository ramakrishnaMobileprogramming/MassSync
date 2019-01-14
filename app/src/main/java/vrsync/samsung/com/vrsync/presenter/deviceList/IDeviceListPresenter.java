package vrsync.samsung.com.vrsync.presenter.deviceList;

import java.util.ArrayList;

import vrsync.samsung.com.vrsync.Utils.ConnectedDeviceExt;
import vrsync.samsung.com.vrsync.view.mvpView.IDeviceListViewRow;

public interface IDeviceListPresenter {

    void getData(IDeviceListViewRow view, int position);

    int getCount();

    void setPlayList(ArrayList<ConnectedDeviceExt> list);

    void resetPlayListAccToController(String action);
}
