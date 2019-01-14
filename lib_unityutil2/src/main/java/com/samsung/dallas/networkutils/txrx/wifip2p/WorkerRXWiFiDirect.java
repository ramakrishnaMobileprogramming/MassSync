package com.samsung.dallas.networkutils.txrx.wifip2p;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Pair;

import com.samsung.dallas.networkutils.txrx.Constants;
import com.samsung.dallas.networkutils.txrx.Logger;
import com.samsung.dallas.networkutils.txrx.ObjectState;
import com.samsung.dallas.networkutils.txrx.TCPReceiver;
import com.samsung.dallas.networkutils.txrx.TCPServer;
import com.samsung.dallas.networkutils.txrx.TXRXCommand;
import com.samsung.dallas.networkutils.txrx.TransmitterDevice;
import com.samsung.dallas.networkutils.txrx.Util;
import com.samsung.dallas.networkutils.txrx.WorkerRXInterface;

import org.json.JSONObject;

import java.net.Socket;

public class WorkerRXWiFiDirect implements WorkerRXInterface, WiFiDirectUtil.Listener {
    private static final String TAG = "WorkerRXWiFiDirect";
    private static final int DISCOVER_SERVICE_TIMER_MILLIS = 5000;

    private final Handler mHandler;
    private final Logger mLog;

    // WiFi Direct specific begins
    private final WiFiDirectUtil mWiFiDirectUtil;
    // Used in the case we are the group owner
    private final Handler mTransmitterConnectionHandler;
    private final TCPServer mTransmitterConnectionListener;
    private final TransmitterPayloadFilterTCPRX mTransmitterPayloadFilter;
    private boolean mRegisteredForService;
    private final CountDownTimer mDiscoveryTimer;
    private boolean mDiscoveringService;
    // WiFi Direct specific ends

    private int mPort;
    private volatile ObjectState mState;
    private final TCPReceiver mTCPReceiver;

