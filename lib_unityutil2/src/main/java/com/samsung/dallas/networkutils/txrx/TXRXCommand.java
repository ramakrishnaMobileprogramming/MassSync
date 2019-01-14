package com.samsung.dallas.networkutils.txrx;

import android.text.TextUtils;

public enum TXRXCommand {
    // Listed alphabetically
    BROADCAST_TX("txrx_broadcast_transmitter"),
    REGISTER_RX("txrx_register_receiver"),
    UNKNOWN("Unknown");

    private final String mCmd;

    TXRXCommand(String cmd) {
        mCmd = cmd;
    }

    public static TXRXCommand getByValue(String cmd) {
        for (TXRXCommand c: values()) {
            if (TextUtils.equals(c.mCmd, cmd)) {
                return c;
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return mCmd;
    }
}
