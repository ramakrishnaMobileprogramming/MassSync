package com.samsung.dallas.networkutils.txrx;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TCPReceiver {
    private static final String TAG = "TCPReceiver";
    private static final int BUFFER_SIZE = 4096;
    private final Logger mLog;

    private enum ThreadState {
        CONNECT_SOCKET,
        SEND_CLIENT_HANDSHAKE,
        RECEIVE_DATA,
    }

    private final Handler mHandler;
    private final byte[] mBuffer;

    private volatile ObjectState mState;
    private TransmitterDevice mTransmitterDevice;
    private Thread mCommThread;
    private volatile boolean mContinue;
    private Socket mSocket;
    private DataInputStream mDataInputStream;
    private DataOutputStream mDataOutputStream;
    private String mDeviceName;

    public TCPReceiver(Handler handler) {
        mHandler = handler;
        mLog = Logger.getInstance();
        mBuffer = new byte[BUFFER_SIZE];
        mState = ObjectState.IDLE;
    }

    public void setDeviceName(String deviceName) {
        mDeviceName = deviceName;
    }

    public boolean start(TransmitterDevice device) {
        if (mState == ObjectState.IDLE) {
            if (mLog.isI()) mLog.logI(TAG + ".start() Target " + device.hostName + ":" + device.sendPort);
            try {
                mTransmitterDevice = device;

                mSocket = new Socket();
                mSocket.bind(null);

                mCommThread = new Thread(new TCPReceiverImpl(), "TCPReceiver");
                mContinue = true;
                mCommThread.start();
                mState = ObjectState.STARTED;
            } catch (IOException e) {
                if (mLog.isE()) mLog.logE(TAG + ".start() " + e.getMessage());
                doStop();
            }
        }
        return mSocket != null;
    }

    public void stop() {
        if (mLog.isI()) mLog.logI(TAG + ".stop()");
        doStop();
        mState = ObjectState.IDLE;
    }

    private void doStop() {
        // Closes both input and output streams
        Util.safeClose(mSocket);
        mSocket = null;
        mCommThread = null;
        mContinue = false;
    }

    private class TCPReceiverImpl implements Runnable {
        @Override
        public void run() {
            final String registerPayload = Util.createPayloadRegisterReceiver(mDeviceName);

            ThreadState mThreadState = ThreadState.CONNECT_SOCKET;
            String payload;
            Exception exception = null;
            int bytesLength;
            Message message;

            if (mLog.isI()) mLog.logI(TAG + ".run() mContinue:" + mContinue);
            while (mContinue && (exception == null)) {
                exception = null;
                try {
                    switch (mThreadState) {
                        case CONNECT_SOCKET:
                            if (mLog.isD()) mLog.logD(TAG + ".run() setting up socket...");
                            mSocket.connect(new InetSocketAddress(mTransmitterDevice.hostName, mTransmitterDevice.sendPort));
                            mDataInputStream = new DataInputStream(mSocket.getInputStream());
                            mDataOutputStream = new DataOutputStream(mSocket.getOutputStream());
                            mThreadState = ThreadState.SEND_CLIENT_HANDSHAKE;
                            break;
                        case SEND_CLIENT_HANDSHAKE:
                            if (mLog.isD()) mLog.logD(TAG + ".run() writing register receiver payload...");
                            // Write availableLength and handshake message
                            final byte[] bytes = registerPayload.getBytes(Util.PAYLOAD_CHARSET);
                            mDataOutputStream.writeInt(bytes.length);
                            mDataOutputStream.write(bytes);
                            // Set to receive data
                            mThreadState = ThreadState.RECEIVE_DATA;
                            // Send receiver connected message
                            message = mHandler.obtainMessage(Constants.MessageId.MSG_RECEIVER_CONNECTED, mTransmitterDevice);
                            mHandler.sendMessage(message);
                            break;
                        case RECEIVE_DATA:
                            //if (mLog.isD()) mLog.logD(TAG + ".run() reading payload...");
                            bytesLength = Util.readBlocking(mDataInputStream, mBuffer);
                            if  (bytesLength > 0) {
                                payload = new String(mBuffer, 0, bytesLength, Util.PAYLOAD_CHARSET);
                                if (mLog.isV()) {
                                    mLog.logV(TAG + ".run() payload:" + payload);
                                } else if (mLog.isD()) {
                                    mLog.logD(TAG + ".run() payload.length:" + payload.length() + ", bytes.length:" + bytesLength);
                                }
                                message = mHandler.obtainMessage(Constants.MessageId.MSG_TCP_PAYLOAD_RECEIVED, payload);
                                mHandler.sendMessage(message);
                            }
                            break;
                        default:
                            break;
                    }
                } catch (IOException e) {
                    // If we are not timed out reading data OR in receiving data state, report fatal error
                    if (!(e instanceof SocketTimeoutException) || (mThreadState != ThreadState.RECEIVE_DATA)) {
                        exception = e;
                    }
                }
                if ((exception != null) && (mState == ObjectState.STARTED)) {
                    if (mLog.isE()) mLog.logE(TAG + ".run() Exception " + exception.getMessage());
                    if (mLog.isE()) mLog.logE(TAG + ".run() Exception " + Log.getStackTraceString(exception));
                    mHandler.sendEmptyMessage(Constants.MessageId.MSG_FATAL_ERROR);
                    break;
                }
            }
            if (mLog.isI()) mLog.logI(TAG + ".run()<");
        }
    }
}
