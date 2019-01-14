package com.samsung.dallas.networkutils.txrx;

import android.util.Log;

public final class Logger {
    private static final String TAG = "Logger";
    private static Logger mSingleton;

    private int mLogLevel;

    private Logger() {
        mLogLevel = BuildConfig.DEBUG ? Log.DEBUG : Log.INFO;
    }

    public static Logger getInstance() {
        if (mSingleton == null) {
            synchronized (Logger.class) {
                if (mSingleton == null) {
                    mSingleton = new Logger();
                }
            }
        }
        return mSingleton;
    }

    public void setLogLevel(int mLogLevel) {
        this.mLogLevel = mLogLevel;
    }

    public boolean isV() {
        return mLogLevel <= Log.VERBOSE;
    }

    public void logV(String msg) {
        if (mLogLevel <= Log.VERBOSE) {
            Log.v(TAG, msg);
        }
    }

    public boolean isD() {
        return mLogLevel <= Log.DEBUG;
    }

    public void logD(String msg) {
        if (mLogLevel <= Log.DEBUG) {
            Log.d(TAG, msg);
        }
    }

    public boolean isI() {
        return mLogLevel <= Log.INFO;
    }

    public void logI(String msg) {
        if (mLogLevel <= Log.INFO) {
            Log.i(TAG, msg);
        }
    }

    public boolean isE() {
        return mLogLevel <= Log.ERROR;
    }

    public void logE(String msg) {
        if (mLogLevel <= Log.ERROR) {
            Log.e(TAG, msg);
        }
    }
}
