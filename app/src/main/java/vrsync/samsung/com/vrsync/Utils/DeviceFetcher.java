package vrsync.samsung.com.vrsync.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.samsung.dallas.networkutils.txrx.ConnectionType;
import com.samsung.dallas.networkutils.txrx.PayloadTXManager;
import com.samsung.dallas.networkutils.txrx.TXRXDevice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import vrsync.samsung.com.vrsync.R;
import vrsync.samsung.com.vrsync.listeners.IDeviceListListener;

public class DeviceFetcher {

    private PayloadTXManager mPayloadTXManager;
    private PayloadTXManager.Listener mTXManagerListener;
    private Context mContext;
    private IDeviceListListener deviceListListener;

    public DeviceFetcher(Context context) {
        this.mContext = context;
        deviceListListener = (IDeviceListListener) mContext;
    }

    //Initialization of All CallBacks of Connected Device
    public void initializeDeviceCallbacks() {
        mPayloadTXManager = PayloadTXManager.getInstance();
        mTXManagerListener = new PayloadTXManager.Listener() {
            @Override
            public void onStarted() {
                Toast.makeText(mContext, "on Start", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartError() {
                Toast.makeText(mContext, "Start error", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFatalError() {
                Toast.makeText(mContext, "Fatal error", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReceiverConnected(TXRXDevice device) {
                deviceListListener.getDeviceList(getDeviceList());
            }

            @Override
            public void onReceiversDisconnected(Collection<TXRXDevice> devices) {
                deviceListListener.getDeviceList(getDeviceList());

            }
        };
        checkHandshakeConnection();
    }


    // Return ArrayList of Connected Devices
    private ArrayList<ConnectedDeviceExt> getDeviceList() {
        PayloadTXManager mPayloadTXManager = PayloadTXManager.getInstance();
        ArrayList<ConnectedDeviceExt> devices = new ArrayList<>();
        Collection<TXRXDevice> receivers = mPayloadTXManager.getReceivers();
        for (TXRXDevice receiver : receivers) {
            ConnectedDeviceExt cde = new ConnectedDeviceExt();
            cde.connectedDevice = receiver;
            devices.add(cde);
        }
        return devices;
    }

    //Check weather connection is establish or not
    private void checkHandshakeConnection() {
        String deviceName = ConstantSValues.getDeviceName(mContext);
        deviceName = TextUtils.isEmpty(deviceName) ? mContext.getResources().getString(R.string.controller) : deviceName;
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        final int port = preferences.getInt(ConstantSFValues.Preferences.CONTROLLER_PORT, ConstantSFValues.Numbers.CONTROLLER_PORT);
        final Set<String> whiteList = preferences.getStringSet(ConstantSFValues.Preferences.CONTROLLER_WHITE_LIST, null);
        if (whiteList != null) {
            for (String device : whiteList) {
                mPayloadTXManager.addToWhiteList(device);
            }
        } else {
            mPayloadTXManager.clearWhiteList();
        }
        mPayloadTXManager = PayloadTXManager.getInstance();
        mPayloadTXManager.addListener(mTXManagerListener);
        mPayloadTXManager.setDeviceName(deviceName);
        mPayloadTXManager.setConnectionType(ConnectionType.TYPE_ETHERNET);
        mPayloadTXManager.setMaxConnections(ConstantSFValues.Numbers.MAX_DEVICE_CONNECTIONS);
        // mPayloadTXManager.setUDPModeEnabled(false);
        //Context context, int handshakePort, int sendPort, int broadcastPort
        mPayloadTXManager.start(mContext, port, ConstantSFValues.Numbers.SEND_PORT_NUMBER, port);
    }
}
