package com.macrokeysclient.views;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.macrokeysclient.R;

/**
 * {@link BaseAdapter} for the view of the Bluetooth
 */
public final class BluetoothServerAdapter extends BaseAdapter {
    
    private final LayoutInflater inflater;
    private final BluetoothDevice[] servers;
    
    public BluetoothServerAdapter(@NonNull Context context,
                                  @NonNull BluetoothDevice[] servers) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.servers = servers;
    }
    
    
    @Override
    public int getCount() {
        return servers.length;
    }
    
    @Override
    public Object getItem(int position) {
        return servers[position];
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BluetoothDevice actual = servers[position];
        if(convertView == null) {
            convertView = inflater.inflate(R.layout.view_item_bluetooth_server, null);
        }
    
    
        TextView txtName = (TextView) convertView.findViewById(R.id.txtServerName);
        TextView txtPaired = (TextView) convertView.findViewById(R.id.txtPaired);
        
        txtName.setText(actual.getName());
        if(actual.getBondState() == BluetoothDevice.BOND_BONDED) {
            txtPaired.setText("Paired");
        } else {
            txtPaired.setText("Not paired");
        }
        
        return convertView;
    }
}