    public WorkerRXWiFiDirect(Handler handler, boolean runInBackground) {
        mHandler = handler;
        mLog = Logger.getInstance();
        mWiFiDirectUtil = new WiFiDirectUtil();
        mDiscoveryTimer = new CountDownTimer(DISCOVER_SERVICE_TIMER_MILLIS, DISCOVER_SERVICE_TIMER_MILLIS) {
            @Override
            public void onTick(long millisUntilFinished) {
            }
            @Override
            public void onFinish() {
                if (mState == ObjectState.STARTED) {
                    if (mLog.isD()) mLog.logD(TAG + ".Timer.onFinish()");
                    if (mWiFiDirectUtil.getServiceTextRecordMap() == null) {
                        mWiFiDirectUtil.discoverService();
                        mDiscoveryTimer.start();
                    }
                }
            }
        };
        mWiFiDirectUtil.setListener(this);
        mTransmitterConnectionHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                doHandleTransmitterMessage(msg);
            }
        };
        mTransmitterConnectionListener = new TCPServer(mTransmitterConnectionHandler);
        mTransmitterPayloadFilter = new TransmitterPayloadFilterTCPRX(mTransmitterConnectionHandler);
        mTCPReceiver = new TCPReceiver(handler);
        mState = ObjectState.IDLE;
    }

    @Override
    public void setDeviceName(String deviceName) {
        mTCPReceiver.setDeviceName(deviceName);
    }

    @Override
    public void setAcceptPayloadFromAll(boolean allow) {
        // No Impl yet
    }

    @Override
    public void start(Context context, int port) {
        if (mState == ObjectState.IDLE) {
            if (mLog.isI()) mLog.logI(TAG + ".start() port:" + port);
            mState = ObjectState.STARTING;
            mPort = port;
            doStart(context);
        }
    }

    @Override
    public void stop() {
        if ((mState == ObjectState.STARTING) || (mState == ObjectState.STARTED)) {
            if (mLog.isI()) mLog.logI(TAG + ".stop()");
            doStop();
        }
    }

    @Override
    public void onWiFiP2PStateChanged(int wiFiP2PState) {
        if (mLog.isD()) mLog.logD(TAG  + ".onWiFiP2PStateChanged() state " + mState);
        if (mState == ObjectState.STARTING) {
            if (wiFiP2PState == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                if (mLog.isI()) mLog.logI(TAG + ".onWiFiP2PStateChanged() WiFi P2P Enabled");
                mState = ObjectState.STARTED;
                mHandler.sendEmptyMessage(Constants.MessageId.MSG_START_RESULT_OK);
            } else {
                if (mLog.isI()) mLog.logI(TAG + ".onWiFiP2PStateChanged() WiFi P2P Disabled");
                doStop();
                mHandler.sendEmptyMessage(Constants.MessageId.MSG_START_RESULT_NOK);
            }
        } else if (mState == ObjectState.STARTED) {
            if (wiFiP2PState == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
                if (mLog.isI()) mLog.logI(TAG + ".onWiFiP2PStateChanged() WiFi P2P Disabled");
                doStop();
                mHandler.sendEmptyMessage(Constants.MessageId.MSG_FATAL_ERROR);
            }
        } else {
            if (mLog.isE()) mLog.logE(TAG + ".onWiFiP2PStateChanged() Unhandled state " + wiFiP2PState);
        }
    }

    @Override
    public void onWiFiP2PGroupStateChanged(WifiP2pInfo wifiP2pInfo, NetworkInfo networkInfo) {
        if (mLog.isI()) mLog.logI(TAG  + ".onWiFiP2PGroupStateChanged() state " + mState
                + ", wifiP2PInfo.groupFormed:" + wifiP2pInfo.groupFormed
                + ", wifiP2pInfo.isGroupOwner:" + wifiP2pInfo.isGroupOwner
                + ", wifiP2pInfo.groupOwnerAddress:" + wifiP2pInfo.groupOwnerAddress);
        if (mState == ObjectState.STARTED) {
            if (wifiP2pInfo.groupFormed) {
                if (wifiP2pInfo.isGroupOwner) {
                    if (mLog.isI()) mLog.logI(TAG + ".onWiFiP2PGroupStateChanged() Listening for transmitter payload on port " + mPort);
                    // Start listener for presenter. Once presenter is connected, we will receive
                    // Constants.MessageId.MSG_TCP_PAYLOAD_RECEIVED with payload
                    mTransmitterConnectionListener.start(mPort);
                    mTransmitterPayloadFilter.start();
                } else {
                    if (mLog.isI()) mLog.logI(TAG + ".onWiFiP2PGroupStateChanged() discovering service... ");
                    doStartDiscovering();
                }
            } else {
                doStopDiscovering();
                doUnRegisterService();
                mTransmitterConnectionListener.stop();
                mTransmitterPayloadFilter.stop();
            }
        }
    }

    @Override
    public void onServicePayload(String payload) {
        if (mLog.isI()) mLog.logI(TAG + ".onServicePayload() Payload:" + payload);
        if (mState == ObjectState.STARTED) {
            doStartTCPReceiver(payload);
        }
    }

    private void doStart(Context context) {
        // Check if WiFi Direct is enabled
        mWiFiDirectUtil.startMonitoring(context, Looper.myLooper());
    }

    private void doStop() {
        mState = ObjectState.IDLE;

        doStopDiscovering();
        doUnRegisterService();
        mWiFiDirectUtil.stopMonitoring();

        mTCPReceiver.stop();
        mTransmitterConnectionListener.stop();
        mTransmitterPayloadFilter.stop();
    }

    private void doHandleTransmitterMessage(Message msg) {
        if (mState == ObjectState.STARTED) {
           switch (msg.what) {
               case Constants.MessageId.MSG_TCP_CLIENT_CONNECTED:
                   if (mLog.isD()) mLog.logD(TAG + ".doHandleTransmitterMessage() TCP client connected.");
                   mTransmitterPayloadFilter.addClient((Socket) msg.obj);
                   break;
               case Constants.MessageId.MSG_FATAL_ERROR:
                   // Just forward message to client
                   if (mLog.isE()) mLog.logE(TAG + ".doHandleTransmitterMessage() Fatal error");
                   doStop();
                   mHandler.sendEmptyMessage(Constants.MessageId.MSG_FATAL_ERROR);
                   break;
               case Constants.MessageId.MSG_TCP_PAYLOAD_RECEIVED:
                   final String payload = (String) msg.obj;
                   if (mLog.isD()) mLog.logD(TAG + ".doHandleTransmitterMessage() Payload received." + payload);
                   // Now we have received payload for register service
                   mTransmitterConnectionListener.stop();
                   mTransmitterPayloadFilter.stop();
                   // The first transmitter wins
                   final boolean registeredForService = this.mRegisteredForService;
                   doRegisterService(payload);
                   // If we registered for service successfully
                   if (!registeredForService && mRegisteredForService) {
                       doStartTCPReceiver(payload);
                   }
           }
        }
    }

    private void doStartTCPReceiver(String payload) {
        if (mLog.isD()) mLog.logD(TAG + ".doStartTCPReceiver() ");
        final Pair<TXRXCommand, String> pair = Util.parseTXRXCommand(payload);
        final String jsonStr = ((pair != null) && (pair.first == TXRXCommand.BROADCAST_TX)) ? pair.second : null;
        final JSONObject jsonObject = Util.createJSON(jsonStr);
        if ((jsonObject != null) && !TextUtils.isEmpty(jsonObject.optString("ip")) && (jsonObject.optInt("sendPort") != 0)) {
            final TransmitterDevice device = Util.create(Util.createJSON(jsonStr));
            final boolean started = mTCPReceiver.start(device);
            if (!started) {
                mHandler.sendEmptyMessage(Constants.MessageId.MSG_FATAL_ERROR);
            }
        }
    }

    // Discover service is a one shot call. If service is register after discover call
    // has been made, we don't get service discovered callback. Hence timer to periodically
    // kick-off service discovery...
    private void doStartDiscovering() {
        if (mLog.isD()) mLog.logD(TAG + ".doStartDiscovering() mDiscoveringService:" + mDiscoveringService);
        if (!mDiscoveringService) {
            mWiFiDirectUtil.discoverService();
            mDiscoveryTimer.start();
            mDiscoveringService = true;
        }
    }

    private void doStopDiscovering() {
        if (mLog.isD()) mLog.logD(TAG + ".doStopDiscovering() mDiscoveringService:" + mDiscoveringService);
        if (mDiscoveringService) {
            mWiFiDirectUtil.cancelDiscoverService();
            mDiscoveryTimer.cancel();
            mDiscoveringService = false;
        }
    }

    private void doRegisterService(String payload) {
        if (mLog.isD()) mLog.logD(TAG + ".doRegisterService() mRegisteredForService:" + mRegisteredForService);
        if (!mRegisteredForService) {
            mWiFiDirectUtil.registerService(payload);
            mRegisteredForService = true;
        }
    }

    private void doUnRegisterService() {
        if (mLog.isD()) mLog.logD(TAG + ".doUnRegisterService() mRegisteredForService:" + mRegisteredForService);
        if (mRegisteredForService) {
            mWiFiDirectUtil.unregisterService();
            mRegisteredForService = false;
        }
    }
}
