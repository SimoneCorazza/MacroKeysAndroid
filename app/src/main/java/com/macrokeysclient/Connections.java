package com.macrokeysclient;

import com.macrokeys.comunication.MacroClient;

/**
 * Classe statica che memorizza le connessioni del device
 */
public final class Connections {

    private static MacroClient connection;

    private Connections() { }


    /**
     * @return Connessione attiva attualmente sul PC
     */
    public static synchronized MacroClient getConnection() {
        return connection;
    }

    /**
     * Setta la connessione col PC
     * @param c - Connessione può essere null (connessione non stabilita)
     */
    public static synchronized void setConnection(MacroClient c) {
        connection = c;
    }
}
