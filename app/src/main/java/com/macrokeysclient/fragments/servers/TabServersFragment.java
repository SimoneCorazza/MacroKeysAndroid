package com.macrokeysclient.fragments.servers;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.View;

import com.macrokeysclient.MacroActivity;
import com.macrokeysclient.ServiceType;



/**
 * {@link Fragment} for a generic tab that shows the list of available servers for a connection type
 */
public abstract class TabServersFragment extends Fragment {
    
    /** Result code of the {@link MacroActivity} */
    private static final int RESULT_CODE_MACRO_VIEW = 1;
    
    public TabServersFragment() {
    
    }
    
    
    
    
    
    /**
     * Find servers
     * @param maxWaitTime Maximum wait time for the server to respond
     */
    public abstract void findServers(int maxWaitTime);
    
    
    /**
     * Reconnect to the previous server if present
     */
    protected abstract void reconnect();
    
    /**
     * @return Implemented service by this tab
     */
    public abstract @NonNull ServiceType getServiceType();
    
    
    /**
     * Starts the activity {@link MacroActivity}.
     * <p>
     *     Needed to set the connection in a
     *     {@link com.macrokeysclient.Connections}
     * </p>
     */
    protected final void startMacroActivity() {
        Intent intent = new Intent(getActivity(), MacroActivity.class);
        startActivityForResult(intent, RESULT_CODE_MACRO_VIEW);
    }
    
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == RESULT_CODE_MACRO_VIEW) {
            if(resultCode == Activity.RESULT_OK) {
                if(getView() == null) {
                    throw new IllegalStateException(
                            "Invalid TabServerFragment: no root view detected");
                }
                
                Snackbar retry = Snackbar.make(getView(),
                        "Lost connection with server",
                        Snackbar.LENGTH_LONG);
                retry.setAction("Reconnect", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        reconnect();
                    }
                });
                retry.show();
            }
        }
    }
}
