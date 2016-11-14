package org.juurlink.atagone.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class NumberUtils {

    /**
     * Rounding to the nearest half or whole.
     * <p/>
     * http://stackoverflow.com/questions/16806987/rounding-to-the-nearest-half-not-the-nearest-whole
     */
    public static float roundHalf(final float pTemperature) {
        return Math.round(pTemperature * 2) / 2.0f;
    }

}
