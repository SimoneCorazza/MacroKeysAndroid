package com.macrokeysclient.views;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;

import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.macrokeys.MacroKey;
import com.macrokeys.MacroScreen;
import com.macrokeys.MacroSetup;
import com.macrokeys.comunication.MacroClient;
import com.macrokeys.rendering.PaintStyle;
import com.macrokeys.rendering.Renderer;
import com.macrokeys.rendering.TextAllign;
import com.macrokeys.screen.Screen;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * View per renderizzare le macro e gestirne l'input
 */
public class MacroView extends View {

    /** Tempo in millisecondi necessari per considerare il touch come pressura del tasto */
    private static final long TIME_KEY_STROKE = 40;

    /** Macro setup da renderizzare al momento */
    private MacroSetup setup;

    /** Client dal quale inviare la pressione dei tasti */
    private MacroClient macroClient;

    /** Area disegnabile della view */
    private com.macrokeys.rendering.RectF drawArea;

    /** Informazioni sullo schermo per un dispositivo android */
    private AndroidScreen androidScreen;

    private Printer renderer;



    public MacroView(Context context) {
        super(context);
        init();
    }

    public MacroView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MacroView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        androidScreen = new AndroidScreen(((Activity) getContext()));
        renderer = new Printer();
    }




    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(setup != null) {
            renderer.reset(canvas);
            List<MacroKey> list = new ArrayList<>();
            list.add(keyPress);
            setup.getActualScreen().render(renderer, androidScreen, drawArea, list);
        }
    }




    /** Minima distanza che deve essere percorsa (in ...?) per riconoscere
     * l'azione del touch come un gesto (es. swipe) */
    private final static float MINIMUM_DISTANCE_GESTURE = 150;

    /** Posizione del primo tocco (X) della gesture attuale in pixel */
    private float firstX;
    /** Posizione del primo tocco (Y) della gesture attuale in pixel */
    private float firstY;
    /** Istante di tempo in ms del primo tocco  */
    private long tKeyStroke = 0;
    /** Indica se è stato eseguito almeno un keystroke durante la gesture corrente; se viene settato
     * a false viene interrotta la sequenza di keystroke impedene altri */
    private boolean keyStrokeMode = false;
    /** Tasto premuto in questa gesture */
    private MacroKey keyPress = null;
    /** Se true indica di ignorare l'input durante il movimento delle dita per eseguire una gesture,
     *  siccome la gesture è stata già riconoscuta e lo swipe già eseguito */
    private boolean moveScreenDone = false;
    /** Thread utilizzato per la pressione dei tasti */
    private Thread thKey;

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        if(setup == null) { //Se il setup non è impostato ignoro tutti gli eventi
            return false;
        }

        //Se più dita toccano lo schermo la pressione i keystroke si interrompono
        if(e.getPointerCount() > 1) {
            tKeyStroke = 0;
            keyStrokeMode = false;
        }


        switch (e.getAction())
        {
            case MotionEvent.ACTION_DOWN: //Il primo dito che tocca lo schermo
                Log.d("TOUCH", "ACTION_DOWN");
                firstX = e.getX();
                firstY = e.getY();
                keyPress = setup.getActualScreen().keyAt(firstX, firstY, androidScreen);
                if (keyPress != null) { //Se è stato premuto un tasto
                    tKeyStroke = System.currentTimeMillis();
                    thKey = new KeyStrokeThread();
                    thKey.setPriority(Thread.MAX_PRIORITY);
                    thKey.start(); //Avvio il thread per la pressione dei tasti
                    invalidate();
                }

                break;
            case MotionEvent.ACTION_MOVE:
                float dx = firstX - e.getX();
                float dy = firstY - e.getY();
                float ax = Math.abs(dx);
                float ay = Math.abs(dy);
                int sx = (int) Math.signum(dx);
                int sy = (int) Math.signum(dy);
                float d = (float) Math.sqrt(dx * dx + dy * dy);

                if(keyStrokeMode) { //Controllo se dopo il movimento il tasto è ancora premuto
                    keyStrokeMode = keyPress == setup.getActualScreen().keyAt(e.getX(), e.getY(), androidScreen);
                    if(!keyStrokeMode) {
                        //Se il tasto non è ancora premuto allora lo rilascio
                        releaseSelectedKey();
                        keyPress = null;
                        invalidate();
                    }
                }

                if(!moveScreenDone && !keyStrokeMode) {
                    if (d >= MINIMUM_DISTANCE_GESTURE) {
                        boolean left = ax >= ay && sx == 1;
                        boolean right = ax >= ay && sx == -1;
                        boolean up = ax < ay && sy == 1;
                        boolean down = ax < ay && sy == -1;

                        if(e.getPointerCount() == 2) { //Due dita
                            if(left) {
                                changeMacroScreen(MacroScreen.SwipeType.Finger2_Left);
                            } else if(right) {
                                changeMacroScreen(MacroScreen.SwipeType.Finger2_Right);
                            } else if(up) {
                                changeMacroScreen(MacroScreen.SwipeType.Finger2_Up);
                            } else if(down) {
                                changeMacroScreen(MacroScreen.SwipeType.Finger2_Down);
                            }
                        } else if(e.getPointerCount() == 3) { //tre dita
                            if(left) {
                                changeMacroScreen(MacroScreen.SwipeType.Finger3_Left);
                            } else if(right) {
                                changeMacroScreen(MacroScreen.SwipeType.Finger3_Right);
                            } else if(up) {
                                changeMacroScreen(MacroScreen.SwipeType.Finger3_Up);
                            } else if(down) {
                                changeMacroScreen(MacroScreen.SwipeType.Finger3_Down);
                            }
                        }

                        moveScreenDone = true;
                    }

                }

                break;
            //Due casi raggruppati:
            case MotionEvent.ACTION_CANCEL: //Generato se il metodo "changeMacroScreen" effettua una rotazione (ACTION_UP non generata)
            case MotionEvent.ACTION_UP:
                Log.d("TOUCH", "ACTION_UP or ACTION_CANCEL");
                moveScreenDone = false;
                tKeyStroke = 0;
                keyStrokeMode = false;
                //Dito sollevato => rilascio l'eventuale tasto
                releaseSelectedKey();
                break;
        }

        return true;
    }

    /**
     * Permette di rilasciare il l'eventuale tasto premuto
     * <p>DEVE ESSERE ESEGUITO SUL MAIN THREAD</p>
     */
    private void releaseSelectedKey() {
        if (Looper.getMainLooper() != Looper.myLooper())
            throw new AssertionError();

        if(keyPress != null) {
            try {
                if(macroClient != null) {
                    macroClient.keyUp(keyPress);
                }
            } catch (IOException ignored) {

            }
            invalidate();
            keyPress = null;

        }
    }

    /**
     * Cambia la schermata della view
     * @param s Nuova schermata della view
     */
    private void changeMacroScreen(MacroScreen.SwipeType s) {
        if(setup.changeScreen(s)) {
            releaseSelectedKey(); //Rilascio il tasto per evitare che la pressione continui
            updateOrientation();
            invalidate();
        }
    }

    /** Aggiorna l'orientamento dell'activity parent in base alla {@link MacroScreen} attuale*/
    private void updateOrientation() {
        int orientation;
        switch(setup.getActualScreen().getOrientation()) {
            case Vertical: orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
                break;
            case Horizontal: orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                break;
            case Rotate: orientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
                break;

            default: throw new AssertionError("Case missing");
        }
        ((Activity) getContext()).setRequestedOrientation(orientation);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        drawArea = new com.macrokeys.rendering.RectF(
                getPaddingLeft(),
                getPaddingTop(),
                getPaddingLeft() + w - getPaddingRight(),
                getPaddingTop() + h - getPaddingBottom());
    }

    /**
     * Imposta la macro da utilizzare
     * @param macroSetup - Macro setup da usare
     */
    public void setMacroSetup(@NotNull MacroSetup macroSetup) {
        setup = macroSetup;
        updateOrientation();
        invalidate();
    }

    /**
     * Imposta il {@link MacroClient} tramite la quale inviare le pressioni dei tasti
     * @param macroClient Istanza del client
     */
    public void setMacroClient(MacroClient macroClient) {
        this.macroClient = macroClient;
    }

    /**
     * @return {@link MacroClient} tramite la quale inviare le pressioni dei tasti
     */
    public MacroClient getMacroClient() {
        return macroClient;
    }


    /**
     * Classe per il thread che gestisce il timer per la pressione dei tasti
     */
    private class KeyStrokeThread extends Thread {
        @Override
        public void run() {
            //Timer per contare la pressione come keystroke
            while (tKeyStroke > 0) {
                if (System.currentTimeMillis() - tKeyStroke >= TIME_KEY_STROKE) {
                    if (tKeyStroke != 0) { //Caso entrassi nell'IF con tKeyStroke = 0
                        tKeyStroke = 0;
                        keyStrokeMode = true;
                    }
                }
            }


            try { //Entro nello stato keydown
                //Siccome "keyPress" e "macroClient" possono essere settati a null in qualsiasi momento
                //ne eseguo la copia ed eseguo i test e le operazioni sulla copia
                MacroKey k = keyPress;
                MacroClient c = macroClient;
                if(k != null && c != null) {
                    c.keyDown(k);
                }
            } catch (IOException e) {

            }
        }
    }




    /** Per il rendering della {@link MacroScreen} */
    private static class Printer implements com.macrokeys.rendering.Renderer {

        private Canvas canvas;
        private Paint paint;

        public Printer() {
        }

        /**
         * Resetta lo stato di this e lo predispone a una nuovo passaggio di rendering
         * @param canvas
         */
        public void reset(@NotNull Canvas canvas) {
            this.canvas = canvas;
            paint = new Paint();
        }

        @Override
        public void setColor(int i) {
            paint.setColor(i);
        }

        @Override
        public void setAntiAlias(boolean b) {
            paint.setAntiAlias(b);
        }

        @Override
        public void setPaintStyle(PaintStyle paintStyle) {
            switch (paintStyle) {
                case Fill: paint.setStyle(Paint.Style.FILL);
                    break;
                case Stroke: paint.setStyle(Paint.Style.STROKE);
                    break;
                case Fill_and_stroke: paint.setStyle(Paint.Style.FILL_AND_STROKE);
                    break;
            }
        }

        @Override
        public void setTextAllign(TextAllign textAllign) {
            switch (textAllign) {
                case Left: paint.setTextAlign(Paint.Align.LEFT);
                    break;
                case Center: paint.setTextAlign(Paint.Align.CENTER);
                    break;
                case Right: paint.setTextAlign(Paint.Align.RIGHT);
                    break;
            }
        }

        @Override
        public void setTextSize(float v) {
            paint.setTextSize(v);
        }

        @Override
        public void ellipse(com.macrokeys.rendering.RectF r) {
            canvas.drawOval(new RectF(r.left, r.top, r.right, r.bottom), paint);
        }

        @Override
        public void rect(com.macrokeys.rendering.RectF r) {
            canvas.drawRect(r.left, r.top, r.right, r.bottom, paint);
        }

        @Override
        public void text(String s, com.macrokeys.rendering.RectF r) {
            drawStringRect(canvas, s, new RectF(r.left, r.top, r.right, r.bottom), paint);
        }


        /**
         * Permette di disegnare la stringa nel rettangolo.
         * <p>
         * Non vengono effettuati controlli sulla dimensione del testo occupato: viene solamente
         * renderizzato il testo
         * </p>
         * @param c - Canvas per il rendering
         * @param s - Stringa da renderizzare
         * @param r - Rettangolo nel quale centrare la scritta
         * @param p - Paint
         */
        private static void drawStringRect(@NotNull Canvas c, @NotNull String s, @NotNull RectF r,
                                    @NotNull Paint p) {
            switch (p.getTextAlign())
            {
                case CENTER: c.drawText(s, 0, s.length(), r.centerX(), r.centerY() + p.getTextSize() / 4.f, p); break;
                case LEFT: c.drawText(s, 0, s.length(), r.left, r.top, p); break;
                case RIGHT: c.drawText(s, 0, s.length(), r.right, r.top, p); break;
            }
        }
    }


    /** Implementazione dello schermo per dispositivi Android */
    private static class AndroidScreen extends Screen {

        private final DisplayMetrics metrics;

        /**
         * @param a Attività dalla quale estrarre le proprietà dello schermo
         */
        AndroidScreen(@NotNull Activity a) {
            metrics = new DisplayMetrics();
            a.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        }

        @Override
        public float getXDpi() {
            return metrics.xdpi;
        }

        @Override
        public float getYDpi() {
            return metrics.ydpi;
        }
    }
}
