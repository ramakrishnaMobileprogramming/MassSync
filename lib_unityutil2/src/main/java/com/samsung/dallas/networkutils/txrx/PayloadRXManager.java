package com.samsung.dallas.networkutils.txrx;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.samsung.dallas.networkutils.txrx.udp.WorkerRXUDP;
import com.samsung.dallas.networkutils.txrx.udptcp.WorkerRXUDPTCP;
import com.samsung.dallas.networkutils.txrx.wifip2p.WorkerRXWiFiDirect;

/**
 * This class provides primary API for receiving payload.
 *
 * Get an instance of this class by calling getInstance().
 *
 * All properties must be set before calling start().
 *
 */
public class PayloadRXManager {
    private static final String TAG = "PayloadRXManager";

    /**
     * Interface definition for callbacks to be invoked.
     */
    public interface Listener {
        /**
         * Called to indicate receiver has started successfully.
         */
        void onStarted();
        /**
         * Called to indicate an error starting receiver manager.
         */
        void onStartError();
        /**
         * Called to indicate an error in receiver manager.
         */
        void onFatalError();
        /**
         * Called to indicate transmitter device connected.
         *
         * @param device
         */
        void onTransmitterConnected(TXRXDevice device);

        /**
         * Called to indicate payload (TCP/UDP) received.
         *
         * @param payload
         */
        void onPayloadReceived(String payload);
    }

    private static PayloadRXManager mSingleton;

    private final Handler mUDPTCPHandler;
    private final WorkerRXUDPTCP mWorkerRXUDPTCP;
    private final WorkerRXWiFiDirect mWorkerRXWiFiDirect;
    private final WorkerRXUDP mWorkerRXUDP;
    private WorkerRXInterface mWorkerImplementation;
    private final Logger mLog;

    private Listener mListener;
    private ObjectState mState;
    private TXRXDevice mTransmitter;
    private String mDeviceName;
    private ConnectionType mConnectionType;

