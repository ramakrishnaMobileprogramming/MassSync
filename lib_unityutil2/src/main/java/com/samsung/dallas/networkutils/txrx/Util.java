package com.samsung.dallas.networkutils.txrx;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

public class Util {
    private static final String TAG = "Util";

    public static final Charset PAYLOAD_CHARSET = Charset.forName("UTF-8");
    public static final int PAYLOAD_BYTES_BUFFER_SIZE = 4096;

    public static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                final Logger l = Logger.getInstance();
                if (l.isE()) l.logE(TAG + ".safeClose() " + e.getMessage());
            }
        }
    }

    public static String createPayloadBroadcastTransmitter(Context context, String deviceName, int sendPort, int broadcastPort, long timestamp, boolean acceptingConnection) {
        return createPayloadBroadcastTransmitter(getLocalIP(context), deviceName, sendPort, broadcastPort, timestamp, acceptingConnection);
    }

    public static String createPayloadBroadcastTransmitter(String hostName, String deviceName, int sendPort, int broadcastPort, long timestamp, boolean acceptingConnection) {
        String payload = null;
        try {
            // TODO use Gson to marshal from POJO
            final JSONObject dataJSON = new JSONObject();
            dataJSON.put("deviceName", (deviceName != null ? deviceName : ""));
            dataJSON.put("ip", hostName);
            dataJSON.put("sendPort", sendPort);
            dataJSON.put("broadcastPort", broadcastPort);
            dataJSON.put("timestamp", timestamp);
            dataJSON.put("acceptingConnection", acceptingConnection);
            payload = createPayload(TXRXCommand.BROADCAST_TX.toString(), dataJSON.toString());
        } catch (JSONException e) {
            final Logger l = Logger.getInstance();
            if (l.isE()) l.logE(TAG + "createPayloadBroadcastTransmitter() " + e.getMessage());
        }
        return payload;
    }

    public static String createPayloadRegisterReceiver(String deviceName) {
        String payload = null;
        try {
            // TODO use Gson to marshal from POJO
            final JSONObject dataJSON = new JSONObject();
            dataJSON.put("deviceName", (deviceName != null ? deviceName : ""));

            payload = createPayload(TXRXCommand.REGISTER_RX.toString(), dataJSON.toString());
        } catch (JSONException e) {
            final Logger l = Logger.getInstance();
            if (l.isE()) l.logE(TAG + "createPayloadRegisterReceiver()" + e.getMessage());
        }
        return payload;
    }

    public static String createPayload(String cmd, String data) {
        JSONObject cmdJSON = new JSONObject();
        try {
            cmdJSON.put("cmd", cmd);
            cmdJSON.put("data", data);
        } catch (JSONException e) {
            final Logger l = Logger.getInstance();
            if (l.isE()) l.logE(TAG + "createPayload() " + e.getMessage());
            cmdJSON = null;
        }
        return cmdJSON != null ? cmdJSON.toString() : null;
    }

    public static Pair<TXRXCommand, String> parseTXRXCommand(String payload) {
        if (!TextUtils.isEmpty(payload)) {
            try {
                final JSONObject payloadJSON = new JSONObject(payload);
                if (payloadJSON.has("cmd") && payloadJSON.has("data")) {
                    final TXRXCommand cmd = TXRXCommand.getByValue(payloadJSON.optString("cmd"));
                    if (cmd != TXRXCommand.UNKNOWN) {
                        return new Pair<TXRXCommand, String>(cmd, payloadJSON.optString("data"));
                    }
                }
            } catch (JSONException e) {
                final Logger l = Logger.getInstance();
                if (l.isE()) l.logE(TAG + "parseTXRXCommand() " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Creates JSON object from string specified.
     *
     * @param jsonString
     * @return null if string is not a valid json
     */
    public static JSONObject createJSON(String jsonString) {
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                return new JSONObject(jsonString);
            } catch (JSONException e) {
                final Logger l = Logger.getInstance();
                if (l.isE()) l.logE(TAG + "createJSON() " + e.getMessage());
            }
        }
        return null;
    }

    public static TransmitterDevice create(JSONObject jsonObject) {
        final TransmitterDevice cd = new TransmitterDevice();
        if (jsonObject != null) {
            cd.hostName = jsonObject.optString("ip");
            cd.deviceName = jsonObject.optString("deviceName");
            cd.sendPort = jsonObject.optInt("sendPort");
            cd.broadcastPort = jsonObject.optInt("broadcastPort");
            cd.timestamp = jsonObject.optLong("timestamp");
            cd.acceptingConnection = jsonObject.optBoolean("acceptingConnection");
        }
        return cd;
    }

    /**
     *
     * @param src Input stream to read data from.
     * @param dataBuffer Destination buffer to read data to
     * @return number of payload bytes read from data input stream
     * @throws IOException
     */
    public static int readBlocking(DataInputStream src, byte[] dataBuffer) throws IOException {
        // Protocol length of bytes followed by actual data string
        int payloadBytesLength = src.readInt(); // Blocking
        // To support reading beyond scratch.length bytes, remove this check
        if ((payloadBytesLength > 0) && (payloadBytesLength <= dataBuffer.length)) {
            final int bytesToReadLength = Math.min(payloadBytesLength, dataBuffer.length);
            final int bytesRead = src.read(dataBuffer, 0, bytesToReadLength); // Blocking
            if (bytesRead != bytesToReadLength) { // If we could not read expected number of bytes
                final Logger l = Logger.getInstance();
                if (l.isE()) l.logE("Error reading payload. Expected bytes.length:" + payloadBytesLength
                        + ". Read bytes.length:" + bytesRead);
                payloadBytesLength = 0;
            }
        } else {
            final Logger l = Logger.getInstance();
            if (l.isE()) l.logE("Available length "+ payloadBytesLength +" greater than data buffer length:" + dataBuffer.length);
            payloadBytesLength = 0;
        }
        return payloadBytesLength;
    }

    /**
     * 
     * @param src Input stream to read data from.
     * @param scratch Scratch buffer to hold temporary data
     * @param dest Contains data string read from input stream 
     * @return number of payload bytes read from data input stream
     * @throws IOException
     */
    public static int readBlocking(DataInputStream src, byte[] scratch, StringBuilder dest) throws IOException {
        int result = 0;
        // Protocol length of bytes followed by actual data string
        final int payloadBytesLength = src.readInt(); // Blocking
        // To support reading beyond scratch.length bytes, remove this check
        if (payloadBytesLength <= scratch.length) {
            result = payloadBytesLength;
            int availableBytesLength = payloadBytesLength;
            int bytesToReadLength = Math.min(payloadBytesLength, scratch.length);
            int bytesRead = 0;
            while (bytesToReadLength > 0) {
                bytesRead = src.read(scratch, 0, bytesToReadLength); // Blocking
                if (bytesRead == bytesToReadLength) { // If we have read expected number of bytes
                    dest.append(new String(scratch, 0, bytesToReadLength, Util.PAYLOAD_CHARSET));
                    availableBytesLength -= bytesToReadLength;
                    bytesToReadLength = Math.min(availableBytesLength, scratch.length);
                } else {
                    final Logger l = Logger.getInstance();
                    if (l.isE()) l.logE("Expected payloadBytes.length:" + payloadBytesLength +". Error reading data string");
                    dest.delete(0, dest.length());
                    result = 0;
                    break;
                }
            }
        }
        return result;
    }

    public static InetAddress getWiFiDHCPBroadcastAddress(Context context) {
        if (context != null) {
            final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                final NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                if ((info != null) && (info.getType() == ConnectivityManager.TYPE_WIFI) && info.isConnected()) {
                    final WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wifiManager != null) {
                        final DhcpInfo dhcp = wifiManager.getDhcpInfo();
                        final int broadcast = dhcp.ipAddress & dhcp.netmask | ~dhcp.netmask;
                        final byte[] quads = new byte[4];
                        for (int k = 0; k < 4; ++k) {
                            quads[k] = (byte) (broadcast >> k * 8 & 255);
                        }
                        try {
                            return InetAddress.getByAddress(quads);
                        } catch (UnknownHostException e) {
                            final Logger l = Logger.getInstance();
                            if (l.isE())
                                l.logE(TAG + ".getWifiDHCPBroadcastAddress(): " + e.getMessage());
                        }
                    }
                }
            }
        }
        return null;
    }

    public static String getLocalIP(Context context) {
        if (context != null) {
            final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (connectivityManager != null && wifiManager != null) {
                final NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                if ((info != null) && (info.getType() == ConnectivityManager.TYPE_WIFI) && info.isConnected()) {
                    final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if (wifiInfo != null) {
                        return Formatter.formatIpAddress(wifiInfo.getIpAddress());
                    }
                }
            }
        }
        return null;
    }

    public static class SocketHolder implements Closeable {
        public String deviceName;
        public String hostName;
        public Socket socket;
        public DataInputStream dataInputStream;
        public DataOutputStream dataOutputStream;

        @Override
        public String toString() {
            return "SocketHolder{" +
                    "host='" + hostName + '\'' +
                    ", deviceName='" + deviceName + '\'' +
                    '}';
        }

        @Override
        public void close() throws IOException {
            Util.safeClose(dataOutputStream);
            Util.safeClose(dataInputStream);
            Util.safeClose(socket);
        }
    }
}
