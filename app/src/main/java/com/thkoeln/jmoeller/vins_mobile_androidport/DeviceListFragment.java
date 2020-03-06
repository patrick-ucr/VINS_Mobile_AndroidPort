package com.thkoeln.jmoeller.vins_mobile_androidport;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.ListFragment;

import java.util.ArrayList;
import java.util.List;

public class DeviceListFragment extends ListFragment implements WifiP2pManager.PeerListListener {

    private final String TAG = "DeviceListFragment";
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private WifiP2pDevice device;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        // Defines the xml file for the fragment
        return inflater.inflate(R.layout.device_list_fragment, parent, false);
    }

    // This event is triggered soon after onCreateView().
    // Any view setup should occur here.  E.g., view lookups and attaching view listeners.
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Setup any handles to view objects here
        // EditText etFoo = (EditText) view.findViewById(R.id.etFoo);
    }

    public WifiP2pDevice getDevice() {
        return device;
    }

    public List<WifiP2pDevice> getPeers() {
        return peers;
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        Log.d(TAG, String.format("Wifi P2p Peerlist: %s",peerList.toString()));
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        if (peers.size() == 0) {
            Log.d(TAG, "No devices found");
            return;
        }
    }
}
