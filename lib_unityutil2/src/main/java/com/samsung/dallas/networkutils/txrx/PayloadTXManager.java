package com.samsung.dallas.networkutils.txrx;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.samsung.dallas.networkutils.txrx.udptcp.WorkerTXUDPTCP;
import com.samsung.dallas.networkutils.txrx.wifip2p.WorkerTXWiFiDirect;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class provides primary API for sending payload.
 *
 * Get an instance of this class by calling getInstance().
 *
 */
public class PayloadTXManager {
    private static final String TAG = "PayloadTXManager";

    /**
     * Interface definition for callbacks to be invoked.
     */
    public interface Listener {
        /**
         * Called to indicate transmitter has started successfully.
         */
        void onStarted();
        /**
         * Called to indicate an error starting transmitter manager.
         */
        void onStartError();
        /**
         * Called to indicate an error in transmitter manager.
         */
        void onFatalError();
        /**
         * Called to indicate a new receiver device connected.
         *
         * @param device
         */
        void onReceiverConnected(TXRXDevice device);
        /**
         * Called to indicate receivers disconnected.
         *
         * This event is called when transmitter finds out the receiver connection no longer exist,
         * which may happen a while after receiver device is actually disconnected.
         * @param devices
         */
        void onReceiversDisconnected(Collection<TXRXDevice> devices);
    }

    private static PayloadTXManager mSingleton;

    private final Handler mUDPTCPHandler;
    private final WorkerTXUDPTCP mWorkerTXUDPTCP;
    private final WorkerTXWiFiDirect mWorkerTXWiFiDirect;
    private WorkerTXInterface mWorkerImplementation;
    private final Map<String, TXRXDevice> mReceivers;
    private final List<Listener> mListeners;
    private final Logger mLog;

    private ObjectState mState;
    private String mDeviceName;
    private final Set<String> mWhiteListedDevices;
    private boolean mCollisionDetectionEnabled;
    private ConnectionType mConnectionType;
    private int mMaxConnections;

