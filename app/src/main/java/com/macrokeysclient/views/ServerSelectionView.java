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
 * View to show and select servers
 */
public class ServerSelectionView extends LinearLayout {

    /** View for the list of items */
    private ListView itemListView;
    
    /** Label to indicate the list is empty */
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
     * @param itemClickListener Event generated at the click of on item
     */
    public void setServerClickListener(AdapterView.OnItemClickListener itemClickListener) {
        itemListView.setOnItemClickListener(itemClickListener);
    }

    /**
     * @return Event generated at the click of on item
     */
    public AdapterView.OnItemClickListener getOnServerClickListener() {
        return itemListView.getOnItemClickListener();
    }
    
    
    /**
     * Adds a header not selectable by the user
     * @param v View to add
     */
    public void addHeaderView(@NonNull View v) {
        itemListView.addHeaderView(v, null, false);
    }
    
    
    /**
     * If no server was found
     */
    public void setResultNoResults() {
        txtNoItems.setVisibility(View.VISIBLE);
        itemListView.setVisibility(View.INVISIBLE);
        disabledConnection.setVisibility(View.INVISIBLE);
        progrssBar.setVisibility(View.INVISIBLE);
    }
    
    /**
     * Show the resulting servers
     * If the adapter is empty this calls equal to a {@link #setResultNoResults()} call
     * @param adapter Adapter containing the servers
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
     * Show an error in the configuration of the device
     * <p>
     *     Is shown:
     *     <li>A button that can be hidden</li>
     *     <li>An icon</li>
     *     <li>A TextView with a text message</li>
     * </p>
     * @param resIcon Id of the icon to show
     * @param errorText Text of the error
     * @param buttonCaption Text of the button; the button is hidden if is null or empty
     * @param l Callback associated at the pressure of the button; if null the button is not shown
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
     * Show the progress bar
     */
    public void showProgressBar() {
        itemListView.setVisibility(View.INVISIBLE);
        txtNoItems.setVisibility(View.INVISIBLE);
        disabledConnection.setVisibility(View.INVISIBLE);
        progrssBar.setVisibility(View.VISIBLE);
        progrssBar.setIndeterminate(true);
    }
}
