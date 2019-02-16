package com.macrokeysclient.AndroidMyUtil;

import android.content.Context;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

/**
 * Classe statica che ha delle utilità per il dispositivo Android
 */
public final class AndroidMyUtil {
    private AndroidMyUtil() {}

    /** 1 pollice in millimetri */
    public static final float INCH_MM = 25.4f;

    /**
     * Mostra un Toast lungo
     * @param C Contex sul quale mostrare
     * @param Message Messaggio da mostrare
     */
    public static void ToastLong(Context C, String Message) {
        if(C == null || Message == null) {
            throw new NullPointerException();
        }
        Toast.makeText(C, Message, Toast.LENGTH_LONG).show();
    }

    /**
     * Mostra un Toast corto
     * @param C Contex sul quale mostrare
     * @param Message Messaggio da mostrare
     */
    public static void ToastShort(Context C, String Message) {
        if(C == null || Message == null) {
            throw new NullPointerException();
        }
        Toast.makeText(C, Message, Toast.LENGTH_SHORT).show();
    }

    /**
     * @param mm Millimetri da convertire
     * @param m Metriche del display
     * @return Numero di pixel presenti nei millimetri indicati sull'asse X del display (<code>mm</code>)
     */
    public static float mmtopx_X(float mm, @NotNull DisplayMetrics m) {
        return (mm / INCH_MM) * m.xdpi;
    }

    /**
     * @param mm Millimetri da convertire
     * @param m Metriche del display
     * @return Numero di pixel presenti nei millimetri indicati sull'asse Y del display (<code>mm</code>)
     */
    public static float mmtopx_Y(float mm, @NotNull DisplayMetrics m) {
        return (mm / INCH_MM) * m.ydpi;
    }

    /**
     * @param px Pixel da convertire
     * @param m Metriche del display
     * @return Numero di millimetri presenti nei pixel indicati sull'asse X del display (<code>mm</code>)
     */
    public static float pxtomm_X(int px, @NotNull DisplayMetrics m) {
        return ((float)px / m.xdpi) * INCH_MM;
    }

    /**
     * @param px Pixel da convertire
     * @param m Metriche del display
     * @return Numero di millimetri presenti nei pixel indicati sull'asse Y del display (<code>mm</code>)
     */
    public static float pxtomm_Y(int px, @NotNull DisplayMetrics m) {
        return ((float)px / m.ydpi) * INCH_MM;
    }

    public static RectF mmtopx(@NotNull RectF r, @NotNull DisplayMetrics m) {
        RectF n = new RectF();
        n.left = mmtopx_X(r.left, m);
        n.top = mmtopx_Y(r.top, m);
        n.right = mmtopx_X(r.right, m);
        n.bottom = mmtopx_Y(r.bottom, m);
        return n;
    }


}
