package com.samsung.dallas.networkutils.txrx;

import android.content.Context;
import android.os.Handler;

import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class UDPTransmitter {
    private static final String TAG = "UDPTransmitter";

    private final Handler mHandler;
    private final Logger mLog;

    private ObjectState mState;
    private WeakReference<Context> mContextRef;
    private MulticastSocket mTransmitterSocket = null;
    private InetAddress mWifiDHCPInetAddress = null;
    private int mPort;

    public UDPTransmitter(Handler handler) {
        mHandler = handler;
        mLog = Logger.getInstance();
        mState = ObjectState.IDLE;
    }

    /**
     * Starts UDP transmitter on specified port.
     *
     * @param context
     * @param port
     * @return true if started successfully or it has already been started.
     */
    public boolean start(Context context, int port) {
        if (mState == ObjectState.IDLE) {
            if (mLog.isI()) mLog.logI(TAG + ".start() port:" + port);
            try {
                mContextRef = new WeakReference<Context>(context);
                mTransmitterSocket = new MulticastSocket();
                mTransmitterSocket.setBroadcast(true);
                mTransmitterSocket.setReuseAddress(true);
                mPort = port;
                mState = ObjectState.STARTED;
            } catch (Exception e) {
                mLog.logE(TAG + ".start(): Error creating");
                doClose();
            }
        }
        return mState == ObjectState.STARTED;
    }

    public void broadcastPayload(String payload) {
        if (mState == ObjectState.STARTED) {
            if (mWifiDHCPInetAddress == null) {
                final Context context = mContextRef != null ? mContextRef.get() : null;
                mWifiDHCPInetAddress = Util.getWiFiDHCPBroadcastAddress(context);
            }
            if (mWifiDHCPInetAddress != null) {
                try {
                    if (mLog.isV()) {
                        mLog.logV(TAG + ".broadcastPayload() payload:" + payload);
                    } else if (mLog.isD()) {
                        mLog.logD(TAG + ".broadcastPayload() payload.length:" + payload.length());
                    }
                    final byte[] e = payload.getBytes(Util.PAYLOAD_CHARSET);
                    final DatagramPacket sendPacket = new DatagramPacket(e, e.length, mWifiDHCPInetAddress, mPort);
                    mTransmitterSocket.send(sendPacket);
                } catch (Exception e) {
                    if (mLog.isE()) mLog.logE(TAG + ".broadcastPayload(): " + e.getMessage());
                    mHandler.sendEmptyMessage(Constants.MessageId.MSG_FATAL_ERROR);
                }
            } else {
                if (mLog.isE()) mLog.logE(TAG + ".broadcastPayload(): Error obtaining broadcast address");
            }
        }
    }

    public void stop() {
        if (mState == ObjectState.STARTED) {
            if (mLog.isI()) mLog.logI(TAG + ".stop()");
            doClose();
            mState = ObjectState.IDLE;
        }
    }

    private void doClose() {
        Util.safeClose(mTransmitterSocket);
        mTransmitterSocket = null;
        mWifiDHCPInetAddress = null;
        mPort = 0;
    }
}
