package com.samsung.dallas.networkutils.txrx.udp;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.samsung.dallas.networkutils.txrx.Logger;
import com.samsung.dallas.networkutils.txrx.ObjectState;
import com.samsung.dallas.networkutils.txrx.TXRXDevice;
import com.samsung.dallas.networkutils.txrx.UDPTransmitter;
import com.samsung.dallas.networkutils.txrx.WorkerTXInterface;

import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Vasudevan Ramachandraiah on 7/21/17.
 */

public class WorkerTXUDP implements WorkerTXInterface {
    private static final String TAG = "WorkerTXUDP";

    private static final int MSG_START = 3100;
    private static final int MSG_STOP = 3101;
    private static final int MSG_SEND_BROADCAST_PAYLOAD = 3102;

    private final Handler mHandler;
    private final UDPTransmitter mPayloadBroadcaster;
    private final List<String> mBroadcastItems;
    private final Logger mLog;

    private WeakReference<Context> mContextRef;
    private int mBroadcastPort;

    private volatile ObjectState mState;
    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;

    public WorkerTXUDP(Handler handler) {
        mHandler = handler;
        mPayloadBroadcaster = new UDPTransmitter(handler);
        mBroadcastItems = new ArrayList<String>();
        mLog = Logger.getInstance();
        mState = ObjectState.IDLE;
    }

    @Override
    public void setDeviceName(String deviceName) {
    }

    @Override
    public void addToWhiteList(String deviceName) {
    }

    @Override
    public void removeFromWhiteList(String deviceName) {
    }

    @Override
    public void clearWhiteList() {
    }

    @Override
    public List<String> getWhiteList() {
        return null;
    }

    @Override
    public void setCollisionDetectionEnabled(boolean enabled) {
    }

    @Override
    public void setMaxConnections(int max) {
    }

    @Override
    public boolean start(Context context, int handshakePort, int sendPort, int broadcastPort) {
        if (mState == ObjectState.IDLE) {
            if (mLog.isI()) mLog.logI(TAG + ".start()");

            mContextRef = new WeakReference<Context>(context);
            mBroadcastPort = broadcastPort;

            mWorkerThread = new HandlerThread("WorkerTXUDP");
            mWorkerThread.start();
            mWorkerHandler = new Handler(mWorkerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    doHandleMessage(msg);
                }
            };
            mWorkerHandler.sendEmptyMessage(MSG_START);
            mState = ObjectState.STARTING;
        }
        return true;
    }

    @Override
    public void sendPayload(String payload) {
        if (mState == ObjectState.STARTED) {
            if (mLog.isE()) mLog.logE(TAG + ".sendPaylaod() Unsupported method.");
        }
    }

    @Override
    public void sendPayload(TXRXDevice device, String payload) {
        if (mState == ObjectState.STARTED) {
            if (mLog.isE()) mLog.logE(TAG + ".sendPaylaod() Unsupported method.");
        }
    }

    @Override
    public void broadcastPayload(String payload) {
        if (mState == ObjectState.STARTED) {
            // We are adding in one thread and removing in another. So synchronize modifiers
            synchronized (mBroadcastItems) {
                mBroadcastItems.add(payload);
            }
            if (!mWorkerHandler.hasMessages(MSG_SEND_BROADCAST_PAYLOAD)) {
                mWorkerHandler.sendEmptyMessage(MSG_SEND_BROADCAST_PAYLOAD);
            }
        }
    }

    @Override
    public void addTCPClient(Socket obj) {
    }

    @Override
    public void stop() {
        if ((mState == ObjectState.STARTING) || (mState == ObjectState.STARTED)) {
            if (mLog.isI()) mLog.logI(TAG + ".stop()");
            mWorkerHandler.removeMessages(MSG_SEND_BROADCAST_PAYLOAD);
            mWorkerHandler.sendEmptyMessage(MSG_STOP);
            mWorkerThread.quitSafely();
            synchronized (mBroadcastItems) {
                mBroadcastItems.clear();
            }
            mState = ObjectState.IDLE;
        }
    }

    private void doHandleMessage(Message msg) {
        switch (msg.what) {
            case MSG_START:
                onMessageStart();
                break;
            case MSG_STOP:
                onMessageStop();
                break;
            case MSG_SEND_BROADCAST_PAYLOAD:
                onMessageBroadcastPayload();
                break;
        }
    }

    private void onMessageStart() {
        if (mLog.isD()) mLog.logD(TAG + ".onMessageStart()");
        final Context context = mContextRef.get();
        mPayloadBroadcaster.start(context, mBroadcastPort);
    }

    private void onMessageStop() {
        if (mLog.isD()) mLog.logD(TAG + ".onMessageStop()");
        mPayloadBroadcaster.stop();
    }

    private void onMessageBroadcastPayload() {
        String broadcastPayload = null;
        int broadcastItemCount = 0;
        synchronized (mBroadcastItems) {
            broadcastItemCount = mBroadcastItems.size();
            if (broadcastItemCount > 0) {
                broadcastPayload = mBroadcastItems.remove(0);
            }
        }
        if (broadcastPayload != null) {
            if (mLog.isV()) mLog.logV(TAG + ".onMessageBroadcastPayload() broadcast payload.length:" + broadcastPayload.length());
            mPayloadBroadcaster.broadcastPayload(broadcastPayload);
            broadcastItemCount--;
        }
        if ((broadcastItemCount > 0) && !mWorkerHandler.hasMessages(MSG_SEND_BROADCAST_PAYLOAD)) {
            mWorkerHandler.sendEmptyMessage(MSG_SEND_BROADCAST_PAYLOAD);
        }
    }
}