    private PayloadRXManager() {
        mState = ObjectState.IDLE;
        mUDPTCPHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                doHandleMessage(msg);
            }
        };
        mWorkerRXUDPTCP = new WorkerRXUDPTCP(mUDPTCPHandler, false);
        mWorkerRXWiFiDirect = new WorkerRXWiFiDirect(mUDPTCPHandler, false);
        mWorkerRXUDP = new WorkerRXUDP(mUDPTCPHandler);
        mWorkerImplementation = mWorkerRXUDPTCP; // Default
        mConnectionType = Constants.DEFAULT_CONNECTION_TYPE;
        mLog = Logger.getInstance();
        if (mLog.isI()) mLog.logI(TAG + " ver " + BuildConfig.VERSION_NAME);
    }

    /**
     * Returns the payload receiver manager singleton instance
     *
     * @return
     */
    public static PayloadRXManager getInstance() {
        if (mSingleton == null) {
            synchronized (PayloadRXManager.class) {
                if (mSingleton == null) {
                    mSingleton = new PayloadRXManager();
                }
            }
        }
        return mSingleton;
    }

    /**
     * Register callback listener.
     *
     * @param listener
     */
    public void setListener(Listener listener) {
        mListener = listener;
    }


    /**
     * Sets device name.
     *
     * This device name will be included in register info payload.
     *
     * @param deviceName
     */
    public void setDeviceName(String deviceName) {
        mDeviceName = deviceName;
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
                        mWorkerImplementation = mWorkerRXUDPTCP;
                        break;
                    case TYPE_WIFI_DIRECT:
                        mWorkerImplementation = mWorkerRXWiFiDirect;
                        break;
                }
            }
        }
    }

    /**
     * Sets whether or not receiver should accept payload from all payload transmitters in the
     * current network. By default payload is accepted only from connected transmitter.
     *
     * @param allow
     */
    public void setAcceptPayloadFromAll(boolean allow) {
        mWorkerImplementation.setAcceptPayloadFromAll(allow);
    }

    /**
     * Starts receiver.
     *
     * When connection type is ETHERNET, port is used to listen to service payload broadcast via UDP.
     * When connection type is WIFI_DIRECT, service payload is obtained via Wi-Fi direct service record.
     *
     * @param context
     * @param txShakeHandPort - Port to be used to listen to service payload.
     */
    public void start(Context context, int txShakeHandPort) {
        start(context, txShakeHandPort, -1);
    }

    /**
     * Starts the receiver.
     *
     * When connection type is ETHERNET:
     * - txShakeHandPort, if non-negative, is used to listen to service payload broadcast via UDP.
     *   This must be specified for receiving TCP-UDP based payload. After TCP connection has been
     *   established, by default only broadcast payload from connected transmitter is reported to
     *   listener via Listener.onPayloadReceived(). However, this can be changed to report broadcast
     *   payload from all transmitters, using  setAcceptPayloadFromAll(true).
     * - broadcastPort, if non-negative, is used to listen to data payload broadcast via UDP. This
     *   is used in the cases where client just wants to receive only UDP payload in the current
     *   network.
     *
     * Note: When connection type is ETHERNET, either txShakeHandPort OR broadcastPort can be
     * specified, but not both.
     *
     * When connection type is WIFI_DIRECT, service payload is obtained via Wi-Fi direct service record.
     *
     * @param context
     * @param txShakeHandPort
     * @param broadcastPort
     * @throws RuntimeException if neither or both ports are specified when connection type is ETHERNET.
     */
    public void start(Context context, int txShakeHandPort, int broadcastPort) {
        if (mState == ObjectState.IDLE) {
            if (mLog.isI()) mLog.logI(TAG + ".start() txShakeHandPort:" + txShakeHandPort + ", broadcastPort:" + broadcastPort);
            int listenPort = txShakeHandPort;
            if (mConnectionType == ConnectionType.TYPE_ETHERNET) {
                // If both are undefined OR defined
                if (((txShakeHandPort < 0) && (broadcastPort < 0))
                        || ((txShakeHandPort >= 0) && (broadcastPort >= 0))) {
                    throw new RuntimeException("Invalid arguments");
                }
                // If broadcastPort is defined
                if (broadcastPort >= 0) {
                    listenPort = broadcastPort;
                    mWorkerImplementation = mWorkerRXUDP;
                }
            }
            mWorkerImplementation.setDeviceName(mDeviceName);
            mState = ObjectState.STARTING;
            mWorkerImplementation.start(context, listenPort);
        }
    }

    /**
     * Returns true if this receiver device is connected to a transmitter.
     *
     * @return
     */
    public boolean isConnected() {
        return mTransmitter != null;
    }

    /**
     * Returns transmitter this receiver device is connected to.
     *
     * @return
     */
    public TXRXDevice getTransmitter() {
        return mTransmitter;
    }

    /**
     * Stop receiver.
     */
    public void stop() {
        if ((mState == ObjectState.STARTING) || (mState == ObjectState.STARTED)) {
            if (mLog.isI()) mLog.logI(TAG + ".stop()");
            doStop();
            mState = ObjectState.IDLE;
        }
    }

    private void doHandleMessage(Message msg) {
        switch (msg.what) {
            case Constants.MessageId.MSG_START_RESULT_OK:
                if (mLog.isD()) mLog.logD(TAG + ".doHandleMessage() started");
                mState = ObjectState.STARTED;
                if (mListener != null) {
                    mListener.onStarted();
                }
                break;
            case Constants.MessageId.MSG_START_RESULT_NOK:
                if (mLog.isD()) mLog.logD(TAG + ".doHandleMessage() start error");
                doStop();
                mState = ObjectState.IDLE;
                if (mListener != null) {
                    mListener.onStartError();
                }
                break;
            case Constants.MessageId.MSG_FATAL_ERROR:
                if (mLog.isD()) mLog.logD(TAG + ".doHandleMessage() fatal error");
                if (mState == ObjectState.STARTED) {
                    doStop();
                    mState = ObjectState.IDLE;
                    if (mListener != null) {
                        mListener.onFatalError();
                    }
                }
                break;
            case Constants.MessageId.MSG_RECEIVER_CONNECTED:
                mTransmitter = (TXRXDevice) msg.obj;
                if (mLog.isD()) mLog.logD(TAG + ".doHandleMessage() Receiver[Self] connected to transmitter:" + mTransmitter);
                if (mListener != null) {
                    mListener.onTransmitterConnected(mTransmitter);
                }
                break;
            case Constants.MessageId.MSG_TCP_PAYLOAD_RECEIVED:
                final String tcpPayload = (String) msg.obj;
                if (mLog.isD()) mLog.logD(TAG + ".doHandleMessage() TCP payload.length:" + tcpPayload.length());
                if (mState == ObjectState.STARTED) {
                    if (mListener != null) {
                        mListener.onPayloadReceived(tcpPayload);
                    }
                }
                break;
            case Constants.MessageId.MSG_UDP_PAYLOAD_RECEIVED:
                final String payload = (String) msg.obj;
                if (mLog.isD()) mLog.logD(TAG + ".doHandleMessage() UDP payload.length:" + payload.length());
                if (mState == ObjectState.STARTED) {
                    if (mListener != null) {
                        mListener.onPayloadReceived(payload);
                    }
                }
                break;
            default:
                break;
        }
    }

    private void doStop() {
        mWorkerImplementation.stop();
        mTransmitter = null;
    }

    @Override
    public String toString() {
        return "PayloadReceiverManager{" +
                "mListener=" + mListener +
                ", mState=" + mState +
                ", mTransmitter'" + mTransmitter + '\'' +
                '}';
    }
}
