package com.samsung.dallas.networkutils.txrx;

import android.content.Context;

import java.net.Socket;
import java.util.List;

public interface WorkerTXInterface {
    void setDeviceName(String deviceName);
    void addToWhiteList(String deviceName);
    void removeFromWhiteList(String deviceName);
    void clearWhiteList();
    List<String> getWhiteList();
    void setCollisionDetectionEnabled(boolean enabled);
    void setMaxConnections(int max);
    boolean start(Context context, int handshakePort, int sendPort, int broadcastPort);
    void sendPayload(String payload);
    void sendPayload(TXRXDevice device, String payload);
    void broadcastPayload(String payload);
    void addTCPClient(Socket obj);
    void stop();
}
