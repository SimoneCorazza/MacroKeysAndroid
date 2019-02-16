package com.macrokeysclient;


import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.macrokeysclient.fragments.servers.TabBluetoohtServFragment;
import com.macrokeysclient.fragments.servers.TabNetServFragment;
import com.macrokeysclient.fragments.servers.TabServersFragment;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/** Activity per la selezione della connessione */
public class ConnectionSelectionActivity extends AppCompatActivity {

    /** Tempo di attesa prima di interrompere la scoperta del Serverin millisecondi;
     * versione più lunga */
    private static final int TIMEOUT_LONG_SSID = 3000;

    /** Per la raccolt delle pagine dei vari  */
    private ViewPager pager;
    
    /** Tab associate al {@link ViewPager} {@link #pager} */
    private TabLayout tabs;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_selection);
        
        pager = (ViewPager) findViewById(R.id.viewPager);
        //Faccio sì che nessuna pagina venga cancellata
        pager.setOffscreenPageLimit(ServiceType.values().length);
        pager.setAdapter(new DevicePagerAdapter(getSupportFragmentManager()));
        tabs = (TabLayout) findViewById(R.id.tabs);
        tabs.setupWithViewPager(pager);
        
        //Rimuovo l'ombra sotto la barra dell'applicazione
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSupportActionBar().setElevation(0);
        }
    }

    
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.connection_selection, menu);
        return true;
    }

    
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_update_list:
                updateServers(TIMEOUT_LONG_SSID);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    /**
     * Aggiorna l'elenco dei server per la tab correntemente selezionata
     * @param maxWaitTime Tempo massimo da aspettare per la risposta dei server
     */
    private void updateServers(int maxWaitTime) {
        DevicePagerAdapter ad = (DevicePagerAdapter) pager.getAdapter();
        int tabSel = pager.getCurrentItem();
        TabServersFragment tab = ad.getFragmentAt(tabSel);
        tab.findServers(maxWaitTime);
    }
    
    
    
    /**
     * {@link android.support.v4.view.PagerAdapter} per la creazione delle tabs
     */
    private static class DevicePagerAdapter extends FragmentPagerAdapter {
        /** Memorizza le varie pagine generate, le chiavi sono le rispettive posizioni */
        private final Map<Integer, TabServersFragment> pagesFragmens = new ArrayMap<>();

        private DevicePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            ServiceType type = ServiceType.values()[position];
    
            TabServersFragment fragment;
            switch (type) {
                case Wifi:
                    fragment = new TabNetServFragment();
                    break;
                case Bluetooth:
                    fragment = new TabBluetoohtServFragment();
                break;
                    
                default:
                    assert false;
                    fragment = null;
            }
            pagesFragmens.put(position, fragment);
            
            return fragment;
        }

        
        
        @Override
        public int getCount() {
            return ServiceType.values().length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            try {
                ServiceType c = ServiceType.values()[position];
                return c.name();
            } catch (IndexOutOfBoundsException e) {
                throw new AssertionError("Connection type not found");
            }

        }
    
        /**
         * Ottiene il fragment alla posizione indicata di questo PageAdapter
         * @param position Posizione del fragment da ottenere
         * @return Fragment alla posizione indicata
         */
        public @NotNull TabServersFragment getFragmentAt(int position) {
            TabServersFragment frag = pagesFragmens.get(position);
            if(frag == null) {
                throw new IndexOutOfBoundsException();
            }
            return frag;
        }
    }
}
