package com.macrokeysclient.fragments.servers;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.macrokeys.comunication.MacroClient;
import com.macrokeys.netcode.MacroNetClient;
import com.macrokeysclient.Connections;
import com.macrokeysclient.R;
import com.macrokeysclient.ServiceType;
import com.macrokeysclient.views.NetServersAdapter;
import com.macrokeysclient.views.ServerSelectionView;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;


/**
 * Implementation of {@link TabServersFragment} for the Wifi service
 */
public final class TabNetServFragment extends TabServersFragment {
    
    /**
     * Short timeout (in milliseconds) for the SSID service
     */
    private static final int TIMEOUT_SHORT_SSID = 1000;
    
    
    /** Server list */
    private ServerSelectionView listView;
    
    /** Last server this was connected; null if none */
    private InetSocketAddress lastServer;
    
    /** Asyncronous process to discover servers */
    private FindServerAsync findServerAsync;
    
    
    public TabNetServFragment() {
    
    }
    
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.i("TAB SERVERS", "Creating view for TCP/IP");
    
        // Listener for a click of an item
        AdapterView.OnItemClickListener lis = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent,
                                    View view, int position, long id) {
                Object parentItem = parent.getItemAtPosition(position);
                final MacroNetClient.SSDPServerInfo serverInfo =
                        (MacroNetClient.SSDPServerInfo) parentItem;
                startConnection(serverInfo.address);
            }
        };
        
        
        listView = new ServerSelectionView(getContext());
        listView.setServerClickListener(lis);
        
        // Executing here because listView now is set
        findServers(TIMEOUT_SHORT_SSID);
        
        return listView;
    }
    
    
    /**
     * Find the servers
     * @param maxWaitTime Timeout time for the server discovery
     */
    @Override
    public void findServers(int maxWaitTime) {
        // Case the a search already running
        if(findServerAsync != null &&
                findServerAsync.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }
        
        
        Context context = getActivity().getApplicationContext();
        final WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    
        // Wifi not enabled
        if(!wifi.isWifiEnabled()) {
            listView.setResultNoConnection(R.drawable.ic_signal_wifi_off,
                    "Wifi disabled",
                    "Enable Wifi",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            wifi.setWifiEnabled(true);
                        }
                    });
            return;
        }
    
    
        ConnectivityManager connManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    
        // Case not connected to an access point
        if (!netInfo.isConnected()) {
            listView.setResultNoConnection(R.drawable.ic_signal_wifi_0_bar,
                    "The device is not connected to a network",
                    "Select a network",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        }
                    });
            return;
        }
    
        findServerAsync = new FindServerAsync(this);
        findServerAsync.execute(maxWaitTime);
    }
    
    
    
    
    @Override
    protected void reconnect() {
        startConnection(lastServer);
    }
    
    
    
    
    @Override
    public @NonNull ServiceType getServiceType() {
        return ServiceType.Wifi;
    }
    
    
    /**
     * Start the connection to the given server
     * @param socket Socket of the server to connect to
     */
    private void startConnection(@NotNull InetSocketAddress socket) {
        if (Connections.getConnection() != null)
            throw new AssertionError("Connection already set");
        ConnectAsync async = new ConnectAsync(this);
        async.execute(socket);
    }
    
    
    
    
    
    
    /** Find the server and connect to one of them */
    private static class FindServerAsync
            extends AsyncTask<Integer, Void, MacroNetClient.SSDPServerInfo[]> {
        
        /** Exception generated while find the servers */
        IOException ex = null;
        
        private final TabNetServFragment fragment;
        
        
        public FindServerAsync(TabNetServFragment a) {
            this.fragment = a;
        }
        
        
        @Override
        protected void onPreExecute() {
            fragment.listView.showProgressBar();
        }
        
        @Override
        protected MacroNetClient.SSDPServerInfo[] doInBackground(Integer[] params) {
            MacroNetClient.SSDPServerInfo[] servers = null;
            
            // Find the address of the servers
            try {
                servers = MacroNetClient.findServer(params[0]);
            } catch (IOException e) {
                ex = e;
            }
            
            return servers;
        }
        
        @Override
        protected void onPostExecute(final MacroNetClient.SSDPServerInfo[] serverInfos) {
            if(ex == null && serverInfos.length > 0) {
                NetServersAdapter adapter = new NetServersAdapter(
                        fragment.getActivity(), serverInfos);
                fragment.listView.setResults(adapter);
            } else {
                fragment.listView.setResultNoResults();
            }
        }
    }
    
    
    
    
    /** Asyncronous process to connect to a server */
    private static class ConnectAsync
            extends AsyncTask<InetSocketAddress, Void, MacroClient> {
        private IOException exception;
        
        /** Dialog to show the progress of the connetion */
        private ProgressDialog prog;
        private TabNetServFragment fragment;
        private InetSocketAddress address;
        
        public ConnectAsync(TabNetServFragment fragment) {
            this.fragment = fragment;
        }
        
        
        @Override
        protected void onPreExecute() {
            prog = ProgressDialog.show(fragment.getActivity(),
                    "Connecting",
                    "Try to connect");
        }
        
        @Override
        protected MacroClient doInBackground(InetSocketAddress[] params) {
            exception = null;
            address = params[0];
            try {
                MacroNetClient c = new MacroNetClient(address);
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
            
            // If no errors occurred and the client is connected
            if(exception == null && client.isConnected()) {
                // Set the last connected server
                fragment.lastServer = address;
                Connections.setConnection(client);
                fragment.startMacroActivity();
            } else {
                Snackbar retry = Snackbar.make(fragment.listView,
                        "Unable to connect",
                        Snackbar.LENGTH_LONG);
                retry.setAction("Retry", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fragment.startConnection(address);
                    }
                });
                retry.show();
            }
        }
    }
    
}
