package vrsync.samsung.com.vrsync.view.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.samsung.dallas.networkutils.txrx.PayloadTXManager;
import com.samsung.dallas.networkutils.txrx.TXRXDevice;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collection;

import vrsync.samsung.com.vrsync.R;
import vrsync.samsung.com.vrsync.presenter.deviceList.IDeviceListPresenter;
import vrsync.samsung.com.vrsync.presenter.deviceList.DeviceListPresenter;
import vrsync.samsung.com.vrsync.Utils.ConnectedDeviceExt;
import vrsync.samsung.com.vrsync.view.adapters.DeviceListAdapter;

public class DeviceListFragment extends BaseFragment {

    private Context mContext;
    private RecyclerView listRV;
    private DeviceListAdapter playListAdapter;
    private LinearLayoutManager linearLayoutManager;
    private ArrayList<ConnectedDeviceExt> deviceList;
    private IDeviceListPresenter presenter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_devicelist, container, false);
        initializeView(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    private void initializeView(View view) {
        mContext = getActivity();
        listRV = view.findViewById(R.id.playList_rv);
        deviceList = new ArrayList<>();
        presenter = new DeviceListPresenter(deviceList);
        playListAdapter = new DeviceListAdapter(mContext, presenter);
        linearLayoutManager = new LinearLayoutManager(mContext);
        setValues();
    }

    private void setValues() {
        listRV.setLayoutManager(linearLayoutManager);
        listRV.setAdapter(playListAdapter);
        deviceList = getDeviceList();
        presenter.setPlayList(deviceList);
        playListAdapter.notifyDataSetChanged();
    }

    //Get List of Connected Devices
    private ArrayList<ConnectedDeviceExt> getDeviceList() {
        PayloadTXManager mPayloadTXManager = PayloadTXManager.getInstance();
        Collection<TXRXDevice> receivers = mPayloadTXManager.getReceivers();
        for (TXRXDevice receiver : receivers) {
            final ConnectedDeviceExt cde = new ConnectedDeviceExt();
            cde.connectedDevice = receiver;
            deviceList.add(cde);
            setDataOnAdapter();
        }
        return deviceList;
    }

    //Call Back of Device List
    @Override
    public void getDeviceList(ArrayList<ConnectedDeviceExt> list) {
        super.getDeviceList(list);
        setDataOnAdapter();
    }

    private void setDataOnAdapter() {
        presenter.setPlayList(deviceList);
        playListAdapter.notifyItemChanged(deviceList.size() - 1);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onControllerEvent(String videoPlayPauseState) {
        presenter.resetPlayListAccToController(videoPlayPauseState);
        playListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
}