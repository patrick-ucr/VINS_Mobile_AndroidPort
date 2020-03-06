package com.thkoeln.jmoeller.vins_mobile_androidport;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
        String TAG = "WiFiDirectBroadcastReceiver";

        private WifiP2pManager mManager;
        private WifiP2pManager.Channel mChannel;
        private MainActivity mActivity;
        private boolean ENABLED_P2P;
        private String thisDeviceMac;
        private WifiP2pManager.ConnectionInfoListener connectionListener = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                // InetAddress from WifiP2pInfo struct.
                String groupOwnerAddress = info.groupOwnerAddress.getHostAddress();
                Log.i(TAG, String.format("Group owner IP address %s",groupOwnerAddress));
                // After the group negotiation, we can determine the group owner
                // (server).
                if (info.groupFormed && info.isGroupOwner) {
                    // Do whatever tasks are specific to the group owner.
                    // One common case is creating a group owner thread and accepting
                    // incoming connections.
                    Log.i(TAG, "This is group owner");
                } else if (info.groupFormed) {
                    // The other device acts as the peer (client). In this case,
                    // you'll want to create a peer thread that connects
                    // to the group owner.
                    Log.i(TAG, "This is group client");
                }
            }
        };

        public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                           MainActivity activity) {
            super();
            this.mManager = manager;
            this.mChannel = channel;
            this.mActivity = activity;
            this.ENABLED_P2P = false;
            this.thisDeviceMac = "";
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    // Wifi P2P is enabled
                    Log.d(TAG, "Wifi P2P is enabled");
                    ENABLED_P2P = true;
                } else {
                    // Wi-Fi P2P is not enabled
                    Log.d(TAG, "Wifi P2P is NOT enabled!!");
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // Call WifiP2pManager.requestPeers() to get a list of current peers
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                // Respond to new connection or disconnectionsc
                if (mManager == null) {
                    return;
                }

                NetworkInfo networkInfo = (NetworkInfo) intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {

                    // We are connected with the other device, request connection
                    // info to find group owner IP

                    mManager.requestConnectionInfo(mChannel, connectionListener);
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                // Respond to this device's wifi state changing
                Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

                thisDeviceMac = device.deviceAddress;

                Log.d(TAG, "Device WiFi P2p MAC Address: " + thisDeviceMac);
            }
        }

    public boolean getP2pEnabled(){
            return ENABLED_P2P;
    }

    public String getThisDeviceMac() {
            return thisDeviceMac;
    }


}
