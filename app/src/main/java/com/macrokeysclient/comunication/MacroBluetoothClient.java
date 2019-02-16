package com.macrokeysclient.comunication;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.NonNull;

import com.macrokeys.comunication.MacroClient;
import com.macrokeys.comunication.MessageProtocol;

import java.io.IOException;
import java.util.UUID;


/**
 * Implementazione di {@link MacroClient} per la comunicazione tramite
 * Bluetooth
 */
public final class MacroBluetoothClient extends MacroClient {
    
    /** UUID usato per identificare l'applicazione lato server */
    private static final UUID SERVICE_UUID = UUID.fromString("a69cea44-c6dd-11e7-abc4-cec278b6b50a");
    
    /** Server al quale ci si sta connettendo */
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
