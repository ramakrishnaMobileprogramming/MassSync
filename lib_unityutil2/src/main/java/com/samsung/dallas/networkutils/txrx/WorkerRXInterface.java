package com.samsung.dallas.networkutils.txrx;

import android.content.Context;

public interface WorkerRXInterface {
    void setDeviceName(String deviceName);
    void setAcceptPayloadFromAll(boolean allow);

    void start(Context context, int txShakeHandPort);
    void stop();
}
