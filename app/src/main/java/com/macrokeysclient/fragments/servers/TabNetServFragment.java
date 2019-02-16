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
 * Implementazione di {@link TabServersFragment} per il servizio Wifi
 */
public final class TabNetServFragment extends TabServersFragment {
    
    /** Tempo di attesa prima di interrompere la scoperta del Server, in millisecondi;
     * versione più corta */
    private static final int TIMEOUT_SHORT_SSID = 1000;
    
    
    /** Lista dei server */
    private ServerSelectionView listView;
    
    /** Ultimo server al quale si era connessi; null se non si è mai stati connessi ad un server */
    private InetSocketAddress lastServer;
    
    /** Processo asincrono per la ricerca dei server */
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
        
        
        AdapterView.OnItemClickListener lis; //Listener per il clik di un'item
        lis = new AdapterView.OnItemClickListener() {
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
        
        // Eseguo qui siccome listView adesso è settata
        findServers(TIMEOUT_SHORT_SSID);
        
        return listView;
    }
    
    
    /**
     * Consente di effettuare la ricerca dei server
     * @param maxWaitTime Massimo tempo da aspettare
     */
    @Override
    public void findServers(int maxWaitTime) {
        // Caso ricerca già avviata
        if(findServerAsync != null &&
                findServerAsync.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }
        
        
        Context context = getActivity().getApplicationContext();
        final WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    
        // Wifi non abilitato
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
    
        // Non connesso a nessun accesspoint
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
     * Fa partire la connessione per il server TCP/IP indicato
     * @param socket Socket del server alla quale connetersi
     */
    private void startConnection(@NotNull InetSocketAddress socket) {
        if (Connections.getConnection() != null)
            throw new AssertionError("Connection already set");
        ConnectAsync async = new ConnectAsync(this);
        async.execute(socket);
    }
    
    
    
    
    
    
    /** Permette di trovare i Server ed eventualmente connettersi ad uno di essi */
    private static class FindServerAsync
            extends AsyncTask<Integer, Void, MacroNetClient.SSDPServerInfo[]> {
        
        /** Eccezione generata per trovre il server */
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
            //Trovo l'indirizzo dei server
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
    
    
    
    
    /** Permette di connettersi asincronamente al Server */
    private static class ConnectAsync
            extends AsyncTask<InetSocketAddress, Void, MacroClient> {
        private IOException exception;
        
        /** Finestrta che mostra il progresso dell'attività */
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
            // Se non ci sono stati errori e il client è connesso
            if(exception == null && client.isConnected()) {
                // Indico l'ultimo server a cui si è connessi
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
