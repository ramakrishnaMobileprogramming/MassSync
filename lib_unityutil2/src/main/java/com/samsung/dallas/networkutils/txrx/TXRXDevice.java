package com.samsung.dallas.networkutils.txrx;

/**
 * Describes the device info
 */
public class TXRXDevice {
    /**
     * IPV4 address of the device
     */
    public String hostName;
    /**
     * Device name set
     */
    public String deviceName;

    @Override
    public String toString() {
        return "TXRXDevice{" +
                "hostName='" + hostName + '\'' +
                ", deviceName='" + deviceName + '\'' +
                '}';
    }
}
