package com.macrokeysclient.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.macrokeys.netcode.MacroNetClient;
import com.macrokeysclient.R;

import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Adapter per gli items della liasta dei server
 */
public class NetServersAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final MacroNetClient.SSDPServerInfo[] serverInfos;

    public NetServersAdapter(@NotNull Context context,
                             @NotNull MacroNetClient.SSDPServerInfo[] serverInfos) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.serverInfos = serverInfos;
    }

    @Override
    public int getCount() {
        return serverInfos.length;
    }

    @Override
    public Object getItem(int position) {
        return serverInfos[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = inflater.inflate(R.layout.view_item_net_server, null);
        }


        TextView serverName = (TextView) convertView.findViewById(R.id.txtServerName);
        serverName.setText(serverInfos[position].name);
        TextView ipAddress = (TextView) convertView.findViewById(R.id.txtIPAddress);
        TextView port = (TextView) convertView.findViewById(R.id.txtPort);
        SocketAddress sa = serverInfos[position].address;
        if(sa instanceof InetSocketAddress) {
            InetSocketAddress isa = (InetSocketAddress) sa;
            ipAddress.setText(isa.getAddress().getHostAddress());
            port.setText(Integer.toString(isa.getPort()));
        }

        return convertView;
    }
}
