package com.samsung.dallas.networkutils.txrx.wifip2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Looper;
import android.text.TextUtils;

import com.samsung.dallas.networkutils.txrx.Logger;
import com.samsung.dallas.networkutils.txrx.ObjectState;
import com.samsung.dallas.networkutils.txrx.TXRXCommand;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class WiFiDirectUtil {

    public interface Listener {
        void onWiFiP2PStateChanged(int wiFiP2PState);
        void onWiFiP2PGroupStateChanged(WifiP2pInfo wifiP2pInfo, NetworkInfo networkInfo);
        void onServicePayload(String payload);
    }

    private static final String TAG = "WiFiDirectUtil";

    private final String mServiceName;
    private final String mServiceType;
    private final Logger mLog;
    private final IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver;

    private ObjectState mState;
    private WeakReference<Context> mContextRef;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private int mWiFiP2PState;
    private WifiP2pInfo mWiFiP2PInfo;
    private NetworkInfo mNetworkInfo;
    private final Map mRecord = new HashMap();
    private WifiP2pDnsSdServiceInfo mRegisterServiceInfo;
    private WifiP2pManager.ActionListener mRegSrvcRemoveServiceListener;
    private WifiP2pManager.ActionListener mRegSrvcAddServiceListener;
    private WifiP2pManager.ActionListener mUnregSrvcRemoveServiceListener;
    private WifiP2pDnsSdServiceRequest mDiscoverServiceRequest;
    private Map<String, String> mDiscoveredServiceTextRecordMap;
    private WifiP2pManager.DnsSdServiceResponseListener mServiceResponseListener;
    private WifiP2pManager.DnsSdTxtRecordListener mDnsSdTxtRecdListener;
    private WifiP2pManager.ActionListener mDiscSrvcRemoveServiceRequestListener;
    private WifiP2pManager.ActionListener mDiscSrvcAddServiceRequestListener;
    private WifiP2pManager.ActionListener mDiscSrvcDiscoverServiceRequestListener;
    private WifiP2pManager.ActionListener mCancelDiscSrvcRemoveServiceRequestListener;

    private Listener mListener;

    public WiFiDirectUtil() {
        mServiceName = TXRXCommand.BROADCAST_TX.toString();
        mServiceType = "_presentation._tcp";
        mLog = Logger.getInstance();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ((context != null) && (intent != null)) {
                    onWiFiP2PEvent(intent);
                } else {
                    if (mLog.isE()) mLog.logE(TAG + ".onReceive() Null context or intent");
                }
            }
        };
        mWiFiP2PState = -1;
        initializeServiceManagementListeners();
        initializeDiscoveryListeners();
        mState = ObjectState.IDLE;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void startMonitoring(Context context, Looper looper) {
        if (mState == ObjectState.IDLE) {
            mContextRef = new WeakReference<Context>(context);
            if (context != null) {
                mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
                mChannel = mManager.initialize(context, looper, null);
                context.registerReceiver(mReceiver, mIntentFilter);
                mState = ObjectState.STARTED;
            }
        }
    }

    public int getWiFiP2PState() {
        return mWiFiP2PState;
    }

    public WifiP2pInfo getWiFiP2PInfo() {
        return mWiFiP2PInfo;
    }

    public NetworkInfo getNetworkInfo() {
        return mNetworkInfo;
    }

    public void stopMonitoring() {
        if (mState == ObjectState.STARTED) {
            final Context context = mContextRef != null ? mContextRef.get() : null;
            if (context != null) {
                context.unregisterReceiver(mReceiver);
            }
            mState = ObjectState.IDLE;
            mManager = null;
            mChannel = null;
            mWiFiP2PState = -1;
            mWiFiP2PInfo = null;
            mNetworkInfo = null;
            mDiscoveredServiceTextRecordMap = null;
        }
    }

    public void registerService(String payload) {
        if (mLog.isD()) mLog.logD(TAG  + ".registerService() state " + mState);
        if (mState == ObjectState.STARTED) {
            // For service register/discovery
            mRecord.put("payload", payload);
            mRegisterServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(mServiceName, mServiceType, mRecord);
            // unregister if any
            mManager.removeLocalService(mChannel, mRegisterServiceInfo, mRegSrvcRemoveServiceListener);
        }
    }

    public void unregisterService() {
        if (mLog.isD()) mLog.logD(TAG  + ".unregisterService() state " + mState);
        if (mState == ObjectState.STARTED) {
            if (mRegisterServiceInfo != null) {
                mManager.removeLocalService(mChannel, mRegisterServiceInfo, mUnregSrvcRemoveServiceListener);
            }
        }
    }

    public void discoverService() {
        if (mLog.isD()) mLog.logD(TAG  + ".discoverService() state " + mState);
        if (mState == ObjectState.STARTED) {
            mDiscoveredServiceTextRecordMap = null;
            mManager.setDnsSdResponseListeners(mChannel, mServiceResponseListener, mDnsSdTxtRecdListener);
            mDiscoverServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
            mManager.removeServiceRequest(mChannel, mDiscoverServiceRequest, mDiscSrvcRemoveServiceRequestListener);
        }
    }

    public Map<String, String> getServiceTextRecordMap() {
        return mDiscoveredServiceTextRecordMap;
    }

    public void cancelDiscoverService() {
        if (mLog.isD()) mLog.logD(TAG  + ".cancelDiscoverService() state " + mState);
        if (mState == ObjectState.STARTED) {
            if (mDiscoverServiceRequest != null) {
                mManager.removeServiceRequest(mChannel, mDiscoverServiceRequest, mCancelDiscSrvcRemoveServiceRequestListener);
            }
        }
    }

    private void onWiFiP2PEvent(Intent intent) {
        if (mState == ObjectState.STARTED) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                mWiFiP2PState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (mLog.isI()) mLog.logI(TAG + ".onWiFiP2PEvent WIFI_P2P_STATE_CHANGED_ACTION - " + mWiFiP2PState);
                if (mListener != null) {
                    mListener.onWiFiP2PStateChanged(mWiFiP2PState);
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if (mLog.isD()) mLog.logD(TAG + ".onWiFiP2PEvent WIFI_P2P_PEERS_CHANGED_ACTION");
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                if (mLog.isI()) mLog.logI(TAG + ".onWiFiP2PEvent WIFI_P2P_CONNECTION_CHANGED_ACTION");
                final WifiP2pInfo wifiP2pInfo = mWiFiP2PInfo;
                mWiFiP2PInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                mNetworkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (((wifiP2pInfo == null) && mWiFiP2PInfo.groupFormed)
                        || ((wifiP2pInfo != null) && (wifiP2pInfo.groupFormed != mWiFiP2PInfo.groupFormed))) {
                    if (mListener != null) {
                        mListener.onWiFiP2PGroupStateChanged(mWiFiP2PInfo, mNetworkInfo);
                    }
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                if (mLog.isI()) mLog.logI(TAG + ".onWiFiP2PEvent WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            }
        }
    }

    private void initializeServiceManagementListeners() {
        mRegSrvcRemoveServiceListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (mState == ObjectState.STARTED) {
                    mManager.addLocalService(mChannel, mRegisterServiceInfo, mRegSrvcAddServiceListener);
                }
            }
            @Override
            public void onFailure(int reason) {
                if (mLog.isE()) mLog.logE(TAG + ".onSuccess() registerService - Error removing service. Reason " + reason);
                // TODO notify listener here
            }
        };
        mRegSrvcAddServiceListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (mLog.isI()) mLog.logI(TAG + ".onSuccess() registerService - Added service successfully");
                // Service successfully registered
            }
            @Override
            public void onFailure(int reason) {
                if (mLog.isE()) mLog.logE(TAG + ".onSuccess() registerService - Error adding service. Reason " + reason);
                // TODO notify listener here....
            }
        };
        mUnregSrvcRemoveServiceListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (mLog.isI()) mLog.logI(TAG + ".onSuccess() unregisterService - Removed service successfully");
            }
            @Override
            public void onFailure(int reason) {
                if (mLog.isE()) mLog.logE(TAG + ".onSuccess() unregisterService - Error removing service. Reason " + reason);
            }
        };
    }

    private void initializeDiscoveryListeners() {
        mDnsSdTxtRecdListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
                if (mLog.isI()) mLog.logI(TAG + ".onDnsSdTxtRecordAvailable() fullDomainName:" + fullDomainName
                        + ", srcDevice.deviceName:" + srcDevice.deviceName);
                if (mState == ObjectState.STARTED) {
                    mDiscoveredServiceTextRecordMap = txtRecordMap;
                    // .local. is required
                    if (!TextUtils.isEmpty(fullDomainName) && fullDomainName.equals(mServiceName + "." + mServiceType + ".local.")) {
                        final String payload = txtRecordMap != null ? txtRecordMap.get("payload") : null;
                        if (!TextUtils.isEmpty(payload) && (mListener != null)) {
                            mListener.onServicePayload(payload);
                        }
                    }
                }
            }
        };
        mServiceResponseListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice resourceType) {
            }
        };
        mDiscSrvcRemoveServiceRequestListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (mState == ObjectState.STARTED) {
                    mManager.addServiceRequest(mChannel, mDiscoverServiceRequest, mDiscSrvcAddServiceRequestListener);
                }
            }
            @Override
            public void onFailure(int reason) {
                if (mLog.isE()) mLog.logE(TAG + ".onSuccess() discoverService - Error removing service. Reason " + reason);
                // TODO notify listener here
            }
        };
        mDiscSrvcAddServiceRequestListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (mState == ObjectState.STARTED) {
                    mManager.discoverServices(mChannel, mDiscSrvcDiscoverServiceRequestListener);
                }
            }
            @Override
            public void onFailure(int reason) {
                if (mLog.isE()) mLog.logE(TAG + ".onSuccess() discoverService - Error adding service. Reason " + reason);
                // TODO notify listener here
            }
        };
        mDiscSrvcDiscoverServiceRequestListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (mLog.isI()) mLog.logI(TAG + ".onSuccess() discoverService  - Discovering service successfully");
            }
            @Override
            public void onFailure(int reason) {
                if (mLog.isE()) mLog.logE(TAG + ".onSuccess() discoverService - Error discovering service. Reason " + reason);
                // TODO notify listener here
            }
        };
        mCancelDiscSrvcRemoveServiceRequestListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (mLog.isI()) mLog.logI(TAG + ".onSuccess() cancelDiscoverService  - success");
            }
            @Override
            public void onFailure(int reason) {
                if (mLog.isE()) mLog.logE(TAG + ".onSuccess() cancelDiscoverService - Error canceling discover service. Reason " + reason);
            }
        };
    }
}