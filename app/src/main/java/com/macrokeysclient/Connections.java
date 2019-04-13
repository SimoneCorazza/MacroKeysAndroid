package com.macrokeysclient;

import com.macrokeys.comunication.MacroClient;

/**
 * Static class to store the active connections
 */
public final class Connections {

    private static MacroClient connection;

    private Connections() { }


    /**
     * @return Active connection of this device; null if none
     */
    public static synchronized MacroClient getConnection() {
        return connection;
    }

    /**
     * Set the active connection of this device
     * @param c New active connection; null if none
     */
    public static synchronized void setConnection(MacroClient c) {
        connection = c;
    }
}
