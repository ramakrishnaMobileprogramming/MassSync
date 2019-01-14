package vrsync.samsung.com.vrsync.Utils;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.samsung.dallas.networkutils.txrx.TXRXDevice;


public class ConnectedDeviceExt {
    public TXRXDevice connectedDevice;
    public boolean selected;
    private boolean playPauseState=false;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (this.getClass() != obj.getClass()) return false;
        final ConnectedDeviceExt cde = (ConnectedDeviceExt)obj;
        return (this.selected == cde.selected)
                && TextUtils.equals(this.connectedDevice.hostName, cde.connectedDevice.hostName)
                && TextUtils.equals(this.connectedDevice.deviceName, cde.connectedDevice.deviceName);
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString();
    }

    public boolean getPlayPauseState() {
        return playPauseState;
    }

    public void setPlayPauseState(boolean playPauseState) {
        this.playPauseState = playPauseState;
    }
}