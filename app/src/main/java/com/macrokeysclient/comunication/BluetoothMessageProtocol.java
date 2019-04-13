package com.macrokeysclient.comunication;

import android.bluetooth.BluetoothSocket;
import android.support.annotation.NonNull;

import com.macrokeys.comunication.MessageProtocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * Implementastion of {@link MessageProtocol} for the Bluetooth comunication
 * <p>
 *     The timeout of the connection is not implementes because the underlaying
 *     layer handle the lost of connection
 * </p>
 */
public final class BluetoothMessageProtocol implements MessageProtocol {
    
    private final BluetoothSocket so;
    private final DataInputStream in;
    private final DataOutputStream out;
    
    
    public BluetoothMessageProtocol(@NonNull BluetoothSocket so)
            throws IOException {
        this.so = so;
        this.in = new DataInputStream(so.getInputStream());
        this.out = new DataOutputStream(so.getOutputStream());
    }
    
    
    @Override
    public boolean isConnected() {
        return so.isConnected();
    }
    
    @Override
    public void setInputKeepAlive(int time) {
        // Not needed
    }
    
    @Override
    public int getInputKeepAlive() {
        return 0; // Not needed
    }
    
    @Override
    public void setOutputKeepAlive(int time) {
        // Not needed
    }
    
    @Override
    public int getOutputKeepAlive() {
        return 0; // Not needed
    }
    
    @Override
    public void sendMessage(byte[] payload) throws IOException {
        out.writeByte(1);
        out.writeByte(0);
        out.writeInt(payload.length);
        out.write(payload);
    }
    
    @Override
    public byte[] receiveMessage() throws IOException {
        byte v1 = in.readByte();
        byte v2 = in.readByte();
        if(v1 != 1 || v2 != 0) {
            throw new IOException("Version of message not known");
        }
    
        int length = in.readInt();
        byte[] payload = new byte[length];
        in.readFully(payload);
    
        return payload;
    }
    
    @Override
    public void close() throws IOException {
        so.close();
    }
}
