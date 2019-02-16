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
 * {@link Fragment} che rappresenta una tab generica che mostra l'elenco dei
 * server attualmente disponibili per un determinata tipologia di connessione
 */
public abstract class TabServersFragment extends Fragment {
    
    /** Codice del risultato della {@link MacroActivity} */
    private static final int RESULT_CODE_MACRO_VIEW = 1;
    
    public TabServersFragment() {
    
    }
    
    
    
    
    
    /**
     * Consente di effettuare la ricerca dei server
     * @param maxWaitTime Massimo tempo da aspettare
     */
    public abstract void findServers(int maxWaitTime);
    
    
    /**
     * Si riconnette al precedente server al quale si è stati connessi.
     */
    protected abstract void reconnect();
    
    /**
     * @return Servizio coperto dalla tab; non null
     */
    public abstract @NonNull ServiceType getServiceType();
    
    
    /**
     * Fa partire l'activity {@link MacroActivity}.
     * <p>
     *     Necessario settare la connessione su
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
