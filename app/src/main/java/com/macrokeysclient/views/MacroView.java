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
import com.macrokeys.rendering.TextAllign;
import com.macrokeys.screen.Screen;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * View to render the macro and handle the input
 */
public class MacroView extends View {

    /** Time in milliseconds to consider the touch as a key press */
    private static final long TIME_KEY_STROKE = 40;

    /** MacroSetup to render */
    private MacroSetup setup;

    /** Client witch send the keypress */
    private MacroClient macroClient;

    /** Are where the drowing take place */
    private com.macrokeys.rendering.RectF drawArea;

    /** Android info for the screen of this device */
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




    /** Minimum distance to recognize a swipe */
    private final static float MINIMUM_DISTANCE_GESTURE = 150;

    /** Location of the first touch (X) of the swipe in pixels */
    private float firstX;
    
    /** Location of the first touch (Y) of the swipe in pixels */
    private float firstY;
    
    /** Timestamp in ms of the first touch  */
    private long tKeyStroke = 0;
    
    /**
     * Indicates if was executed at least one keystroke during the current swipe;
     * if is set to false the keystroke sequence is interrupted
     */
    private boolean keyStrokeMode = false;
    
    /** Key pressed in this gesture */
    private MacroKey keyPress = null;
    
    /**
     * If true indicates to ignore the input during the movment of the finger to execute a swipe,
     *  because a swipe was already recognized and the swipe executed
     */
    private boolean moveScreenDone = false;
    
    /** Thread uused for the keystroke */
    private Thread thKey;

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        if(setup == null) {
            return false;
        }

        // If more fingers touch the screen the keystroke is interrupted
        if(e.getPointerCount() > 1) {
            tKeyStroke = 0;
            keyStrokeMode = false;
        }


        switch (e.getAction())
        {
            case MotionEvent.ACTION_DOWN: // First finger touch the screen
                Log.d("TOUCH", "ACTION_DOWN");
                firstX = e.getX();
                firstY = e.getY();
                keyPress = setup.getActualScreen().keyAt(firstX, firstY, androidScreen);
                if (keyPress != null) { // If a key was pressed
                    tKeyStroke = System.currentTimeMillis();
                    thKey = new KeyStrokeThread();
                    thKey.setPriority(Thread.MAX_PRIORITY);
                    thKey.start(); // Start the thread for the pressure of the keys
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
    
                // Ceck if after the movment the key is still pressed
                if(keyStrokeMode) {
                    keyStrokeMode = keyPress == setup.getActualScreen().keyAt(e.getX(), e.getY(), androidScreen);
                    if(!keyStrokeMode) {
                        // If the key is not released i release it
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

                        if(e.getPointerCount() == 2) { // 2 finger
                            if(left) {
                                changeMacroScreen(MacroScreen.SwipeType.Finger2_Left);
                            } else if(right) {
                                changeMacroScreen(MacroScreen.SwipeType.Finger2_Right);
                            } else if(up) {
                                changeMacroScreen(MacroScreen.SwipeType.Finger2_Up);
                            } else if(down) {
                                changeMacroScreen(MacroScreen.SwipeType.Finger2_Down);
                            }
                        } else if(e.getPointerCount() == 3) { // 3 finger
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
            // Two case grouped:
            case MotionEvent.ACTION_CANCEL: // Generated if the method "changeMacroScreen" executes a rotation (ACTION_UP non generata)
            case MotionEvent.ACTION_UP:
                Log.d("TOUCH", "ACTION_UP or ACTION_CANCEL");
                moveScreenDone = false;
                tKeyStroke = 0;
                keyStrokeMode = false;
                
                // Finger up => release the key
                releaseSelectedKey();
                break;
        }

        return true;
    }

    /**
     * Release the pressed key
     * <p>MUST BE EXECUTED ON THE MAIN THREAD</p>
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
     * Change the MacroScreen used
     * @param s MacroScreen to select
     */
    private void changeMacroScreen(MacroScreen.SwipeType s) {
        if(setup.changeScreen(s)) {
            releaseSelectedKey(); //Rilascio il tasto per evitare che la pressione continui
            updateOrientation();
            invalidate();
        }
    }

    /** Update the orientation of the activity parent thanks to the actual {@link MacroScreen} */
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
     * Sets the MacroSetup to use
     * @param macroSetup MacroSetup to use
     */
    public void setMacroSetup(@NotNull MacroSetup macroSetup) {
        setup = macroSetup;
        updateOrientation();
        invalidate();
    }

    /**
     * Sets the {@link MacroClient} thatg sends the keystroke to the server
     * @param macroClient Instance of the client
     */
    public void setMacroClient(MacroClient macroClient) {
        this.macroClient = macroClient;
    }

    /**
     * @return {@link MacroClient} that sends the keystroke to the server
     */
    public MacroClient getMacroClient() {
        return macroClient;
    }


    /**
     * Thread that handles the time for the key pressure
     */
    private class KeyStrokeThread extends Thread {
        @Override
        public void run() {
            // Timer to count the pression as keystroke
            while (tKeyStroke > 0) {
                if (System.currentTimeMillis() - tKeyStroke >= TIME_KEY_STROKE) {
                    if (tKeyStroke != 0) { // Case enters in the if with tKeyStroke = 0
                        tKeyStroke = 0;
                        keyStrokeMode = true;
                    }
                }
            }


            try { // Enter in the state keydown
                // Because "keyPress" and "macroClient" can be setted to null at any time
                // i copy them
                MacroKey k = keyPress;
                MacroClient c = macroClient;
                if(k != null && c != null) {
                    c.keyDown(k);
                }
            } catch (IOException e) {

            }
        }
    }




    /** For the rendering of the {@link MacroScreen} */
    private static class Printer implements com.macrokeys.rendering.Renderer {

        private Canvas canvas;
        private Paint paint;

        public Printer() {
        }

        /**
         * Reset the state of this and arrange it for a new draw cicle
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
         * Draw a string in a rectangle
         * <p>
         * Does not check the size of the rendered text
         * </p>
         * @param c Canvas for the rendering
         * @param s Text to render
         * @param r Rectangle where to render the tet
         * @param p Paint
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


    /** Android implementation for a {@link Screen} */
    private static class AndroidScreen extends Screen {

        private final DisplayMetrics metrics;

        /**
         * @param a Activity from witch get the screen property
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
