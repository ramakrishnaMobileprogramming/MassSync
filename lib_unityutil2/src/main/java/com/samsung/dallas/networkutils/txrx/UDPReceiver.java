package com.samsung.dallas.networkutils.txrx;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class UDPReceiver {
    private static final String TAG = "UDPReceiver";

    private final Handler mHandler;
    private final byte[] mBuffer;
    private final DatagramPacket mDatagramPacket;
    private final Logger mLog;

    private ObjectState mState;
    private WeakReference<Context> mContextRef;
    private int mHandlerMessageId;
    private MulticastSocket mReceiverSocket = null;
    private String mLocalIPAddress;
    private Thread mServerThread;
    private volatile boolean mContinue;
    private String mHost;

    public UDPReceiver(Handler handler) {
        mBuffer = new byte[1024];
        mDatagramPacket = new DatagramPacket(mBuffer, mBuffer.length);
        mHandler = handler;
        mLog = Logger.getInstance();
        mState = ObjectState.IDLE;
    }

    /**
     * If set, accepts payload only from the host specified.
     *
     * Datagram packets from all other hosts are ignored.
     *
     * @param host
     */
    public void setHost(String host) {
        this.mHost = host;
    }

    public boolean start(Context context, int port, int handlerMsgId) {
        if (mState == ObjectState.IDLE) {
            if (mLog.isI()) mLog.logI(TAG + ".start() port:" + port + ", Allowed host:" + mHost);
            try {
                mContextRef = new WeakReference<Context>(context);
                mHandlerMessageId = handlerMsgId;
                mReceiverSocket = new MulticastSocket(port);
                // Do not set socket timeout here. If we do, we have seen the thread missing commands
                // between the time timeout happens and we call receive().
                mReceiverSocket.setReuseAddress(true);
                mServerThread = new Thread(new UDPReceiverRunnable(), "TCPServer");
                mContinue = true;
                mServerThread.start();
                mState = ObjectState.STARTED;
            } catch (Exception e) {
                if (mLog.isE()) mLog.logE(TAG + ".start(): Error creating. Reason " + e.getMessage());
                doClose();
            }
        }
        return mState == ObjectState.STARTED;
    }

    public void stop() {
        if (mState == ObjectState.STARTED) {
            if (mLog.isI()) mLog.logI(TAG + ".stop()");
            doClose();
            mState = ObjectState.IDLE;
        }
    }

    private void doClose() {
        Util.safeClose(mReceiverSocket);
        mReceiverSocket = null;
        mServerThread = null;
        mContinue = false;
    }

    private class UDPReceiverRunnable implements Runnable {

        @Override
        public void run() {
            if (mLog.isI()) mLog.logI(TAG + ".run()");
            Exception exception = null;
            String payloadHost;
            while (mContinue && (exception == null)) {
                // This function gets called from background thread context. The client might call destroy()
                // while we are still trying to read from socket. So to be safe, get a reference to
                // receiver socket and use that.
                final MulticastSocket multicastSocket = mReceiverSocket;
                try {
                    //if (mLog.isD()) mLog.logD(TAG + ".run() waiting for broadcast...");
                    // Reset mDatagramPacket to receive mBuffer
                    mDatagramPacket.setData(mBuffer);
                    // Blocking call
                    multicastSocket.receive(mDatagramPacket);
                    if (mDatagramPacket.getLength() > 0) {
                        if (mLocalIPAddress == null) {
                            final Context context = mContextRef != null ? mContextRef.get() : null;
                            mLocalIPAddress = Util.getLocalIP(context);
                            if (mLog.isI()) mLog.logI(TAG + ".run() Local IP Address " + mLocalIPAddress);
                        }
                        payloadHost = mDatagramPacket.getAddress().getHostAddress();
                        // Ignore UDP payload generated from current device
                        if (!payloadHost.equals(mLocalIPAddress)) {
                            // Process if a) host is not set OR b) host is same as payload host
                            if ((mHost == null) || mHost.equals(payloadHost)) {
                                final String payload = new String(mDatagramPacket.getData(), 0, mDatagramPacket.getLength(), Util.PAYLOAD_CHARSET);
                                if (mLog.isV()) {
                                    mLog.logV(TAG + ".run() payload:" + payload);
                                } else if (mLog.isD()) {
                                    mLog.logD(TAG + ".run() payload.length:" + payload.length() + ", bytes.length:" + mDatagramPacket.getLength());
                                }
                                if (mLog.isD()) mLog.logD(TAG + ".run() payload.length:" + payload.length());
                                final Message message = mHandler.obtainMessage(mHandlerMessageId, payload);
                                mHandler.sendMessage(message);
                            }
                        }
                    }
                } catch (IOException e) {
                    exception = e;
                }
                // This state check is to prevent generating fatal error message when stop() is called
                // which closes socket resulting in an exception here in run().
                if ((exception != null) && (mState == ObjectState.STARTED)) {
                    if (mLog.isE()) mLog.logE(TAG + ".run() " + exception.getMessage());
                    mHandler.sendEmptyMessage(Constants.MessageId.MSG_FATAL_ERROR);
                }
            }
            if (mLog.isI()) mLog.logI(TAG + ".run()<");
        }
    }
}
