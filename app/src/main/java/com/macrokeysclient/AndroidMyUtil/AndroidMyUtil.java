package com.macrokeysclient.AndroidMyUtil;

import android.util.DisplayMetrics;

import org.jetbrains.annotations.NotNull;

/**
 * Utility static class
 */
public final class AndroidMyUtil {
    private AndroidMyUtil() {}

    /** 1 incth to millimeters */
    public static final float INCH_MM = 25.4f;

    /**
     * @param px Pixel to convert
     * @param m Display metrics
     * @return Converts the given pixels in millimeters for the X axis
     */
    public static float pxtomm_X(int px, @NotNull DisplayMetrics m) {
        return ((float)px / m.xdpi) * INCH_MM;
    }
}
