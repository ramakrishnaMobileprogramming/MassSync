package com.samsung.dallas.networkutils.txrx.udptcp;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;

import com.samsung.dallas.networkutils.txrx.Constants;
import com.samsung.dallas.networkutils.txrx.Logger;
import com.samsung.dallas.networkutils.txrx.ObjectState;
import com.samsung.dallas.networkutils.txrx.TCPReceiver;
import com.samsung.dallas.networkutils.txrx.TXRXCommand;
import com.samsung.dallas.networkutils.txrx.TransmitterDevice;
import com.samsung.dallas.networkutils.txrx.UDPReceiver;
import com.samsung.dallas.networkutils.txrx.Util;
import com.samsung.dallas.networkutils.txrx.WorkerRXInterface;

import java.lang.ref.WeakReference;

public class WorkerRXUDPTCP implements WorkerRXInterface {
    private static final String TAG = "WorkerRXUDPTCP";

    private final Handler mHandler;
    private final Logger mLog;

    private volatile ObjectState mState;
    private boolean mAcceptPayloadFromAll;
    private WeakReference<Context> mContextRef;
    private int mTxShakeHandPort;

    private Handler mWorkerHandler;

    private final UDPReceiver mUDPReceiver;
    private final TCPReceiver mTCPReceiver;
    private TransmitterDevice mTransmitterDevice;

    public WorkerRXUDPTCP(Handler handler, boolean runInBackground) {
        mHandler = handler;
        mLog = Logger.getInstance();
        mWorkerHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                workerHandleMessage(msg);
            }
        };
        mUDPReceiver = new UDPReceiver(mWorkerHandler);
        mTCPReceiver = new TCPReceiver(handler);
        mState = ObjectState.IDLE;
    }

    @Override
    public void setDeviceName(String deviceName) {
        mTCPReceiver.setDeviceName(deviceName);
    }

    @Override
    public void setAcceptPayloadFromAll(boolean allow) {
        if (mState == ObjectState.IDLE) {
            mAcceptPayloadFromAll = allow;
        } else {
            if (mLog.isE()) mLog.logE(TAG + ".setAcceptPayloadFromAll() Invalid state " + mState);
        }
    }

    @Override
    public void start(Context context, int txShakeHandPort) {
        if (mState == ObjectState.IDLE) {
            if (mLog.isI()) mLog.logI(TAG + ".start()");

            mContextRef = new WeakReference<Context>(context);
            mTxShakeHandPort = txShakeHandPort;

            onMessageStart();
        }
    }

    @Override
    public void stop() {
        if ((mState == ObjectState.STARTING) || (mState == ObjectState.STARTED)) {
            if (mLog.isI()) mLog.logI(TAG + ".stop()");
            onMessageStop();

            mState = ObjectState.IDLE;
        }
    }

    private void onMessageStart() {
        if (mLog.isD()) mLog.logD(TAG + ".onMessageStart()");
        final Context context = mContextRef.get();
        boolean result = context == null ? false : true;
        if (result) {
            // Setup UDP receiver listen to handshake messages
            result = mUDPReceiver.start(context, mTxShakeHandPort, Constants.MessageId.MSG_HANDSHAKE_RECEIVED);
        }
        if (result) {
            mHandler.sendEmptyMessage(Constants.MessageId.MSG_START_RESULT_OK);
        } else {
            mHandler.sendEmptyMessage(Constants.MessageId.MSG_START_RESULT_NOK);
        }
        mState = ObjectState.STARTED;
    }

    private void onMessageStop() {
        if (mLog.isD()) mLog.logD(TAG + ".onMessageStop()");
        mUDPReceiver.stop();
        mUDPReceiver.setHost(null);
        mTCPReceiver.stop();
        mTransmitterDevice = null;
    }

    private void onMessageUDPToTCP(TransmitterDevice device) {
        if (mLog.isD()) mLog.logD(TAG + ".onMessageUDPToTCP() :" + device);
        // Teardown UDP receiver listening to handshake messages
        mUDPReceiver.stop();
        // Setup UDP receiver listen to payload broadcast messages
        final String host = mAcceptPayloadFromAll ? null : device.hostName;
        mUDPReceiver.setHost(host);
        boolean success = mUDPReceiver.start(mContextRef.get(), device.broadcastPort, Constants.MessageId.MSG_UDP_PAYLOAD_RECEIVED);
        if (success) {
            // Start TCP receiver to listen to payload messages
            success =  mTCPReceiver.start(device);
        }
        if (!success) {
            mHandler.sendEmptyMessage(Constants.MessageId.MSG_FATAL_ERROR);
        }
    }

    private void workerHandleMessage(Message msg) {
        switch (msg.what) {
            case Constants.MessageId.MSG_HANDSHAKE_RECEIVED:
                final String hsPayload = (String) msg.obj;
                if (mLog.isD()) mLog.logD(TAG + ".doHandleMessage() UDP handshake payload.length:" + hsPayload.length());
                if (mState == ObjectState.STARTED) {
                    final Pair<TXRXCommand, String> pair = Util.parseTXRXCommand(hsPayload);
                    if ((pair != null) && (pair.first == TXRXCommand.BROADCAST_TX)) {
                        if (mTransmitterDevice == null) {
                            mTransmitterDevice = Util.create(Util.createJSON(pair.second));
                            mTransmitterDevice.handshakePort = mTxShakeHandPort;
                            onMessageUDPToTCP(mTransmitterDevice);
                        } else {
                            if (mLog.isE()) mLog.logE(TAG + ".doHandleMessage() UDP payload Already connected");
                        }
                    }
                }
                break;
            case Constants.MessageId.MSG_UDP_PAYLOAD_RECEIVED:
                mHandler.sendMessage(mHandler.obtainMessage(msg.what, msg.obj));
                break;
            case Constants.MessageId.MSG_RECEIVER_CONNECTED:
                mHandler.sendMessage(mHandler.obtainMessage(msg.what, msg.obj));
                break;
            case Constants.MessageId.MSG_TCP_PAYLOAD_RECEIVED:
                mHandler.sendMessage(mHandler.obtainMessage(msg.what, msg.obj));
                break;
            default:
                break;
        }
    }
}
