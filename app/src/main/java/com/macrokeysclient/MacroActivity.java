package com.macrokeysclient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.PowerManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.macrokeys.MacroKey;
import com.macrokeys.comunication.MacroClient;
import com.macrokeysclient.AndroidMyUtil.AndroidMyUtil;
import com.macrokeys.MacroSetup;
import com.macrokeys.MSLoadException;
import com.macrokeysclient.views.MacroView;
import com.macrokeysclient.views.OnMacroKeyStroke;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Attività per le macro
 */
public class MacroActivity extends AppCompatActivity {

    /** MacroSetup attualmente in uso, inviata dal Server; null se il caricamento va male */
    private MacroSetup originalSetup;

    /** MacroSetup in uso adattata alle dimensioni del devce */
    private MacroSetup correctSizedSetup;

    /** Utilizzata mentre si aspetta la MacroSetup dal server */
    private ProgressDialog progressDialog;

    /** Thread che ascolta l'arrivo delle macro setup inviate dal server */
    private Thread thMacroSetupListener;

    /** View usata per renderizzare le varie schermate */
    private MacroView macroView;
    
    /**
     * Permette di non mandere in standby l'applicazione, evitndo così che i
     * thrad necessari per mantenere attiva la connessione con il server
     * vengano fermati
     */
    private PowerManager.WakeLock wakeLock;
    
    /** Metriche dello schermo */
    private Display display;
    private DisplayMetrics metrics;


    /** Argomento passato all'{@link Intent} che rappresenta il risultato
     * di questa Activity */
    public static final String ARG_RESULT = "ARG_RES";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_macro);

        //Controllo il settaggio della connessione
        if(Connections.getConnection() == null) {
            throw new RuntimeException("Connection not set use: Connections.setConnection()");
        }

        macroView = (MacroView) findViewById(R.id.macroView);
        assert macroView != null;
        macroView.setMacroClient(Connections.getConnection());

        //Fullscreen:
        if(Build.VERSION.SDK_INT >= 16) {
            hideSystemUI();
        }
    
        //Per tenere acceso lo schermo sempre
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    
        //Per impedire di mandare in standby l'app e i thread che comunicano con il server
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        assert powerManager != null;
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();
        
        
        //Metriche dello schermo
        display = getWindowManager().getDefaultDisplay();
        metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        progressDialog = ProgressDialog.show(MacroActivity.this,
                "Waiting for Macro Setup",
                "Waiting...");
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // Connessione interrotta dall'utente
                setResult(true);
                finish();
            }
        });

        thMacroSetupListener = new Thread(new MacroSetupListener());
        thMacroSetupListener.setUncaughtExceptionHandler(new UncaughException());
        thMacroSetupListener.start();
    }

    /**
     * Nesconde l'UI di sistema
     */
    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT < 19) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            macroView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hideSystemUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    
        
        
        MacroClient c = Connections.getConnection();
        try {
            c.close();
        } catch (IOException ignored) { }
        //Resetto la connessione
        Connections.setConnection(null);
    
        //Rilascio il lock
        wakeLock.release();
    }

    
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //Aggiorno la setup
        updateSetup();
    }
    
    /**
     * Imposta il risultato inerente a questa activity
     * @param res True: disconnessione causata dall'utente; False altrimenti
     */
    private void setResult(boolean res) {
        // Connessione interrotta dall'utente
        Intent returnIntent = new Intent();
        returnIntent.putExtra(ARG_RESULT, res);
        setResult(Activity.RESULT_OK, returnIntent);
    }
    
    /**
     * Permette di ricalcolare la dimensione corretta della {@link MacroSetup} per il device
     * corrente, aggiornado così il campo {@code correctSizedSetup}; in oltre aggiorna la view
     * dedicata a mostrare la {@link MacroSetup}, Quest'utima operazione è thread-safe.
     * <p>{@link #originalSetup} non deve essere {@code null}</p>
     */
    private void updateSetup() {
        assert originalSetup != null;

        Point p = new Point();
        display.getRealSize(p);
        float width = AndroidMyUtil.pxtomm_X(p.x, metrics);
        float height = AndroidMyUtil.pxtomm_X(p.y, metrics);
        correctSizedSetup = originalSetup.fitFor(width, height);

        MacroActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                macroView.setMacroSetup(correctSizedSetup);
                if(progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        });
    }

    /**
     * Eseguibile per la ricezione delle MacroSetup
     */
    private class MacroSetupListener implements Runnable {
        @Override
        public void run() {
            while (!MacroActivity.this.isFinishing()) {
                try {
                    originalSetup = Connections.getConnection().reciveMacroSetup();
                } catch (Throwable e) {
                    //Forzo la generazione dell'eccezione per generare un'eccezzione non gestita
                    throw new RuntimeException(e);
                }

                updateSetup();
            }
        }
    }

    /**
     * Handler per le eccezzioni generate dal thread che ascolta pa ricezione di {@link MacroSetup}
     */
    private class UncaughException implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(java.lang.Thread thread, Throwable ex) {

            //Caso l'eccezione sia generata a causa della chiusura dell'activity
            if(MacroActivity.this.isFinishing()) {
                // Connessione interrotta dall'utente
                setResult(true);
                return;
            }
    
            Log.i("DISCONNECTED", "Disconnected from server", ex);
            MacroActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Connessione interrotta dal server
                    setResult(false);
                    finish();
                }
            });
        }
    }



}
