package com.samsung.dallas.networkutils.txrx;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Pair;

import com.samsung.dallas.networkutils.txrx.Util.SocketHolder;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class TCPTransmitter {
    private static final String TAG = "TCPTransmitter";

    private static final int MSG_ADD_CLIENT = 100;
    private static final int MSG_SEND_PAYLOAD = 101;
    private static final int MSG_STOP = 102;
    private static final int MSG_SEND_PAYLOAD_TO_DEVICE = 103;

    private final Handler mHandler;
    private final boolean mRunInBackground;
    private final Logger mLog;
    private final byte[] mBuffer;
    private final StringBuilder mStringBuilder;
    private final Map<String, SocketHolder> mSocketHolderMap;
    private final List<String> mInvalidKeys; // Temp list of invalid host names
    private final Set<String> mHostNameSet; // Temp list
    private final List<String> mWhiteList;

    private ObjectState mState;
    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;
    private volatile int mClientCount;

    public TCPTransmitter(Handler handler, boolean runInBackground) {
        mHandler = handler;
        mRunInBackground = runInBackground;
        mLog = Logger.getInstance();
        mBuffer = new byte[1024];
        mStringBuilder = new StringBuilder();
        mSocketHolderMap = new HashMap<String, SocketHolder>();
        mInvalidKeys = new ArrayList<String>();
        mHostNameSet = new TreeSet<String>();
        mWhiteList = new ArrayList<String>();

        mState = ObjectState.IDLE;
        mClientCount = 0;
    }

    public int getReceiversCount() {
        return mClientCount;
    }

    public void addToWhiteList(String deviceName) {
        if (!TextUtils.isEmpty(deviceName) && !mWhiteList.contains(deviceName)) {
            mWhiteList.add(deviceName);
        }
    }

    public void removeFromWhiteList(String deviceName) {
        mWhiteList.remove(deviceName);
    }

    public void clearWhiteList() {
        mWhiteList.clear();
    }

    public List<String> getWhiteList() {
        return mWhiteList;
    }

    /**
     * Starts TCP transmitter
     *
     * @return true if started successfully or it has already been started.
     */
    public boolean start() {
        if (mState == ObjectState.IDLE) {
            if (mLog.isI()) mLog.logI(TAG + ".start()");
            if (mRunInBackground) {
                mWorkerThread = new HandlerThread("TCPTransmitter");
                mWorkerThread.start();
                mWorkerHandler = new Handler(mWorkerThread.getLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        doHandleMessage(msg);
                    }
                };
            }
            mState = ObjectState.STARTED;
        }
        return mState == ObjectState.STARTED;
    }

    /**
     * Takes ownership of the socket
     *
     * @param socket
     * @return
     */
    public boolean addTCPClient(Socket socket) {
        boolean result = false;
        if (mState == ObjectState.STARTED) {
            if (mLog.isI()) mLog.logI(TAG + ".addTCPClient()");
            if (mRunInBackground) {
                final Message message = mWorkerHandler.obtainMessage(MSG_ADD_CLIENT, socket);
                result = mWorkerHandler.sendMessage(message);
            } else {
                onMessageAddClient(socket);
                result = true;
            }
        }
        if (!result) {
            Util.safeClose(socket);
        }
        return result;
    }

    public void sendPayload(String payload) {
        if ((mState == ObjectState.STARTED) && (mClientCount > 0)) {
            if (mLog.isD()) mLog.logD(TAG + ".sendPayload() payload.length:" + payload.length());
            if (mRunInBackground) {
                final Message message = mWorkerHandler.obtainMessage(MSG_SEND_PAYLOAD, payload);
                mWorkerHandler.sendMessage(message);
            } else {
                onMessageSendPayload(payload);
            }
        }
    }

    public void sendPayload(TXRXDevice device, String payload) {
        if ((mState == ObjectState.STARTED) && (mClientCount > 0)) {
            if (mLog.isD()) mLog.logD(TAG + ".sendPayload() target:" + payload + ", payload.length:" + payload.length());
            if (mRunInBackground) {
                final PayloadToDevice msgObj = new PayloadToDevice();
                msgObj.device = device;
                msgObj.payload = payload;
                final Message message = mWorkerHandler.obtainMessage(MSG_SEND_PAYLOAD_TO_DEVICE, msgObj);
                mWorkerHandler.sendMessage(message);
            } else {
                onMessageSendPayload(device, payload);
            }
        }
    }

    public void stop() {
        if ((mState == ObjectState.STARTING) || (mState == ObjectState.STARTED)) {
            if (mLog.isI()) mLog.logI(TAG + ".stop()");
            if (mRunInBackground) {
                // Remove all pending messages
                // Don't remove MSG_ADD_CLIENT, else we will not cleanup sockets
                mWorkerHandler.removeMessages(MSG_SEND_PAYLOAD);
                mWorkerHandler.removeMessages(MSG_SEND_PAYLOAD_TO_DEVICE);
                // Send message to stop
                mWorkerHandler.sendEmptyMessage(MSG_STOP);
                mWorkerThread.quitSafely();
            } else {
                onMessageStop();
            }

            mState = ObjectState.IDLE;
            mWorkerThread = null;
            mWorkerHandler = null;
        }
    }

    private void doHandleMessage(Message msg) {
        switch (msg.what) {
            case MSG_ADD_CLIENT:
                onMessageAddClient((Socket) msg.obj);
                break;
            case MSG_SEND_PAYLOAD:
                onMessageSendPayload((String) msg.obj);
                break;
            case MSG_STOP:
                onMessageStop();
                break;
            case MSG_SEND_PAYLOAD_TO_DEVICE:
                final PayloadToDevice msgObj = (PayloadToDevice) msg.obj;
                onMessageSendPayload(msgObj.device, msgObj.payload);
                break;
            default:
                if (mLog.isE()) mLog.logE(TAG + ".doHandleMessage() Unhandled msg.what " + msg.what);
                break;
        }
    }

    private void onMessageAddClient(Socket socket) {
        if (mLog.isD()) mLog.logD(TAG + ".onMessageAddClient()");
        final SocketHolder socketHolder = new SocketHolder();
        try {
            // Use half second max timeout, else we will block send payload commands
            //final String handshake = getClientHandshake(socket, 500);
            socketHolder.hostName = socket.getInetAddress().getHostName();
            socketHolder.socket = socket;
            socketHolder.dataInputStream = new DataInputStream(socket.getInputStream());
            socketHolder.dataOutputStream = new DataOutputStream(socket.getOutputStream());

            final String handshake = getClientHandshake(socketHolder, 500);
            final Pair<TXRXCommand, String> pair = Util.parseTXRXCommand(handshake);
            if ((pair != null) && (pair.first == TXRXCommand.REGISTER_RX)) {
                // TODO use Gson to unmarshal into POJO
                final JSONObject jsonObject = Util.createJSON(pair.second);
                if (jsonObject != null) {
                    socketHolder.deviceName = jsonObject.optString("deviceName", "");
                }
                // We clean up broken sockets when we try to send payload.
                // Highly unlikely case where we may end up keeping one end of a broken pipe.
                final SocketHolder danglingHolder = mSocketHolderMap.remove(socketHolder.hostName);
                Util.safeClose(danglingHolder);

                boolean canConnect = true;
                // If devices are white listed, then only device name from list is added
                final Iterator<String> iterator = mWhiteList.iterator();
                if (iterator.hasNext()) {
                    // Check to see if the device is white listed
                    canConnect = false;
                    do {
                        if (TextUtils.equals(iterator.next(), socketHolder.deviceName)) {
                            if (mLog.isI()) mLog.logI(TAG + ".onMessageAddClient() White listed " + socketHolder);
                            canConnect = true;
                            break;
                        }
                    } while (iterator.hasNext());
                }

                if (canConnect) {
                    mSocketHolderMap.put(socketHolder.hostName, socketHolder);
                    mClientCount = mSocketHolderMap.size();

                    final TXRXDevice cd = new TXRXDevice();
                    cd.deviceName = socketHolder.deviceName;
                    cd.hostName = socketHolder.hostName;
                    if (mLog.isI()) mLog.logI(TAG + ".onMessageAddClient() added " + cd);

                    final Message message = mHandler.obtainMessage(Constants.MessageId.MSG_RECEIVER_CONNECTED, cd);
                    mHandler.sendMessage(message);
                } else {
                    if (mLog.isI()) mLog.logI(TAG + ".onMessageAddClient() Not white listed. Closing " + socketHolder);
                    Util.safeClose(socketHolder);// Close client socket
                }
            } else {
                if (mLog.isE()) mLog.logE(TAG + ".onMessageAddClient() Error: Invalid receiver socket. Closing " + socketHolder.hostName);
                Util.safeClose(socketHolder);// Close client socket
            }
        } catch (Exception e) {
            if (mLog.isE()) mLog.logE(TAG + ".onMessageAddClient() " + e.getMessage());
            Util.safeClose(socket);
        }
    }

    private void onMessageSendPayload(String payload) {
        onMessageSendPayload(mSocketHolderMap.keySet(), payload);
    }

    private void onMessageSendPayload(TXRXDevice device, String payload) {
        mHostNameSet.clear();
        mHostNameSet.add(device.hostName);
        onMessageSendPayload(mHostNameSet, payload);
    }

    private void onMessageSendPayload(Set<String> keySet, String payload) {
        final long startTimeMillis = System.currentTimeMillis();
        mInvalidKeys.clear();
        final byte[] payloadBytes = payload.getBytes(Util.PAYLOAD_CHARSET);
        if (mLog.isV()) {
            mLog.logV(TAG + ".onMessageSendPayload() payload:" + payload);
        } else if (mLog.isD()) {
            mLog.logD(TAG + ".onMessageSendPayload() payload.length:" + payload.length() + ", bytes.length:" + payloadBytes.length);
        }
        for (String key : keySet) {
            final SocketHolder holder = mSocketHolderMap.get(key);
            try {
                // Protocol length of bytes followed by actual data
                holder.dataOutputStream.writeInt(payloadBytes.length);
                holder.dataOutputStream.write(payloadBytes);
                holder.dataOutputStream.flush();
            } catch (IOException e) {
                if (mLog.isI()) mLog.logI(TAG + ".onMessageSendPayload() Error writing to IP " + holder.hostName);
                mInvalidKeys.add(key);
            }
        }
        if (mInvalidKeys.size() > 0) {
            final List<TXRXDevice> devices = new ArrayList<TXRXDevice>(mInvalidKeys.size());
            for (String hostName : mInvalidKeys) {
                final SocketHolder holder = mSocketHolderMap.remove(hostName);
                Util.safeClose(holder);
                final TXRXDevice device = new TXRXDevice();
                device.hostName = holder.hostName;
                device.deviceName = holder.deviceName;
                devices.add(device);
                if (mLog.isI()) mLog.logI(TAG + ".onMessageSendPayload() Removed IP " + holder.hostName);
            }
            mClientCount = mSocketHolderMap.size();
            mHandler.sendMessage(mHandler.obtainMessage(Constants.MessageId.MSG_RECEIVERS_DISCONNECTED, devices));
        }
        if (mLog.isD()) mLog.logD(TAG + ".onMessageSendPayload() time taken:" +
                (System.currentTimeMillis() - startTimeMillis)+ " millis");
    }

    private void onMessageStop() {
        if (mLog.isD()) mLog.logD(TAG + ".onMessageStop()");
        final Set<String> keySet = mSocketHolderMap.keySet();
        for (String key : keySet) {
            final SocketHolder holder = mSocketHolderMap.get(key);
            if (mLog.isD()) mLog.logD(TAG + ".onMessageStop() closing IP:" + holder.hostName);
            Util.safeClose(holder); // Close client socket
        }
        mSocketHolderMap.clear();
        mClientCount = mSocketHolderMap.size();
    }

    private String getClientHandshake(SocketHolder socketHolder, int timeoutMillis) throws IOException {
        final Socket socket = socketHolder.socket;
        final int currentSoTimeout = socket.getSoTimeout(); // Preserve current read-timeout
        // Set read timeout, read data from the socket
        socket.setSoTimeout(timeoutMillis);
        mStringBuilder.delete(0, mStringBuilder.length()); // Clear existing data (if any)
        Util.readBlocking(socketHolder.dataInputStream, mBuffer, mStringBuilder);
        socket.setSoTimeout(currentSoTimeout); // Reset read-timeout
        return mStringBuilder.toString();
    }

    private static class PayloadToDevice {
        public String payload;
        public TXRXDevice device;
    }
}