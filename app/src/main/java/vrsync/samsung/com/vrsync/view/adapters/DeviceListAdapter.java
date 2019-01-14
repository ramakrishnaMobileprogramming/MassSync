package vrsync.samsung.com.vrsync.view.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.dallas.networkutils.txrx.PayloadTXManager;
import com.samsung.dallas.networkutils.txrx.TXRXDevice;

import java.util.Collection;

import vrsync.samsung.com.vrsync.R;
import vrsync.samsung.com.vrsync.Utils.ConnectedDeviceExt;
import vrsync.samsung.com.vrsync.Utils.PayloadUtil;
import vrsync.samsung.com.vrsync.model.playList.PlayListData;
import vrsync.samsung.com.vrsync.presenter.deviceList.IDeviceListPresenter;
import vrsync.samsung.com.vrsync.view.mvpView.IDeviceListViewRow;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.PlayListViewHolder> {

    private IDeviceListPresenter presenter;
    private Context mContext;

    public DeviceListAdapter(Context context, IDeviceListPresenter userDataPresenter) {
        this.mContext = context;
        this.presenter = userDataPresenter;
    }

    @NonNull
    @Override
    public PlayListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.devicelist_rv_row, viewGroup, false);
        return new PlayListViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayListViewHolder viewHolder, int i) {
        presenter.getData(viewHolder, i);
    }

    @Override
    public int getItemCount() {
        return presenter.getCount();
    }

    class PlayListViewHolder extends RecyclerView.ViewHolder implements IDeviceListViewRow, View.OnClickListener {
        private TextView macAddressTV;
        private TextView playBackDelayTV;
        private TextView temperatureTV;
        private TextView batteryLevelTV;
        private ImageView playPauseStateIV;
        private ConnectedDeviceExt model;
        private String payload;
        private PayloadUtil mPayloadUtil;
        private PayloadTXManager mPayloadTXManager;

        PlayListViewHolder(View v) {
            super(v);
            macAddressTV = v.findViewById(R.id.mac_address_tv);
            playBackDelayTV = v.findViewById(R.id.playback_delay_tv);
            temperatureTV = v.findViewById(R.id.temperature_tv);
            batteryLevelTV = v.findViewById(R.id.battery_level_tv);
            playPauseStateIV = v.findViewById(R.id.play_pause_iv);
            mPayloadUtil = PayloadUtil.INSTANCE;
            mPayloadTXManager = PayloadTXManager.getInstance();
            setListeners();
        }

        private void setListeners() {
            playPauseStateIV.setOnClickListener(this);
        }

        @Override
        public void getData(ConnectedDeviceExt model) {
            this.model = model;
            String macAddress = model.connectedDevice.deviceName + "\n" + model.connectedDevice.hostName;
            macAddressTV.setText(macAddress);
            /*playBackDelayTV.setText(playListItemClicked.getPlayBackDelay());
            temperatureTV.setText(playListItemClicked.getTemperature());
            batteryLevelTV.setText(playListItemClicked.getBatteryLevel());*/
            playPauseStateIV.setImageResource(getDrawable(model.getPlayPauseState()));
            // temperatureTV.setText(" 46.7"+ #x2103);
            temperatureTV.setTextColor(mContext.getResources().getColor(R.color.critical));
            playBackDelayTV.setText("0");
            batteryLevelTV.setText(R.string.twelve_percent);
            batteryLevelTV.setTextColor(mContext.getResources().getColor(R.color.critical));
        }

        private int getDrawable(boolean playPauseState) {
            if (playPauseState) {
                return R.drawable.pause_small_btn;
            } else {
                return R.drawable.play_small_btn;
            }
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case (R.id.play_pause_iv):
                    if (checkIfModelIsConnected()) {
                        if (model.getPlayPauseState()) {
                            playPauseStateIV.setImageResource(R.drawable.play_small_btn);
                            payload = mPayloadUtil.createPause(PlayListData.getInstance().getCurrentModel());
                            model.setPlayPauseState(false);
                            new SendPayload().execute();
                        } else {
                            playPauseStateIV.setImageResource(R.drawable.pause_small_btn);
                            payload = mPayloadUtil.createPlay(PlayListData.getInstance().getCurrentModel());
                            model.setPlayPauseState(true);
                            new SendPayload().execute();
                        }
                    } else {
                        Toast.makeText(mContext, "Error!!,Device may be disconnected", Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
            }
        }

        private boolean checkIfModelIsConnected() {
            Collection<TXRXDevice> receivers = mPayloadTXManager.getReceivers();
            for (TXRXDevice receiver : receivers) {
                if (model.connectedDevice.hostName.equalsIgnoreCase(receiver.hostName)) {
                    return true;
                }
            }
            return false;
        }

        public class SendPayload extends AsyncTask<String, String, String> {

            @Override
            protected String doInBackground(String... strings) {
                //for particular device
                mPayloadTXManager.sendPayload(model.connectedDevice, payload);
                //for all devices
                //mPayloadTXManager.sendPayload(payload);
                return null;
            }
        }
    }
}