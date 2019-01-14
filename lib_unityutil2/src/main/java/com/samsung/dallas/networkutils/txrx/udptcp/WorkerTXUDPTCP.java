package com.samsung.dallas.networkutils.txrx.udptcp;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Pair;

import com.samsung.dallas.networkutils.txrx.Constants;
import com.samsung.dallas.networkutils.txrx.Logger;
import com.samsung.dallas.networkutils.txrx.ObjectState;
import com.samsung.dallas.networkutils.txrx.TCPServer;
import com.samsung.dallas.networkutils.txrx.TCPTransmitter;
import com.samsung.dallas.networkutils.txrx.TXRXCommand;
import com.samsung.dallas.networkutils.txrx.TXRXDevice;
import com.samsung.dallas.networkutils.txrx.TransmitterDevice;
import com.samsung.dallas.networkutils.txrx.UDPPortScanner;
import com.samsung.dallas.networkutils.txrx.UDPTransmitter;
import com.samsung.dallas.networkutils.txrx.Util;
import com.samsung.dallas.networkutils.txrx.WorkerTXInterface;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class WorkerTXUDPTCP implements WorkerTXInterface {
    private static final String TAG = "WorkerTXUDPTCP";

    private static final long HANDSHAKE_BROADCAST_INTERVAL_MILLIS = 3000;
    // Make this at-least 3 times the duration of HANDSHAKE_BROADCAST_INTERVAL_MILLIS so we can
    // check other transmitters on UDP port
    private static final long UDP_PORT_SCANNER_DURATION_MILLIS = 10000;

    private static final int MSG_START = 2100;
    private static final int MSG_STOP = 2101;
    private static final int MSG_SEND_BROADCAST_PAYLOAD = 2102;
    private static final int MSG_ADD_TCP_CLIENT = 2103;
    private static final int MSG_BROADCAST_HANDSHAKE = 2104;
    private static final int MSG_PRE_START = 2105;

    private final Handler mHandler;
    private final UDPTransmitter mHandshakeBroadcaster;
    private final UDPTransmitter mPayloadBroadcaster;
    private final TCPServer mTCPServer;
    private final TCPTransmitter mPayloadSender;

    private final List<String> mSendItems;
    private final List<String> mBroadcastItems;
    private final Logger mLog;
    // If enabled, check for other transmitters before accepting connections
    private boolean mCollisionDetectionEnabled;
    private int mMaxConnections;
    // Used to check if there are other transmitters using UDP port on current network
    // These are used in the background thread context
    private final UDPPortScanner mUDPPortScanner;
    private final UDPPortScanner.Listener mUDPPortListener;
    private long mPresenterStartTime;

    private volatile ObjectState mState;
    private WeakReference<Context> mContextRef;
    private int mHandshakePort;
    private int mSendPort;
    private int mBroadcastPort;

    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;

    private final CountDownTimer mTimer;
    private String mHandshakeMessage;
    private String mDeviceName;

    public WorkerTXUDPTCP(Handler handler) {
        mHandler = handler;
        mLog = Logger.getInstance();
        mCollisionDetectionEnabled = false;
        mHandshakeBroadcaster = new UDPTransmitter(handler);
        mPayloadBroadcaster = new UDPTransmitter(handler);
        mTCPServer = new TCPServer(handler);
        mPayloadSender = new TCPTransmitter(handler, false);
        mSendItems = new ArrayList<String>();
        mBroadcastItems = new ArrayList<String>();
        mUDPPortListener = new UDPPortScanner.Listener() {
            @Override
            public void onDataReceived(int port, String data) {
                if (mState == ObjectState.STARTING) {
                    // Parse data to see if this is a hand shake message
                    if (mLog.isD()) mLog.logD(TAG + "UDPPortListener.onDataReceived() data:" + data);
                    final Pair<TXRXCommand, String> pair = Util.parseTXRXCommand(data);
                    if ((pair != null) && (pair.first == TXRXCommand.BROADCAST_TX)) {
                        final JSONObject jsonObject = Util.createJSON(pair.second);
                        // This should never be null, but just in case
                        if (jsonObject != null) {
                            final TransmitterDevice td = Util.create(jsonObject);
                            // If remote presenter has started before us, we should bail out/
                            if (td.timestamp < mPresenterStartTime) {
                                td.handshakePort = mHandshakePort;
                                if (mLog.isE()) mLog.logE(TAG + "UDPPortListener.onDataReceived() Error active transmitter " + td + " found. stopping...");
                                final Message message = mHandler.obtainMessage(Constants.MessageId.MSG_START_RESULT_NOK);
                                mHandler.sendMessage(message);
                                // Send a message to stop self
                                mWorkerHandler.sendEmptyMessage(MSG_STOP);
                            }
                        }
                    }
                }
            }
            @Override
            public void onStopped() {
                if (mState == ObjectState.STARTING) {
                    if (mLog.isI()) mLog.logI(TAG + "UDPPortListener.onStopped()");
                    mWorkerHandler.sendEmptyMessage(MSG_START);
                }
            }
        };
        mUDPPortScanner = new UDPPortScanner(UDP_PORT_SCANNER_DURATION_MILLIS);
        mUDPPortScanner.addListener(mUDPPortListener);

        mTimer = new CountDownTimer(HANDSHAKE_BROADCAST_INTERVAL_MILLIS, HANDSHAKE_BROADCAST_INTERVAL_MILLIS) {
            @Override
            public void onTick(long millisUntilFinished) {
            }
            @Override
            public void onFinish() {
                switch (mState) {
                    case STARTING: // Fall through
                    case STARTED:
                        mWorkerHandler.removeMessages(MSG_BROADCAST_HANDSHAKE);
                        final Message message = mWorkerHandler.obtainMessage(MSG_BROADCAST_HANDSHAKE, mHandshakeMessage);
                        mWorkerHandler.sendMessage(message);
                        mTimer.start();
                        break;
                }
            }
        };

        mState = ObjectState.IDLE;
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
    public void setDeviceName(String deviceName) {
        mDeviceName = deviceName;
    }

    @Override
    public void setCollisionDetectionEnabled(boolean enabled) {
        if (mState == ObjectState.IDLE) {
            mCollisionDetectionEnabled = enabled;
        }
    }

    @Override
    public void setMaxConnections(int max) {
        mMaxConnections = max;
    }

    @Override
    public boolean start(Context context, int handshakePort, int sendPort, int broadcastPort) {
        if (mState == ObjectState.IDLE) {
            if (mLog.isI()) mLog.logI(TAG + ".start()");

            mContextRef = new WeakReference<Context>(context);
            mHandshakePort = handshakePort;
            mSendPort = sendPort;
            mBroadcastPort = broadcastPort;

            mWorkerThread = new HandlerThread("WorkerTXUDPTCP");
            mWorkerThread.start();
            mWorkerHandler = new Handler(mWorkerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    doHandleMessage(msg);
                }
            };

            // Since we don't have a global UTC, we rely on OS's default EPOC time in UTC. Ideally
            // users device is configured to right time.
            mPresenterStartTime = System.currentTimeMillis();

            if (mCollisionDetectionEnabled) {
                mWorkerHandler.sendEmptyMessage(MSG_PRE_START);
            } else {
                mWorkerHandler.sendEmptyMessage(MSG_START);
            }
            mState = ObjectState.STARTING;
        }
        return true;
    }

    /**
     * Takes ownership of the socket.
     *
     * @param socket
     */
    @Override
    public void addTCPClient(Socket socket) {
        if (socket != null) {
            boolean added = false;
            if (mState == ObjectState.STARTED) {
                if (mLog.isI()) mLog.logI(TAG + ".addTCPClient()");
                final Message message = mWorkerHandler.obtainMessage(MSG_ADD_TCP_CLIENT, socket);
                added = mWorkerHandler.sendMessage(message);
            }
            if (!added) {
                if (mLog.isI()) mLog.logI(TAG + ".addTCPClient() Error adding");
                Util.safeClose(socket);
            }
        }
    }

    @Override
    public void sendPayload(String payload) {
        if ((mState == ObjectState.STARTED) && (mPayloadSender.getReceiversCount() > 0)) {
            // We are adding in one thread and removing in another. So synchronize modifiers
            synchronized (mSendItems) {
                mSendItems.add(payload);
            }
            if (!mWorkerHandler.hasMessages(MSG_SEND_BROADCAST_PAYLOAD)) {
                mWorkerHandler.sendEmptyMessage(MSG_SEND_BROADCAST_PAYLOAD);
            }
        }
    }

    @Override
    public void sendPayload(TXRXDevice device, String payload) {
        if ((mState == ObjectState.STARTED) && (mPayloadSender.getReceiversCount() > 0)) {
            mPayloadSender.sendPayload(device, payload);
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
    public void stop() {
        if ((mState == ObjectState.STARTING) || (mState == ObjectState.STARTED)) {
            if (mLog.isI()) mLog.logI(TAG + ".stop()");

            // Remove all pending messages except the following:
            // MSG_ADD_TCP_CLIENT, else we will not cleanup sockets that are already in queue
            // MSG_SEND_BROADCAST_PAYLOAD, else send items are already queued up will be lost
            mWorkerHandler.removeMessages(MSG_PRE_START);
            mWorkerHandler.removeMessages(MSG_START);
            synchronized (mBroadcastItems) {
                mBroadcastItems.clear();
            }
            mWorkerHandler.removeMessages(MSG_BROADCAST_HANDSHAKE);
            // Send message to stop
            mWorkerHandler.sendEmptyMessage(MSG_STOP);
            mWorkerThread.quitSafely();
            // For all practical purposes we are stopped even though we are sending pending items
            // to receivers
            mState = ObjectState.IDLE;
        }
    }

    private void doHandleMessage(Message msg) {
        switch (msg.what) {
            case MSG_PRE_START:
                onMessagePreStart();
                break;
            case MSG_START:
                onMessageStart();
                break;
            case MSG_STOP:
                onMessageStop();
                break;
            case MSG_SEND_BROADCAST_PAYLOAD:
                onMessageSendBroadcastPayload();
                break;
            case MSG_ADD_TCP_CLIENT:
                onMessageAddTCPClient((Socket) msg.obj);
                break;
            case MSG_BROADCAST_HANDSHAKE:
                onMessageBroadcastHandshake((String)msg.obj);
            default:
                break;
        }
    }
    private void onMessagePreStart() {
        if (mLog.isD()) mLog.logD(TAG + ".onMessagePreStart()");
        mUDPPortScanner.cancel();
        // Just check for hand shake port, we don't need to check for broadcast port
        // as we check for connected host when we listen to broadcast messages (TODO check this!!!)
        mUDPPortScanner.addPort(mHandshakePort);
        final Context context = mContextRef.get();
        boolean result = mUDPPortScanner.start(context);
        if (result) {
            mHandshakeMessage = Util.createPayloadBroadcastTransmitter(context, mDeviceName, mSendPort,
                    mBroadcastPort, mPresenterStartTime, false);

            result = mHandshakeBroadcaster.start(context, mHandshakePort);
        }
        if (result) {
            // Send a handshake broadcast immediately
            mWorkerHandler.removeMessages(MSG_BROADCAST_HANDSHAKE);
            final Message message = mWorkerHandler.obtainMessage(MSG_BROADCAST_HANDSHAKE, mHandshakeMessage);
            mWorkerHandler.sendMessage(message);
            // Start timer
            mTimer.start();
        } else {
            final Message message = mHandler.obtainMessage(Constants.MessageId.MSG_START_RESULT_NOK);
            mHandler.sendMessage(message);
            if (mLog.isE()) mLog.logE(TAG + ".onMessagePreStart() Error starting poller. stopping...");
            // Send a message to stop self
            mWorkerHandler.sendEmptyMessage(MSG_STOP);
        }
    }

    private void onMessageStart() {
        if (mLog.isD()) mLog.logD(TAG + ".onMessageStart()");
        final Context context = mContextRef.get();
        boolean result = context == null ? false : true;
        if (result && !mCollisionDetectionEnabled && (mHandshakePort != -1)) {
            result = mHandshakeBroadcaster.start(context, mHandshakePort);
        }
        if (result && (mBroadcastPort != -1)) {
            result = mPayloadBroadcaster.start(context, mBroadcastPort);
        }
        if (result && (mSendPort != -1)) {
            result = mTCPServer.start(mSendPort);
        }
        if (result && (mSendPort != -1)) {
            result = mPayloadSender.start();
        }
        if (result) {
            if (mHandshakePort != -1) {
                // Now we are ready to accept connections from receivers. Update our handshake message
                mHandshakeMessage = Util.createPayloadBroadcastTransmitter(context, mDeviceName, mSendPort,
                        mBroadcastPort, mPresenterStartTime, true);
                if (!mCollisionDetectionEnabled) {
                    // Send a handshake broadcast immediately
                    mWorkerHandler.removeMessages(MSG_BROADCAST_HANDSHAKE);
                    final Message message = mWorkerHandler.obtainMessage(MSG_BROADCAST_HANDSHAKE, mHandshakeMessage);
                    mWorkerHandler.sendMessage(message);
                    // Start timer
                    mTimer.start();
                }
            }

            mHandler.sendEmptyMessage(Constants.MessageId.MSG_START_RESULT_OK);
            mState = ObjectState.STARTED;
        } else {
            final Message message = mHandler.obtainMessage(Constants.MessageId.MSG_START_RESULT_NOK);
            mHandler.sendMessage(message);
            if (mLog.isE()) mLog.logE(TAG + ".onMessageStart() Error starting channels. stopping...");
            // Send a message to stop self
            mWorkerHandler.sendEmptyMessage(MSG_STOP);
        }
    }

    private void onMessageStop() {
        if (mLog.isD()) mLog.logD(TAG + ".onMessageStop()");
        mTimer.cancel();
        mUDPPortScanner.cancel();
        mHandshakeBroadcaster.stop();
        mPayloadBroadcaster.stop();
        mTCPServer.stop();
        // Send all pending mSendItems.
        boolean hasSendItems;
        do {
            hasSendItems = doSendBroadcastPayload();
        } while (hasSendItems);
        mPayloadSender.stop();
    }


    private void onMessageSendBroadcastPayload() {
        if (doSendBroadcastPayload()) {
            mWorkerHandler.sendEmptyMessage(MSG_SEND_BROADCAST_PAYLOAD);
        } else {
            if (mLog.isV()) mLog.logV(TAG + ".onMessageSendBroadcastPayload() Nothing to send/broadcast");
        }
    }

    private void onMessageAddTCPClient(Socket socket) {
        if (mLog.isD()) mLog.logD(TAG + ".onMessageAddTCPClient()");
        if (mPayloadSender.getReceiversCount() < mMaxConnections) {
            if (!mPayloadSender.addTCPClient(socket)) {
                if (mLog.isE()) mLog.logE(TAG + ".onMessageAddTCPClient() Error adding TCP client socket");
                Util.safeClose(socket);
            }
        } else { // We have reached maximum number of connections.
            if (mLog.isI()) mLog.logI(TAG + ".onMessageAddTCPClient() Max connection " + mMaxConnections
                    + " reached. Refusing connection request from IP:" + socket.getInetAddress().getHostName()
                    + ". Shutting down TCPServer...");
            Util.safeClose(socket);
            mTCPServer.stop();
            mWorkerHandler.removeMessages(MSG_BROADCAST_HANDSHAKE);
            mHandshakeBroadcaster.stop();
            mTimer.cancel();
        }
    }

    private void onMessageBroadcastHandshake(String payload) {
        if (mLog.isD()) mLog.logD(TAG + ".onMessageBroadcastHandshake() payload.length:" + payload.length());
        mHandshakeBroadcaster.broadcastPayload(payload);
    }

    /**
     * Removes and sends first item from mSendItems list via TCP. If mSendItems list is empty,
     * removes and sends first item from mBroadcastItems list via UDP.
     *
     * Returns true if mSendItems OR mBroadcastItems lists (after removal) are NOT empty. If both
     * lists are empty, returns false.
     *
     * Note: This could starve broadcast payload if transmitter is bombarded with send payload.
     * At the same time, this scheme will make sure send payload (sent via TCP) gets higher priority
     * than broadcast payload (sent via UDP)
     *
     * @return
     */
    private boolean doSendBroadcastPayload() {
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
            if (mLog.isV()) mLog.logV(TAG + ".doSendBroadcastPayload() send payload.length:" + sendPayload.length());
            mPayloadSender.sendPayload(sendPayload);
            sendItemCount--;
            if (sendItemCount == 0) { // mSendItems is empty
                synchronized (mBroadcastItems) {
                    broadcastItemCount = mBroadcastItems.size();
                }
            }
        } else {
            synchronized (mBroadcastItems) {
                broadcastItemCount = mBroadcastItems.size();
                if (broadcastItemCount > 0) {
                    broadcastPayload = mBroadcastItems.remove(0);
                }
            }
            if (broadcastPayload != null) {
                if (mLog.isV()) mLog.logV(TAG + ".doSendBroadcastPayload() broadcast payload.length:" + broadcastPayload.length());
                mPayloadBroadcaster.broadcastPayload(broadcastPayload);
                broadcastItemCount--;
            }
        }
        return ((sendItemCount >= 1) || (broadcastItemCount >= 1));
    }
}