    private PayloadTXManager() {
        mState = ObjectState.IDLE;
        mUDPTCPHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                doHandleMessage(msg);
            }
        };
        mWorkerTXUDPTCP = new WorkerTXUDPTCP(mUDPTCPHandler);
        mWorkerTXWiFiDirect = new WorkerTXWiFiDirect(mUDPTCPHandler);
        mWorkerImplementation = mWorkerTXUDPTCP; // Default to Connection.
        mConnectionType = Constants.DEFAULT_CONNECTION_TYPE;
        mMaxConnections = Constants.DEFAULT_MAX_RECEIVERS;
        mReceivers = new HashMap<String, TXRXDevice>();
        mListeners = new ArrayList<Listener>();
        mLog = Logger.getInstance();
        if (mLog.isI()) mLog.logI(TAG + " ver " + BuildConfig.VERSION_NAME);
        mWhiteListedDevices = new TreeSet<String>();
    }

    /**
     * Returns the payload transmission manager singleton instance
     *
     * @return
     */
    public static PayloadTXManager getInstance() {
        if (mSingleton == null) {
            synchronized (PayloadTXManager.class) {
                if (mSingleton == null) {
                    mSingleton = new PayloadTXManager();
                }
            }
        }
        return mSingleton;
    }

    /**
     * Returns list of receivers connected if available.
     *
     * If transmitter is initialized in connection-less mode (for instance UDP). Then this will return
     * an empty list.
     *
     * @return
     */
    public Collection<TXRXDevice> getReceivers() {
        return mReceivers.values();
    }

    /**
     * Register callback listener.
     *
     * @param listener
     */
    public void addListener(Listener listener) {
        if ((listener != null) && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    /**
     * Unregister callback listener.
     *
     * @param listener
     */
    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Sets device name.
     *
     * This device name will be included in service info payload.
     *
     * @param deviceName
     */
    public void setDeviceName(String deviceName) {
        mDeviceName = deviceName;
    }

    /**
     * Adds a device name to white list.
     *
     * If white list is not empty, connections from device name that exist in the list are accepted.
     *
     * @param deviceName
     */
    public void addToWhiteList(String deviceName) {
        mWhiteListedDevices.add(deviceName);
    }

    /**
     * Removes device name from white list.
     *
     * @param deviceName
     */
    public void removeFromWhiteList(String deviceName) {
        mWhiteListedDevices.remove(deviceName);
    }

    /**
     * Removes all device names from white list.
     *
     */
    public void clearWhiteList() {
        mWhiteListedDevices.clear();
    }

    /**
     * Returns white listed device names.
     *
     * @return
     */
    public Set<String> getWhiteList() {
        return Collections.unmodifiableSet(mWhiteListedDevices);
    }

    /**
     * Enables checking transmitter in the same setup.
     *
     * If a transmitter already exist, a start error callback is invoked.
     *
     * @param enabled
     */
    public void setCollisionDetectionEnabled(boolean enabled) {
        mCollisionDetectionEnabled = enabled;
    }

    /**
     * Sets connection type.
     *
     * If Wi-Fi Direct, devices need to be connected from System settings.
     *
     * @param type
     */
    public void setConnectionType(ConnectionType type) {
        if (mState == ObjectState.IDLE) {
            if (mConnectionType != type) {
                mConnectionType = type;
                switch (type) {
                    case TYPE_ETHERNET:
                        mWorkerImplementation = mWorkerTXUDPTCP;
                        break;
                    case TYPE_WIFI_DIRECT:
                        mWorkerImplementation = mWorkerTXWiFiDirect;
                        break;
                }
            }
        }
    }

    /**
     * Sets maximum connections.
     *
     * After receivers count reaches maximum connections, Transmitter will stop accepting new
     * connections.
     *
     * @param max
     */
    public void setMaxConnections(int max) {
        if (max >= 0) {
            mMaxConnections = max;
        }
    }
    /**
     * Starts transmitter.
     *
     * When connection type is ETHERNET, service payload is broadcast via UDP on port handshakePort.
     * When connection type is WIFI_DIRECT, service payload is broadcast via Wi-Fi direct service record.
     *
     * @param context
     * @param handshakePort - Port to be used to broadcast service payload.
     * @param sendPort - Port to be used to send payload via TCP.
     * @param broadcastPort - Port to be used to broadcast payload via UDP
     */
    public void start(Context context, int handshakePort, int sendPort, int broadcastPort) {
        if (mState == ObjectState.IDLE) {

            mWorkerImplementation.setDeviceName(mDeviceName);
            mWorkerImplementation.clearWhiteList();
            for (String deviceName : mWhiteListedDevices) {
                mWorkerImplementation.addToWhiteList(deviceName);
            }
            mWorkerImplementation.setCollisionDetectionEnabled(mCollisionDetectionEnabled);
            mWorkerImplementation.setMaxConnections(mMaxConnections);

            if (mLog.isI()) mLog.logI(TAG + ".start() handshakePort: " + handshakePort
                    + ", sendPort:" + sendPort + ", broadcastPort:" + broadcastPort);
            if (mWorkerImplementation.start(context, handshakePort, sendPort, broadcastPort)) {
                mState = ObjectState.STARTING;
            }
        }
    }

    /**
     * Send payload via TCP to all connected receivers.
     *
     * Payload sent via TCP takes precedence and are processed ahead of queued broadcast payloads.
     * This also means that this can starve broadcast payloads.
     *
     * @param payload
     */
    public void sendPayload(String payload) {
        if ((mState == ObjectState.STARTED) && (!TextUtils.isEmpty(payload))) {
            if (mLog.isD()) mLog.logD(TAG + ".sendPayload() payload.length:" + payload.length());
            mWorkerImplementation.sendPayload(payload);
        }
    }

    /**
     * Send payload via TCP to a specific device.
     *
     * @param device
     * @param payload
     */
    public void sendPayload(TXRXDevice device, String payload) {
        if ((mState == ObjectState.STARTED) && (device != null) &&
                !TextUtils.isEmpty(device.hostName) && !TextUtils.isEmpty(payload)) {
            if (mLog.isD()) mLog.logD(TAG + ".sendPayload() target: " + device + ", payload.length:" + payload.length());
            mWorkerImplementation.sendPayload(device, payload);
        }
    }

    /**
     * Broadcast payload via UDP.
     *
     * @param payload
     */
    public void broadcastPayload(String payload) {
        if ((mState == ObjectState.STARTED) && (!TextUtils.isEmpty(payload))) {
            if (mLog.isD()) mLog.logD(TAG + ".broadcastPayload() payload.length:" + payload.length());
            mWorkerImplementation.broadcastPayload(payload);
        }
    }

    /**
     * Stop transmitter.
     *
     * All queued send payload are sent to connected devices.
     * All queued broadcast payload are discarded.
     */
    public void stop() {
        if ((mState == ObjectState.STARTING) || (mState == ObjectState.STARTED)) {
            if (mLog.isI()) mLog.logI(TAG + ".stop()");
            doStop();
            mState = ObjectState.IDLE;
        }
    }

    // Internal implementation
    private void doHandleMessage(Message msg) {
        switch (msg.what) {
            case Constants.MessageId.MSG_START_RESULT_OK:
                if (mLog.isD()) mLog.logD(TAG + ".doHandleMessage() started");
                if (mState == ObjectState.STARTING) {
                    mState = ObjectState.STARTED;
                    final Iterator<Listener> iterator = mListeners.iterator();
                    while (iterator.hasNext()) {
                        iterator.next().onStarted();
                    }
                }
                break;
            case Constants.MessageId.MSG_START_RESULT_NOK:
                if (mLog.isD()) mLog.logD(TAG + ".doHandleMessage() start error");
                if (mState == ObjectState.STARTING) {
                    doStop();
                    mState = ObjectState.IDLE;
                    final Iterator<Listener> iterator = mListeners.iterator();
                    while (iterator.hasNext()) {
                        iterator.next().onStartError();
                    }
                }
                break;
            case Constants.MessageId.MSG_FATAL_ERROR:
                if (mLog.isD()) mLog.logD(TAG + ".doHandleMessage() fatal error");
                if (mState == ObjectState.STARTED) {
                    doStop();
                    mState = ObjectState.IDLE;
                    final Iterator<Listener> iterator = mListeners.iterator();
                    while (iterator.hasNext()) {
                        iterator.next().onFatalError();
                    }
                }
                break;
            case Constants.MessageId.MSG_TCP_CLIENT_CONNECTED:
                if (mLog.isD()) mLog.logD(TAG + ".doHandleMessage() TCP client connected");
                mWorkerImplementation.addTCPClient((Socket)msg.obj);
                break;
            case Constants.MessageId.MSG_RECEIVER_CONNECTED:
                if (mLog.isD()) mLog.logD(TAG + ".doHandleMessage() TCP receiver connected");
                if (mState == ObjectState.STARTED) {
                    final TXRXDevice connectedDevice = (TXRXDevice) msg.obj;
                    mReceivers.put(connectedDevice.hostName, connectedDevice);
                    final Iterator<Listener> iterator = mListeners.iterator();
                    while (iterator.hasNext()) {
                        iterator.next().onReceiverConnected(connectedDevice);
                    }
                }
                break;
            case Constants.MessageId.MSG_RECEIVERS_DISCONNECTED:
                if (mLog.isD()) mLog.logD(TAG + ".doHandleMessage() TCP receivers disconnected");
                if (mState == ObjectState.STARTED) {
                    final Collection<TXRXDevice> list = (Collection<TXRXDevice>) msg.obj;
                    int removed = 0;
                    if (list != null) {
                        TXRXDevice removedDevice;
                        for (TXRXDevice item : list) {
                            removedDevice = mReceivers.remove(item.hostName);
                            if (removedDevice != null) {
                                removed++;
                                if (mLog.isD()) mLog.logD(TAG + ".doHandleMessage() TCP receiver disconnected. " + removedDevice);
                            }
                        }
                        if (mLog.isI()) mLog.logI(TAG + ".doHandleMessage() TCP receivers disconnected. Removed " + removed + " items");
                        final Iterator<Listener> iterator = mListeners.iterator();
                        while (iterator.hasNext()) {
                            iterator.next().onReceiversDisconnected(list);
                        }
                    }
                }
                break;
            default:
                if (this.mLog.isE()) this.mLog.logE(TAG + ".doHandleMessage() Unhandled message.what:" + msg.what);
                break;
        }
    }

    private void doStop() {
        mWorkerImplementation.stop();
        mReceivers.clear();
    }
}
