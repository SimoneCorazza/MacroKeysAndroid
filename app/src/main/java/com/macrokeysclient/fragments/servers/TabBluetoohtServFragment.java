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
 * Implementazione di {@link TabServersFragment} per il servizio Bluetooth
 */
public final class TabBluetoohtServFragment extends TabServersFragment {
    
    /** Timeout per trovare i server */
    private static final int TIMEOUT_FIND = 5000;
    
    /** {@link BroadcastReceiver} per i la ricerca di dispositivi Bluetooth */
    private final FindDevices brodRecFind = new FindDevices();
    
    /** {@link BroadcastReceiver} per l'abilitazione dell'adapter Bluetooth */
    private final EnableAdapter brodRecEnable = new EnableAdapter();
    
    /** Lista dei server */
    private ServerSelectionView serversView;
    
    /** Adapter per il bluetooth*/
    private BluetoothAdapter adapter;
    
    /** Ultimo server al quale si è stati connessi; null se nessuno */
    private BluetoothDevice lastServer;
    
    /** Progesso asincrono per trovare i server */
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
        
        // Eseguo qui siccome listView adesso è settata
        findServers(TIMEOUT_FIND);
        
        return serversView;
    }
    
    /**
     * Fa partire la connessione per il server Bluetooth indicato
     * @param device Dispositivo server
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
        
        // Niente bluetooth sul device
        if(adapter == null) {
            serversView.setResultNoConnection(R.drawable.ic_bluetooth_disabled,
                    "This device does not have a bluetooth adapter",
                    null,
                    null);
            return;
        }
        // Bluetooth disabilitato
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
    
    
    
    /** Permette di trovare i Server ed eventualmente connettersi ad uno di essi */
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
     * Receiver utilizzato per coprire i devices non pairati
     */
    private static class FindDevices extends BroadcastReceiver {
    
        final List<BluetoothDevice> devices = new ArrayList<>();
        
        public BluetoothDevice[] findServers(BluetoothAdapter adapter, int maxWaitTime) {
            devices.clear();
            // Aggiungo i device già peirati
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
     * Receiver usato per aggiornare l'elenco dei dispositivi bluetooth disponibili
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
    
    
    
    /** Permette di connettersi asincronamente al Server */
    private static class ConnectAsync
            extends AsyncTask<BluetoothDevice, Void, MacroClient> {
        private IOException exception;
        
        /** Finestrta che mostra il progresso dell'attività */
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
            // Se non ci sono stati errori e il client è connesso
            if(exception == null && client.isConnected()) {
                Connections.setConnection(client);
                // Indico l'ultimo server a cui si è connessi
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
