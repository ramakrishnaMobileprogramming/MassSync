package com.samsung.dallas.networkutils.txrx.wifip2p;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Pair;

import com.samsung.dallas.networkutils.txrx.Constants;
import com.samsung.dallas.networkutils.txrx.Logger;
import com.samsung.dallas.networkutils.txrx.ObjectState;
import com.samsung.dallas.networkutils.txrx.TXRXCommand;
import com.samsung.dallas.networkutils.txrx.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Reads from a TCP Sockets in a different thread and look for TXRXCommand.BROADCAST_TX payload
 * from Transmitter socket.
 *
 */
public class TransmitterPayloadFilterTCPRX {

    private static final String TAG = "TransmitterPayloadFilterTCPRX";
    private final int MSG_CLIENT_CONNECTED = 1;
    private final int MSG_READ_PAYLOAD = 2;
    private final int MSG_QUIT = 3;

    private final Handler mHandler;
    private final Logger mLog;
    private final List<Util.SocketHolder> mClients;
    private final byte[] mBuffer;

    private ObjectState mState;
    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;

    public TransmitterPayloadFilterTCPRX(Handler handler) {
        mHandler = handler;
        mLog = Logger.getInstance();
        mClients = new ArrayList<Util.SocketHolder>();
        mBuffer = new byte[Util.PAYLOAD_BYTES_BUFFER_SIZE];
        mState = ObjectState.IDLE;
    }

    public void start() {
        if (mState == ObjectState.IDLE) {
            if (mLog.isI()) mLog.logI(TAG + ".start() ");
            mWorkerThread = new HandlerThread("TransmitterPayloadFilterTCPRX.WorkerThread");
            mWorkerThread.start();
            mWorkerHandler = new Handler(mWorkerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    doHandleMessage(msg);
                }
            };
            mState = ObjectState.STARTED;
        }
    }

    public void addClient(Socket socket) {
        if (mState == ObjectState.STARTED) {
            if (mLog.isI()) mLog.logI(TAG + ".addClient() ");
            final Message message = mWorkerHandler.obtainMessage(MSG_CLIENT_CONNECTED, socket);
            if (!mWorkerHandler.sendMessage(message)) {
                if (mLog.isE()) mLog.logE(TAG + ".addClient() Error posting message to queue");
                Util.safeClose(socket);
            }
        } else {
            Util.safeClose(socket);
        }
    }

    public void stop() {
        if (mState == ObjectState.STARTED) {
            if (mLog.isI()) mLog.logI(TAG + ".stop() ");
            mWorkerHandler.removeMessages(MSG_READ_PAYLOAD); // Remove pending reads
            mWorkerHandler.sendEmptyMessage(MSG_QUIT);
            mWorkerThread.quitSafely();
            mWorkerThread = null;
            mWorkerHandler = null;
            mState = ObjectState.IDLE;
        }
    }

    private void doHandleMessage(Message msg) {
        switch (msg.what) {
            case MSG_CLIENT_CONNECTED:
                doHandleClientConnected(msg);
                break;
            case MSG_READ_PAYLOAD:
                doHandleReadPayload();
                break;
            case MSG_QUIT:
                doHandleQuit();
                break;
        }
    }

    private void doHandleClientConnected(Message msg) {
        //if (mLog.isD()) mLog.logD(TAG + ".doHandleClientConnected() ");
        final Util.SocketHolder sh = new Util.SocketHolder();
        sh.socket = (Socket) msg.obj;
        try {
            sh.socket.setSoTimeout(1000); // Set a very short read timeout
            sh.hostName = sh.socket.getInetAddress().getHostName();
            sh.dataInputStream = new DataInputStream(sh.socket.getInputStream());
            mClients.add(sh);
            //if (mLog.isD()) mLog.logD(TAG + ".doHandleClientConnected() " + sh);
            if (!mWorkerHandler.hasMessages(MSG_READ_PAYLOAD)) {
                mWorkerHandler.sendEmptyMessage(MSG_READ_PAYLOAD);
            }
        } catch (IOException e) {
            if (mLog.isE()) mLog.logE(TAG + ".doHandleClientConnected() Error adding socket into list. Reason:" + e.getMessage());
            Util.safeClose(sh);
        }
    }

    private void doHandleReadPayload() {
        //if (mLog.isD()) mLog.logD(TAG + ".doHandleReadPayload() ");
        if (mClients.size() > 0) {
            final Util.SocketHolder sh = mClients.remove(0);
            try {
                final int bytesRead = Util.readBlocking(sh.dataInputStream, mBuffer);
                if (bytesRead > 0) {
                    final String payload = new String(mBuffer, 0, bytesRead, Util.PAYLOAD_CHARSET);
                    if (mLog.isV()) {
                        mLog.logV(TAG + ".doHandleReadPayload() payload:" + payload);
                    } else if (mLog.isD()) {
                        mLog.logD(TAG + ".doHandleReadPayload() payload.length:" + payload.length() + ", bytes.length:" + bytesRead);
                    }
                    // We are looking for TXRXCommand.BROADCAST_TX specifically
                    final Pair<TXRXCommand, String> pair = Util.parseTXRXCommand(payload);
                    final JSONObject jsonObject = Util.createJSON(((pair != null) && (pair.first == TXRXCommand.BROADCAST_TX)) ? pair.second : null);
                    // Do not check for IP here, the IP will be null
                    if ((jsonObject != null) && (jsonObject.optInt("sendPort") != 0)) {
                        jsonObject.put("ip", sh.hostName); // Update IP
                        final String updatedPayload = Util.createPayload(pair.first.toString(), jsonObject.toString());
                        final Message message = mHandler.obtainMessage(Constants.MessageId.MSG_TCP_PAYLOAD_RECEIVED, updatedPayload);
                        mHandler.sendMessage(message);
                    }
                }
            } catch (IOException e) {
                if (mLog.isE()) mLog.logE(TAG + ".doHandleReadPayload() Error reading payload from socket. Reason:" + e.getMessage());
            } catch (JSONException e) {
                if (mLog.isE()) mLog.logE(TAG + ".doHandleReadPayload() Error creating/updating JSON payload. Reason:" + e.getMessage());
            } finally {
                Util.safeClose(sh);
            }
        }
        // Wait here and see if there are more clients
        // Client may have called stop() while we were blocked
        if ((mClients.size() > 0) && !mWorkerHandler.hasMessages(MSG_QUIT)) {
            mWorkerHandler.sendEmptyMessage(MSG_READ_PAYLOAD);
        }
    }

    private void doHandleQuit() {
        //if (mLog.isD()) mLog.logD(TAG + ".doHandleQuit() ");
        for (Util.SocketHolder client : mClients) {
            Util.safeClose(client);
        }
        mClients.clear();
    }
}
