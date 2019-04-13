package com.macrokeysclient.comunication;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.NonNull;

import com.macrokeys.comunication.MacroClient;
import com.macrokeys.comunication.MessageProtocol;

import java.io.IOException;
import java.util.UUID;


/**
 * Implementation of {@link MacroClient} for the comunication with Bluetooth
 */
public final class MacroBluetoothClient extends MacroClient {
    
    /** UUID used to identify the application in the server side */
    private static final UUID SERVICE_UUID = UUID.fromString("a69cea44-c6dd-11e7-abc4-cec278b6b50a");
    
    /** Server this is comunicating with */
    private final BluetoothDevice server;
    
    
    public MacroBluetoothClient(@NonNull BluetoothDevice server) {
        this.server = server;
    }
    
    
    
    @Override
    protected MessageProtocol innerConnectToServer() throws IOException {
        BluetoothSocket so = server.createRfcommSocketToServiceRecord(SERVICE_UUID);
        try {
            so.connect();
        } catch(IOException e) {
            e.printStackTrace();
            throw e;
        }
        return new BluetoothMessageProtocol(so);
    }
}
