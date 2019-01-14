package com.samsung.dallas.networkutils.txrx;

/**
 * Created by Vasudevan Ramachandraiah on 5/8/17.
 */

public class TransmitterDevice extends TXRXDevice {

    public int handshakePort;
    public int sendPort;
    public int broadcastPort;
    public long timestamp;
    public boolean acceptingConnection;

    @Override
    public String toString() {
        return "TransmitterDevice{" +
                super.toString() +
                ", handshakePort=" + handshakePort +
                ", sendPort=" + sendPort +
                ", broadcastPort=" + broadcastPort +
                ", timestamp=" + timestamp +
                ", acceptingConnection=" + acceptingConnection +
                '}';
    }
}
