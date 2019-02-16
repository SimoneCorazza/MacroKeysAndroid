package com.macrokeysclient.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.macrokeysclient.R;

/**
 * View per la visualizzazione e selezione di un server
 */
public class ServerSelectionView extends LinearLayout {

    /** View per la lista degli items */
    private ListView itemListView;
    
    /** Label per indicare che la lista è vuota */
    private TextView txtNoItems;
    
    private LinearLayout disabledConnection;
    private TextView disabledConnection_error;
    private ImageView disabledConnection_icon;
    private Button disabledConnection_btn;
    private ProgressBar progrssBar;

    public ServerSelectionView(Context context) {
        super(context);
        init();
    }

    public ServerSelectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public ServerSelectionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View v = inflater.inflate(R.layout.view_server_selection, null, false);
        this.addView(v);


        txtNoItems = (TextView) findViewById(R.id.txtNoItems);
        itemListView = (ListView) findViewById(R.id.itemListView);
        disabledConnection = (LinearLayout) findViewById(R.id.disabledConnection);
        disabledConnection_error = (TextView) findViewById(R.id.disabledConnection_error);
        disabledConnection_icon = (ImageView) findViewById(R.id.disabledConnection_icon);
        disabledConnection_btn = (Button) findViewById(R.id.disabledConnection_btn);
        progrssBar = (ProgressBar) findViewById(R.id.progrssBar);
        

        txtNoItems.setVisibility(View.INVISIBLE);
        disabledConnection.setVisibility(View.INVISIBLE);
        progrssBar.setVisibility(View.INVISIBLE);
    }


    /**
     * @param itemClickListener Evento generato al click di un item
     */
    public void setServerClickListener(AdapterView.OnItemClickListener itemClickListener) {
        itemListView.setOnItemClickListener(itemClickListener);
    }

    /**
     * @return Evento generato al click di un item rappresentante un server
     */
    public AdapterView.OnItemClickListener getOnServerClickListener() {
        return itemListView.getOnItemClickListener();
    }
    
    
    /**
     * Aggiunge una view non selezionabile all'elenco di servers
     * @param v View da aggiungere; non null
     */
    public void addHeaderView(@NonNull View v) {
        itemListView.addHeaderView(v, null, false);
    }
    
    
    /**
     * Se nessun server è stato trovato.
     */
    public void setResultNoResults() {
        txtNoItems.setVisibility(View.VISIBLE);
        itemListView.setVisibility(View.INVISIBLE);
        disabledConnection.setVisibility(View.INVISIBLE);
        progrssBar.setVisibility(View.INVISIBLE);
    }
    
    /**
     * Mostra i server risultanti.
     * Se l'adaprter è vuoto questa chiamata è equivalente a
     * {@link #setResultNoResults()}.
     * @param adapter Adapter contenente i server
     * @see #setResultNoResults()
     */
    public void setResults(@NonNull BaseAdapter adapter) {
        if(adapter.getCount() == 0) {
            setResultNoResults();
        } else {
            itemListView.setAdapter(adapter);
            itemListView.setVisibility(View.VISIBLE);
            txtNoItems.setVisibility(View.INVISIBLE);
            disabledConnection.setVisibility(View.INVISIBLE);
            progrssBar.setVisibility(View.INVISIBLE);
        }
    }
    
    
    /**
     * Imposta il risultato segnalando un errore nella configurazione nel
     * dispositivo.
     * <p>
     *     Vengono mostrati:
     *     <li>Un bottone, che può anche non essere mostrato</li>
     *     <li>Un icona</li>
     *     <li>Ua TextView con un messaggio d'errore</li>
     * </p>
     * @param resIcon Id della risorsa dell'icona dell'errore
     * @param errorText Stringa di errore
     * @param buttonCaption Testo del bottone; se null o stringa vuota il
     *                      bottone non viene mostrato
     * @param l Evento associato alla pressione del bottone; se null il
     *          bottone non viene mostrato
     */
    public void setResultNoConnection(int resIcon,
                                      String errorText,
                                      String buttonCaption,
                                      OnClickListener l) {
        disabledConnection_icon.setImageResource(resIcon);
        disabledConnection_error.setText(errorText);
        
        if(buttonCaption == null || buttonCaption.isEmpty() || l == null) {
            disabledConnection_btn.setVisibility(View.INVISIBLE);
        } else {
            disabledConnection_btn.setVisibility(View.VISIBLE);
            disabledConnection_btn.setText(buttonCaption);
            disabledConnection_btn.setOnClickListener(l);
        }
    
    
        itemListView.setVisibility(View.INVISIBLE);
        txtNoItems.setVisibility(View.INVISIBLE);
        disabledConnection.setVisibility(View.VISIBLE);
        progrssBar.setVisibility(View.INVISIBLE);
    }
    
    
    /**
     * Mostra la progress bar
     */
    public void showProgressBar() {
        itemListView.setVisibility(View.INVISIBLE);
        txtNoItems.setVisibility(View.INVISIBLE);
        disabledConnection.setVisibility(View.INVISIBLE);
        progrssBar.setVisibility(View.VISIBLE);
        progrssBar.setIndeterminate(true);
    }
}
