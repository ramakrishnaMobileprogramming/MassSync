package com.samsung.dallas.networkutils.txrx.udp;

import android.content.Context;
import android.os.Handler;

import com.samsung.dallas.networkutils.txrx.Constants;
import com.samsung.dallas.networkutils.txrx.Logger;
import com.samsung.dallas.networkutils.txrx.ObjectState;
import com.samsung.dallas.networkutils.txrx.UDPReceiver;
import com.samsung.dallas.networkutils.txrx.WorkerRXInterface;

/**
 * Created by Vasudevan Ramachandraiah on 7/21/17.
 */

public class WorkerRXUDP implements WorkerRXInterface {
    private static final String TAG = "WorkerRXUDP";

    private final Handler mHandler;
    private final Logger mLog;

    private volatile ObjectState mState;
    private final UDPReceiver mUDPReceiver;

    public WorkerRXUDP(Handler handler) {
        mHandler = handler;
        mLog = Logger.getInstance();
        mState = ObjectState.IDLE;
        mUDPReceiver = new UDPReceiver(handler);
    }

    @Override
    public void setDeviceName(String deviceName) {
    }

    @Override
    public void setAcceptPayloadFromAll(boolean allow) {
    }

    @Override
    public void start(Context context, int broadcastPort) {
        if (mState == ObjectState.IDLE) {
            if (mLog.isI()) mLog.logI(TAG + ".start() broadcastPort:" + broadcastPort);
            final boolean start = mUDPReceiver.start(context, broadcastPort, Constants.MessageId.MSG_UDP_PAYLOAD_RECEIVED);
            mHandler.sendEmptyMessage(start ? Constants.MessageId.MSG_START_RESULT_OK : Constants.MessageId.MSG_START_RESULT_NOK);
        }
    }

    @Override
    public void stop() {
        mUDPReceiver.stop();
    }
}
