package com.thkoeln.jmoeller.vins_mobile_androidport;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class PeerManager implements WifiP2pManager.PeerListListener {
    final String TAG = "PeerManager";
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        Log.d(TAG, String.format("onPeersAvailable() peerList: %s",peerList.toString()));
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        if (peers.size() == 0) {
            Log.d(TAG, "No WiFi P2P devices found");
            return;
        }
    }

    public List<WifiP2pDevice> getPeerList(){
        return peers;
    }
}
