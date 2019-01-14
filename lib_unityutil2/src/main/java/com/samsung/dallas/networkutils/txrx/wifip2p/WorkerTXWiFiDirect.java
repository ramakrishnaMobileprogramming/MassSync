package com.samsung.dallas.networkutils.txrx.wifip2p;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.samsung.dallas.networkutils.txrx.Constants;
import com.samsung.dallas.networkutils.txrx.Logger;
import com.samsung.dallas.networkutils.txrx.ObjectState;
import com.samsung.dallas.networkutils.txrx.TCPServer;
import com.samsung.dallas.networkutils.txrx.TCPTransmitter;
import com.samsung.dallas.networkutils.txrx.TXRXDevice;
import com.samsung.dallas.networkutils.txrx.Util;
import com.samsung.dallas.networkutils.txrx.WorkerTXInterface;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class WorkerTXWiFiDirect implements WorkerTXInterface, WiFiDirectUtil.Listener {
    private static final String TAG = "WorkerTXWiFiDirect";
    private static final int MSG_SEND_PAYLOAD = 2100;
    private static final int MSG_STOP = 2101;

    private final Handler mHandler;
    private final Logger mLog;

    // WiFi Direct specific begins
    private final WiFiDirectUtil mWiFiDirectUtil;
    // Used to send transmitter record to group owner if this is not group owner
    private final TransmitterPayloadTCPTX mTransmitterPayloadTCPTX;
    private boolean mRegisteredForService;
    // WiFi Direct specific ends

    private String mDeviceName;
    private int mHandshakePort;
    private int mPort;
    private volatile ObjectState mState;
    private final TCPServer mTCPServer; // Listen to receivers socket connections
    private final TCPTransmitter mPayloadSender; // Sends payload via TCPIP
    private int mMaxConnections;
    private final List<String> mSendItems;
    private final List<String> mBroadcastItems;
    private final Handler mWorkerHandler;

    public WorkerTXWiFiDirect(Handler handler) {
        mHandler = handler;
        mLog = Logger.getInstance();
        mWiFiDirectUtil = new WiFiDirectUtil();
        mWiFiDirectUtil.setListener(this);
        mWorkerHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                doHandleWorkerMessage(msg);
            }
        };
        mTransmitterPayloadTCPTX = new TransmitterPayloadTCPTX(mWorkerHandler);
        mTCPServer = new TCPServer(handler);
        mPayloadSender = new TCPTransmitter(handler, true);
        mSendItems = new ArrayList<String>();
        mBroadcastItems = new ArrayList<String>();
        mState = ObjectState.IDLE;
    }

    @Override
    public void setDeviceName(String deviceName) {
        this.mDeviceName = deviceName;
    }

    @Override
    public void addToWhiteList(String deviceName) {
        mPayloadSender.addToWhiteList(deviceName);
    }

    @Override
    public void removeFromWhiteList(String deviceName) {
        mPayloadSender.removeFromWhiteList(deviceName);
    }

    @Override
    public void clearWhiteList() {
        mPayloadSender.clearWhiteList();
    }

    @Override
    public List<String> getWhiteList() {
        return mPayloadSender.getWhiteList();
    }

    @Override
    public void setCollisionDetectionEnabled(boolean enabled) {
    }

    @Override
    public void setMaxConnections(int max) {
        mMaxConnections = max;
    }

    @Override
    public boolean start(Context context, int handshakePort, int sendPort, int broadcastPort) {
        if (mState == ObjectState.IDLE) {
            if (mLog.isI()) mLog.logI(TAG + ".start() handshakePort: " + handshakePort + ", sendPort:" + sendPort);
            mHandshakePort = handshakePort;
            mPort = sendPort;
            mState = ObjectState.STARTING;
            doStart(context);
        }
        return true;
    }

    @Override
    public void sendPayload(String payload) {
        if (mState == ObjectState.STARTED) {
            // We don't know from what thread context client calls this function, but we are
            // removing in UI thread (default Handler looper). So synchronize modifiers
            synchronized (mSendItems) {
                mSendItems.add(payload);
            }
            if (!mWorkerHandler.hasMessages(MSG_SEND_PAYLOAD)) {
                mWorkerHandler.sendEmptyMessage(MSG_SEND_PAYLOAD);
            }
        }
    }

    @Override
    public void sendPayload(TXRXDevice device, String payload) {
        if (mState == ObjectState.STARTED) {
            mPayloadSender.sendPayload(device, payload);
        }
    }

    @Override
    public void broadcastPayload(String payload) {
        if (mState == ObjectState.STARTED) {
            // We don't know from what thread context client calls this function, but we are
            // removing in UI thread (default Handler looper). So synchronize modifiers
            synchronized (mBroadcastItems) {
                mBroadcastItems.add(payload);
            }
            if (!mWorkerHandler.hasMessages(MSG_SEND_PAYLOAD)) {
                mWorkerHandler.sendEmptyMessage(MSG_SEND_PAYLOAD);
            }
        }
    }

    @Override
    public void addTCPClient(Socket socket) {
        if (mLog.isD()) mLog.logD(TAG + ".addTCPClient()");
        if (mPayloadSender.getReceiversCount() < mMaxConnections) {
            if (!mPayloadSender.addTCPClient(socket)) {
                if (mLog.isE()) mLog.logE(TAG + ".addTCPClient() Error adding TCP client socket");
                Util.safeClose(socket);
            }
        } else { // We have reached maximum number of connections.
            if (mLog.isI()) mLog.logI(TAG + ".onMessageAddTCPClient() Max connection " + mMaxConnections
                    + " reached. Refusing connection request from IP:" + socket.getInetAddress().getHostName()
                    + ". Shutting down TCPServer...");
            Util.safeClose(socket);
            mTCPServer.stop();
        }
    }

    @Override
    public void stop() {
        if ((mState == ObjectState.STARTING) || (mState == ObjectState.STARTED)) {
            if (mLog.isI()) mLog.logI(TAG + ".stop()");
            doStop(false);
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
                doStop(true);
                mHandler.sendEmptyMessage(Constants.MessageId.MSG_START_RESULT_NOK);
            }
        } else if (mState == ObjectState.STARTED) {
            if (wiFiP2PState == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
                if (mLog.isI()) mLog.logI(TAG + ".onWiFiP2PStateChanged() WiFi P2P Disabled");
                doStop(true);
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
                // Group owner ip address starts with a '/'. Remove this leading forward slash
                final String groupOwnerIp = wifiP2pInfo.groupOwnerAddress.toString().substring(1);
                // Don't assign IP address here. This payload is just for group owner. The group owner will
                // extract presenter IP and fill the payload for the registered service correctly.
                final String payloadIP = wifiP2pInfo.isGroupOwner ? groupOwnerIp : "";
                // Since we don't have a global UTC, we rely on OS's default EPOC time in UTC. Ideally
                // users device is configured to right time.
                final String payload = Util.createPayloadBroadcastTransmitter(payloadIP, mDeviceName, mPort, -1, System.currentTimeMillis(), true);
                if (wifiP2pInfo.isGroupOwner) {
                    if (mLog.isI()) mLog.logI(TAG + ".onWiFiP2PGroupStateChanged() registering service... ");
                    doRegisterService(payload);
                    doStartTransmitter();
                } else {
                    if (mLog.isI()) mLog.logI(TAG + ".onWiFiP2PGroupStateChanged() Trying to write transmitter payload on port " + mHandshakePort);
                    mTransmitterPayloadTCPTX.start(groupOwnerIp, mHandshakePort, payload);
                }
            } else {
                doUnRegisterService();
                mTransmitterPayloadTCPTX.stop();
                doStopTransmitter();
            }
        }
    }

    @Override
    public void onServicePayload(String payload) {
        if (mLog.isI()) mLog.logI(TAG + ".onServicePayload() Payload:" + payload);
    }

    private void doStart(Context context) {
        if (mLog.isI()) mLog.logI(TAG + ".doStart()");
        // Check if WiFi Direct is enabled
        mWiFiDirectUtil.startMonitoring(context, Looper.myLooper());
    }

    private void doStartTransmitter() {
        mTCPServer.start(mPort);
        mPayloadSender.start();
    }

    private void doStopTransmitter() {
        mTCPServer.stop();
        mPayloadSender.stop();
    }

    /**
     * Stops worker sender.
     *
     * If forceStop is false, all payload items queued up via sendPayload() will be sent to the
     * connected receiver(s) before shutting down the worker. Until then worker will be in state
     * STARTED.
     *
     * If forceStop is true, all items queued up are discarded and worker will change to state IDLE
     * immediately.
     *
     * @param forceStop
     */
    private void doStop(boolean forceStop) {
        if (mLog.isI()) mLog.logI(TAG + ".doStop() forceStop:" + forceStop);

        doUnRegisterService();
        mWiFiDirectUtil.stopMonitoring();

        mTransmitterPayloadTCPTX.stop(); // Stop Transmitter payload sender
        mTCPServer.stop(); // Stop accepting connections from receivers

        synchronized (mBroadcastItems) {
            mBroadcastItems.clear();
        }

        // For all practical purposes we are stopped even though we are sending pending items
        // to receivers
        mState = ObjectState.IDLE;

        if (forceStop) {
            synchronized (mSendItems) {
                mSendItems.clear();
            }
            mPayloadSender.stop();
        } else {
            mWorkerHandler.sendEmptyMessage(MSG_STOP);
        }
    }

    private void doRegisterService(String payload) {
        if (mLog.isI()) mLog.logI(TAG + ".doRegisterService() mRegisteredForService:" + mRegisteredForService);
        if (!mRegisteredForService) {
            mWiFiDirectUtil.registerService(payload);
            mRegisteredForService = true;
        }
    }

    private void doUnRegisterService() {
        if (mLog.isI()) mLog.logI(TAG + ".doUnRegisterService() mRegisteredForService:" + mRegisteredForService);
        if (mRegisteredForService) {
            mWiFiDirectUtil.unregisterService();
            mRegisteredForService = false;
        }
    }

    private void doHandleWorkerMessage(Message msg) {
        switch (msg.what) {
            case Constants.MessageId.MSG_HANDSHAKE_SENT:
                if (mState == ObjectState.STARTED) {
                    mTransmitterPayloadTCPTX.stop();
                    doStartTransmitter();
                }
                break;
            case MSG_SEND_PAYLOAD:
                if (mState == ObjectState.STARTED) {
                    doHandleWorkerProcessPayloadItems();
                }
                break;
            case MSG_STOP:
                // Continue handling stop message
                doHandleWorkerStopMessage();
                break;
        }
    }

    /**
     * Sends first item from mSendItems list. If mSendItems list is empty, sends first item
     * from mBroadcastItems list. All items are sent via TCP.
     *
     * Note: This could starve broadcast payload if transmitter is bombarded with send payload.
     * At the same time, this scheme will make sure send payload gets higher priority than broadcast
     * payload
     */
    private void doHandleWorkerProcessPayloadItems() {
        String sendPayload = null;
        int sendItemCount = 0;
        synchronized (mSendItems) {
            sendItemCount = mSendItems.size();
            if (sendItemCount > 0) {
                sendPayload = mSendItems.remove(0);
            }
        }
        String broadcastPayload = null;
        int broadcastItemCount = 0;
        if (sendPayload != null) {
            if (mLog.isV()) mLog.logV(TAG + ".doHandleWorkerProcessPayloadItems() send payload.length:" + sendPayload.length());
            mPayloadSender.sendPayload(sendPayload);
            sendItemCount--;
            if (sendItemCount == 0) { // mSendItems has become empty
                synchronized (mBroadcastItems) {
                    broadcastItemCount = mBroadcastItems.size();
                }
            }
        } else { // mSendItems is empty
            synchronized (mBroadcastItems) {
                broadcastItemCount = mBroadcastItems.size();
                if (broadcastItemCount > 0) {
                    broadcastPayload = mBroadcastItems.remove(0);
                }
            }
            if (broadcastPayload != null) {
                if (mLog.isV()) mLog.logV(TAG + ".doHandleWorkerProcessPayloadItems() broadcast payload.length:" + broadcastPayload.length());
                mPayloadSender.sendPayload(broadcastPayload);
                broadcastItemCount--;
            }
        }
        if (!mWorkerHandler.hasMessages(MSG_SEND_PAYLOAD) &&
                ((sendItemCount >= 1) || (broadcastItemCount >= 1))) {
            mWorkerHandler.sendEmptyMessage(MSG_SEND_PAYLOAD);
        }
    }

    private void doHandleWorkerStopMessage() {
        // Check to see if there are any pending payload items
        int sendItemCount;
        synchronized (mSendItems) {
            sendItemCount = mSendItems.size();
        }
        int broadcastItemCount = 0;
        if (sendItemCount == 0) {
            synchronized (mBroadcastItems) {
                broadcastItemCount = mBroadcastItems.size();
            }
        }

        if ((sendItemCount > 0) || (broadcastItemCount > 0)) {
            if (mLog.isV()) mLog.logV(TAG + ".doHandleWorkerStopMessage() processing pending items...");
            doHandleWorkerProcessPayloadItems();
            mWorkerHandler.sendEmptyMessage(MSG_STOP);
        } else { // No more pending items.
            if (mLog.isV()) mLog.logV(TAG + ".doHandleWorkerStopMessage() Stopping PayloadSender");
            mPayloadSender.stop();
            mState = ObjectState.IDLE;
        }
    }
}
