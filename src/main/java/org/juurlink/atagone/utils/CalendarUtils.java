package org.juurlink.atagone.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import javax.annotation.Nonnull;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;

/**
 * Date related utility methods.
 */
@UtilityClass
public class CalendarUtils {

    /**
     * Convert ATAG One date to Java date Object.
     *
     * @return Date object
     */
    @Nonnull
    public static LocalDateTime toDateObject(final long atagOneDate) {
        val timeZone = TimeZone.getDefault();
        val offset = timeZone.getOffset(atagOneDate * 1000);
        val ofEpoch = LocalDateTime.ofEpochSecond(atagOneDate, 0, ZoneOffset.ofTotalSeconds(offset / 1000));
        return ofEpoch.plusYears(30).minusDays(1);
    }

    /**
     * Format date to YYYY-mm-dd HH:MM:SS.
     *
     * @return formatted date.
     */
    @Nonnull
    public static String formatDate(@Nonnull @NonNull LocalDateTime dateObject) {
        // 2015-12-11 23:56:55
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(dateObject);
    }
}
