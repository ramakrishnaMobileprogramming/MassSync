package com.samsung.dallas.networkutils.txrx.wifip2p;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.samsung.dallas.networkutils.txrx.Constants;
import com.samsung.dallas.networkutils.txrx.Logger;
import com.samsung.dallas.networkutils.txrx.ObjectState;
import com.samsung.dallas.networkutils.txrx.Util;

import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 *
 * Sends Payload to host:port specified.
 *
 * If payload is not sent, after preset duration, payload is sent again.
 * If payload is sent successfully, nothing more happens.
 *
 */
public class TransmitterPayloadTCPTX {
    private static final String TAG = "TransmitterPayloadTCPTX";
    private final long POST_RETRY_INTERVAL_MILLIS = 5000;
    private final int MSG_POST_PAYLOAD = 1;

    private final Handler mHandler;
    private final Logger mLog;

    private String mHost;
    private int mPort;
    private String mHandShakePayload;

    private ObjectState mState;
    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;

    public TransmitterPayloadTCPTX(Handler handler) {
        mHandler = handler;
        mLog = Logger.getInstance();
        mState = ObjectState.IDLE;
    }

    public boolean start(String host, int port, String handShakePayload) {
        boolean result = false;
        if (mState == ObjectState.IDLE) {
            if (mLog.isI()) mLog.logI(TAG + ".start() host:" + host + ", port:" + port
                    + ", payload.length:" + (handShakePayload != null ? handShakePayload.length() : "null"));
            mHost = host;
            mPort = port;
            mHandShakePayload = handShakePayload;
            mWorkerThread = new HandlerThread("TransmitterPayloadTCPTX.WorkerThread");
            mWorkerThread.start();
            mWorkerHandler = new Handler(mWorkerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    doHandleMessage(msg);
                }
            };
            mWorkerHandler.sendEmptyMessage(MSG_POST_PAYLOAD);
            mState = ObjectState.STARTED;
        }
        return result;
    }

    public void stop() {
        if (mState == ObjectState.STARTED) {
            if (mLog.isI()) mLog.logI(TAG + ".stop()");
            mWorkerHandler.removeMessages(MSG_POST_PAYLOAD);
            mWorkerThread.quitSafely();
            mWorkerThread = null;
            mWorkerHandler = null;
            mState = ObjectState.IDLE;
        }
    }

    private void doHandleMessage(Message msg) {
        switch (msg.what) {
            case MSG_POST_PAYLOAD:
                doPostPayload();
                break;
            default:
                break;
        }
    }

    private void doPostPayload() {
        //if (mLog.isD()) mLog.logD(TAG + ".doPostPayload()");
        boolean posted = false;
        final Util.SocketHolder sh = new Util.SocketHolder();
        try {
            Socket mSocket = new Socket();
            mSocket.bind(null);
            mSocket.connect(new InetSocketAddress(mHost, mPort));
            sh.dataOutputStream = new DataOutputStream(mSocket.getOutputStream());
            final byte[] bytes = mHandShakePayload.getBytes(Util.PAYLOAD_CHARSET);
            if (mLog.isD()) mLog.logD(TAG + ".doPostPayload() writing payload.length:" + mHandShakePayload.length()
                    + ". bytes.length:" + bytes.length);
            sh.dataOutputStream.writeInt(bytes.length);
            sh.dataOutputStream.write(bytes);
            posted = true;
            mHandler.sendEmptyMessage(Constants.MessageId.MSG_HANDSHAKE_SENT);
        } catch (Exception e) {
            if (mLog.isE()) mLog.logE(TAG + ".doPostPayload() Error posting payload. Exception:" + e.getMessage());
        } finally {
            Util.safeClose(sh);
        }
        if (!posted) {
            mWorkerHandler.sendEmptyMessageDelayed(MSG_POST_PAYLOAD, POST_RETRY_INTERVAL_MILLIS);
        }
    }
}
