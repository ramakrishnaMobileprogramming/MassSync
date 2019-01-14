package com.samsung.dallas.networkutils.txrx;

public interface Constants {
    interface MessageId {
        int MSG_START_RESULT_OK = 100;
        int MSG_START_RESULT_NOK = 101;
        int MSG_FATAL_ERROR = 102;
        int MSG_UDP_PAYLOAD_RECEIVED = 103;
        int MSG_TCP_CLIENT_CONNECTED = 103;
        int MSG_RECEIVER_CONNECTED = 104;
        int MSG_RECEIVERS_DISCONNECTED = 105;
        int MSG_TCP_PAYLOAD_RECEIVED = 106;
        int MSG_HANDSHAKE_RECEIVED = 107;
        int MSG_HANDSHAKE_SENT = 108;
    }

    ConnectionType DEFAULT_CONNECTION_TYPE = ConnectionType.TYPE_ETHERNET;
    int DEFAULT_MAX_RECEIVERS = Integer.MAX_VALUE;
}
