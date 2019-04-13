package com.macrokeysclient;


import android.os.Build;
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

/** Activity for the selection of the connection */
public class ConnectionSelectionActivity extends AppCompatActivity {

    /**
     * Long timeout for the server discovery in milliseconds
     */
    private static final int TIMEOUT_LONG_SSID = 3000;

    /** Set of pages for the various connection types */
    private ViewPager pager;
    
    /** Associated tabs at the {@link ViewPager} {@link #pager} */
    private TabLayout tabs;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_selection);
        
        pager = (ViewPager) findViewById(R.id.viewPager);
        
        // No page must be deleted
        pager.setOffscreenPageLimit(ServiceType.values().length);
        pager.setAdapter(new DevicePagerAdapter(getSupportFragmentManager()));
        tabs = (TabLayout) findViewById(R.id.tabs);
        tabs.setupWithViewPager(pager);
        
        // Remove the shadow under the app bar
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
     * Update the list of servers for the currently selected tab
     * @param maxWaitTime Timeout for the server discovery
     */
    private void updateServers(int maxWaitTime) {
        DevicePagerAdapter ad = (DevicePagerAdapter) pager.getAdapter();
        int tabSel = pager.getCurrentItem();
        TabServersFragment tab = ad.getFragmentAt(tabSel);
        tab.findServers(maxWaitTime);
    }
    
    
    
    /**
     * {@link android.support.v4.view.PagerAdapter} for the creation of tabs
     */
    private static class DevicePagerAdapter extends FragmentPagerAdapter {
        /** Store the various generated page, the keys are their indexes */
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
         * Gests the fragment at the position given in this PageAdapter
         * @param position Position of the fragment to get
         * @return Fragment at the given position
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
