package com.macrokeysclient.fragments.servers;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;


import com.macrokeys.comunication.MacroClient;
import com.macrokeysclient.Connections;
import com.macrokeysclient.R;
import com.macrokeysclient.ServiceType;
import com.macrokeysclient.comunication.MacroBluetoothClient;
import com.macrokeysclient.views.BluetoothServerAdapter;
import com.macrokeysclient.views.ServerSelectionView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the {@link TabServersFragment} for the Bluetooth service
 */
public final class TabBluetoohtServFragment extends TabServersFragment {
    
    /** Timeout to find the server */
    private static final int TIMEOUT_FIND = 5000;
    
    /** {@link BroadcastReceiver} to find the Bluetooth devices */
    private final FindDevices brodRecFind = new FindDevices();
    
    /** {@link BroadcastReceiver} for enabling the Bluetooth adapter*/
    private final EnableAdapter brodRecEnable = new EnableAdapter();
    
    /** Server list */
    private ServerSelectionView serversView;
    
    /** Bluetooth adapter*/
    private BluetoothAdapter adapter;
    
    /** Last connected server to this; null if none */
    private BluetoothDevice lastServer;
    
    /** Async process to find the servers */
    private FindServerAsync findServerAsync;
    
    
    public TabBluetoohtServFragment() {
    
    }
    
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        adapter = BluetoothAdapter.getDefaultAdapter();
    
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(brodRecFind, filter);
    
        IntentFilter filterEnable = new IntentFilter();
        filterEnable.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(brodRecEnable, filterEnable);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.i("TAB SERVERS", "Creating view for bluetooth");
        
        
        
        AdapterView.OnItemClickListener lis = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent,
                                    View view, int position, long id) {
                Object parentItem = parent.getItemAtPosition(position);
                final BluetoothDevice server = (BluetoothDevice) parentItem;
                startConnection(server);
            }
        };
    
    
    
    
        serversView = new ServerSelectionView(getContext());
        serversView.setServerClickListener(lis);
    
        TextView txt = new TextView(getContext());
        txt.setText("Paired devices:");
        serversView.addHeaderView(txt);
        
        // Executing here bacause listView now is setted
        findServers(TIMEOUT_FIND);
        
        return serversView;
    }
    
    /**
     * Starts the connection for the given Bluetooth server
     * @param device Server device
     */
    private void startConnection(BluetoothDevice device) {
        if (Connections.getConnection() != null)
            throw new AssertionError("Connection already set");
        ConnectAsync async = new ConnectAsync(this);
        async.execute(device);
    }
    
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        getActivity().unregisterReceiver(brodRecFind);
        getActivity().unregisterReceiver(brodRecEnable);
    }
    
    /**
     * Search the reserch of the servers
     * @param maxWaitTime Find timeout for the server
     */
    @Override
    public void findServers(int maxWaitTime) {
        // Search already started
        if(findServerAsync != null &&
                findServerAsync.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }
        
        // Bluetooth not supported by this device
        if(adapter == null) {
            serversView.setResultNoConnection(R.drawable.ic_bluetooth_disabled,
                    "This device does not have a bluetooth adapter",
                    null,
                    null);
            return;
        }
        // Bluetooth disabled on this device
        if(!adapter.isEnabled()) {
            serversView.setResultNoConnection(R.drawable.ic_bluetooth_disabled,
                    "Bluetooth disabled",
                    "Enable bluetooth",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            adapter.enable();
                        }
                    });
            return;
        }
    
        findServerAsync = new FindServerAsync(this);
        findServerAsync.execute(maxWaitTime);
    }
    
    @Override
    protected void reconnect() {
        if(lastServer != null) {
            startConnection(lastServer);
        }
    }
    
    @Override
    public @NonNull ServiceType getServiceType() {
        return ServiceType.Bluetooth;
    }
    
    
    
    /** Find the servers and connect to one of them */
    private static class FindServerAsync
            extends AsyncTask<Integer, Void, BluetoothDevice[]> {
        
        
        private final TabBluetoohtServFragment fragment;
        
        
        public FindServerAsync(TabBluetoohtServFragment a) {
            this.fragment = a;
        }
        
        
        @Override
        protected void onPreExecute() {
            fragment.serversView.showProgressBar();
        }
        
        @Override
        protected BluetoothDevice[] doInBackground(Integer[] params) {
            return fragment.brodRecFind.findServers(fragment.adapter, params[0]);
        }
        
        @Override
        protected void onPostExecute(final BluetoothDevice[] servers) {
            if(servers.length > 0) {
                fragment.serversView.setResults(new BluetoothServerAdapter(
                        fragment.getContext(), servers));
            } else {
                fragment.serversView.setResultNoResults();
            }
        }
    }
    
    
    /**
     * Receiver used to handle the devices not paired
     */
    private static class FindDevices extends BroadcastReceiver {
    
        final List<BluetoothDevice> devices = new ArrayList<>();
        
        public BluetoothDevice[] findServers(BluetoothAdapter adapter, int maxWaitTime) {
            devices.clear();
            
            // Add devices already peered
            devices.addAll(adapter.getBondedDevices());
            /*
            adapter.startDiscovery();
            try {
                Thread.sleep(maxWaitTime);
            } catch (InterruptedException ignored) { }
            adapter.cancelDiscovery();
            */
            
            BluetoothDevice[] d = new BluetoothDevice[devices.size()];
            devices.toArray(d);
            return d;
        }
        
        
        
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devices.add(device);
            }
        }
    }
    
    
    /**
     * Receiver uased to update the list of available Bluetooth devices
     */
    private class EnableAdapter extends BroadcastReceiver {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    TabBluetoohtServFragment.this.findServers(TIMEOUT_FIND);
                }
            }
        }
    }
    
    
    
    /** Asyconous connection to a server */
    private static class ConnectAsync
            extends AsyncTask<BluetoothDevice, Void, MacroClient> {
        private IOException exception;
        
        /** Dialog to show the connection progress */
        private ProgressDialog prog;
        private TabBluetoohtServFragment fragment;
        private BluetoothDevice server;
        
        public ConnectAsync(TabBluetoohtServFragment fragment) {
            this.fragment = fragment;
        }
        
        
        @Override
        protected void onPreExecute() {
            prog = ProgressDialog.show(fragment.getActivity(),
                    "Connecting",
                    "Try to connect");
        }
        
        @Override
        protected MacroClient doInBackground(BluetoothDevice[] params) {
            exception = null;
            server = params[0];
            try {
                MacroBluetoothClient c = new MacroBluetoothClient(server);
                c.connectToServer();
                return c;
            } catch (IOException e) {
                exception = e;
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(MacroClient client) {
            prog.dismiss();
            
            // No errors and the client is connected
            if(exception == null && client.isConnected()) {
                Connections.setConnection(client);
                
                // Set the last conneted server
                fragment.lastServer = server;
                fragment.startMacroActivity();
            } else {
                Snackbar retry = Snackbar.make(fragment.serversView,
                        "Unable to connect",
                        Snackbar.LENGTH_LONG);
                retry.setAction("Retry", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fragment.startConnection(server);
                    }
                });
                retry.show();
            }
        }
    }
}
