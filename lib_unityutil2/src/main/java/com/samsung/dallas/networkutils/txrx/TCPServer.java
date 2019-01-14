package com.samsung.dallas.networkutils.txrx;

import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {
    private static final String TAG = "TCPServer";

    private final Handler mHandler;
    private final Logger mLog;

    private volatile ObjectState mState;
    private Thread mServerThread;
    private volatile boolean mContinue;
    private ServerSocket mServerSocket;

    public TCPServer(Handler handler) {
        mHandler = handler;
        mLog = Logger.getInstance();
        mState = ObjectState.IDLE;
    }

    /**
     * Starts TCP server on specified port.
     *
     * @param port
     * @return true if started successfully or it has already been started.
     */
    public boolean start(int port) {
        if (mState == ObjectState.IDLE) {
            if (mLog.isI()) mLog.logI(TAG + ".start() port:" + port);
            try {
                mServerSocket = new ServerSocket(port);
                mServerThread = new Thread(new TCPServerImpl(), "TCPServer");
                mContinue = true;
                mServerThread.start();
                mState = ObjectState.STARTED;
            } catch (IOException e) {
                if (mLog.isE()) mLog.logE(TAG + ".start() " + e.getMessage());
                doStop();
            }
        }
        return mState == ObjectState.STARTED;
    }

    public void stop() {
        if (mLog.isI()) mLog.logI(TAG + ".stop()");
        doStop();
        mState = ObjectState.IDLE;
    }

    private void doStop() {
        Util.safeClose(mServerSocket);
        mServerSocket = null;
        mServerThread = null;
        mContinue = false;
    }

    private class TCPServerImpl implements Runnable {
        @Override
        public void run() {
            if (mLog.isI()) mLog.logI(TAG + ".run()");
            Exception exception = null;
            while (mContinue && (exception == null)) {
                try {
                    if (mLog.isD()) mLog.logD(TAG + ".run() waiting for client connection...");
                    final Socket socket = mServerSocket.accept();
                    if (mLog.isD()) mLog.logD(TAG + ".run() client " + socket.getInetAddress().getHostAddress() + " connected");
                    final Message message = mHandler.obtainMessage(Constants.MessageId.MSG_TCP_CLIENT_CONNECTED, socket);
                    if (!mHandler.sendMessage(message)) {
                        if (mLog.isE()) mLog.logE(TAG + ".run() error processing client socket");
                        Util.safeClose(socket);
                    }
                } catch (IOException e) {
                    exception = e;
                }
                if ((exception != null) && (mState == ObjectState.STARTED)) {
                    if (mLog.isE()) mLog.logE(TAG + ".run() " + exception.getMessage());
                    mHandler.sendEmptyMessage(Constants.MessageId.MSG_FATAL_ERROR);
                }
            }
            if (mLog.isI()) mLog.logI(TAG + ".run()<");
        }
    }
}
