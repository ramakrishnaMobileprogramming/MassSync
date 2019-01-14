package com.samsung.dallas.networkutils.txrx;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Vasudevan Ramachandraiah on 5/5/17.
 *
 * Listens for payload from UDP port(s) for the specified duration of time and notifies listener
 *
 */

public class UDPPortScanner {
    public interface Listener {
        void onDataReceived(int port, String data);
        void onStopped();
    }

    private static final String TAG = "UDPPortScanner";

    private final CountDownTimer mTimer;
    private final Set<Integer> mPorts;
    private final Logger mLog;
    private final List<Listener> mListeners;
    private final List<UDPReceiver> mUDPReceivers;
    private final Handler mHandler;

    private ObjectState mState;

    public UDPPortScanner(long durationMillis) {
        mTimer = new CountDownTimer(durationMillis, durationMillis) {
            @Override
            public void onTick(long millisUntilFinished) {
            }
            @Override
            public void onFinish() {
                doCancel();
                for (Listener l : mListeners) {
                    l.onStopped();
                }
            }
        };
        mPorts = new HashSet<Integer>();
        mLog = Logger.getInstance();
        mListeners = new ArrayList<Listener>();
        mUDPReceivers = new ArrayList<UDPReceiver>();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (mState == ObjectState.STARTED) {
                    for (Listener l : mListeners) {
                        l.onDataReceived(msg.what, (String) msg.obj);
                    }
                }
            }
        };

        mState = ObjectState.IDLE;
    }

    public boolean addPort(int port) {
        boolean result = false;
        if ((port >= 0) && (mState == ObjectState.IDLE)) {
            result = mPorts.add(port);
        }
        return result;
    }

    public void removePort(int port) {
        mPorts.remove(port);
    }

    public void addListener(Listener l) {
        if ((l != null) && (!mListeners.contains(l))) {
            mListeners.add(l);
        }
    }

    public void removeListener(Listener l) {
        if (l != null) {
            mListeners.remove(l);
        }
    }

    public boolean start(Context context) {
        boolean result = false;
        if ((mState == ObjectState.IDLE) && (context != null)) {
            if (mLog.isI()) mLog.logI(TAG  + ".start()");
            mState = ObjectState.STARTED;
            UDPReceiver r;
            for (int port : mPorts) {
                r = new UDPReceiver(mHandler);
                result = r.start(context, port, port);
                if (!result) {
                    break;
                }
                mUDPReceivers.add(r);
            }
            if (result) {
                mTimer.start();
            } else {
                doCancel();
            }
        }
        return result;
    }

    public void cancel() {
        if (mState == ObjectState.STARTED) {
            if (mLog.isI()) mLog.logI(TAG  + ".cancel()");
            doCancel();
        }
    }

    private void doCancel() {
        if (mLog.isD()) mLog.logD(TAG  + ".doCancel()");
        mState = ObjectState.IDLE;
        mTimer.cancel();
        // Remove all pending messages from handler
        for (int port : mPorts) {
            mHandler.removeMessages(port);
        }
        mPorts.clear();
        for (UDPReceiver r : mUDPReceivers) {
            r.stop();
        }
        mUDPReceivers.clear();
    }
}
