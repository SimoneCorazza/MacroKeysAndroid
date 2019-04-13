package com.macrokeysclient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.macrokeys.comunication.MacroClient;
import com.macrokeysclient.AndroidMyUtil.AndroidMyUtil;
import com.macrokeys.MacroSetup;
import com.macrokeysclient.views.MacroView;

import java.io.IOException;

/**
 * Activity for the macro
 */
public class MacroActivity extends AppCompatActivity {

    /** MacroSetup actually used; null if the loading does not go well */
    private MacroSetup originalSetup;

    /** MacroSetup used in the size of this device */
    private MacroSetup correctSizedSetup;

    /** Dialog to show when the server has not sent the MacroSetup yet */
    private ProgressDialog progressDialog;

    /** Thread that listen the incomming MacroSetup sent by the server */
    private Thread thMacroSetupListener;

    /** View used to render the screens */
    private MacroView macroView;
    
    /**
     * Uset to not stanby the app, avoiding that threads to keep alive the connection
     * with the server are stopped
     */
    private PowerManager.WakeLock wakeLock;
    
    /** Screen metrics */
    private Display display;
    private DisplayMetrics metrics;


    /** Argument passed at the {@link Intent} that rapresent the result of this Activity */
    public static final String ARG_RESULT = "ARG_RES";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_macro);

        // Check the presence of the connection
        if(Connections.getConnection() == null) {
            throw new RuntimeException("Connection not set use: Connections.setConnection()");
        }

        macroView = (MacroView) findViewById(R.id.macroView);
        assert macroView != null;
        macroView.setMacroClient(Connections.getConnection());

        // Fullscreen
        if(Build.VERSION.SDK_INT >= 16) {
            hideSystemUI();
        }
    
        // To keep the screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    
        // To avoid the app standby
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        assert powerManager != null;
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();
        
        
        // Screen metric
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
                // Connection stopped by the user
                setResult(true);
                finish();
            }
        });

        thMacroSetupListener = new Thread(new MacroSetupListener());
        thMacroSetupListener.setUncaughtExceptionHandler(new UncaughException());
        thMacroSetupListener.start();
    }

    /**
     * Hide the system UI
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
        
        // Reset the connection
        Connections.setConnection(null);
    
        // Release the lock
        wakeLock.release();
    }

    
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // Update the setup
        updateSetup();
    }
    
    /**
     * Set the result of this Activity
     * @param res True: disconnection caused by the user, false otherwise
     */
    private void setResult(boolean res) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(ARG_RESULT, res);
        setResult(Activity.RESULT_OK, returnIntent);
    }
    
    /**
     * Recalculate the correct size of the {@link MacroSetup} for this device
     * updating the field {@link #correctSizedSetup}; Moreover updates the view
     * dedicated at showing the {@link MacroSetup}.
     * The last operation is thread-safe.
     * <p>{@link #originalSetup} must not be {@code null}</p>
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
     * Runnable for receiving the MacroSetup
     */
    private class MacroSetupListener implements Runnable {
        @Override
        public void run() {
            while (!MacroActivity.this.isFinishing()) {
                try {
                    originalSetup = Connections.getConnection().reciveMacroSetup();
                } catch (Throwable e) {
                    // Fore a generation of an unhandled exception
                    throw new RuntimeException(e);
                }

                updateSetup();
            }
        }
    }

    /**
     * Handler for the exception generated by the thread that receive the {@link MacroSetup}
     */
    private class UncaughException implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(java.lang.Thread thread, Throwable ex) {

            // If exception generated by the closing Activity
            if(MacroActivity.this.isFinishing()) {
                // Connection iterrupted by the user
                setResult(true);
                return;
            }
    
            Log.i("DISCONNECTED", "Disconnected from server", ex);
            MacroActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Connection iterrupted by the server
                    setResult(false);
                    finish();
                }
            });
        }
    }



}
