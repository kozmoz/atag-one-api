package org.juurlink.atagone.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import javax.annotation.Nonnull;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

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
        TimeZone timeZone = TimeZone.getDefault();
        int offset = timeZone.getOffset(atagOneDate * 1000);
        LocalDateTime ofEpoch = LocalDateTime.ofEpochSecond(atagOneDate, 0, ZoneOffset.ofTotalSeconds(offset/1000));
        LocalDateTime reportTime = ofEpoch.plusYears(30).minusDays(1);
        return reportTime;
    }

    /**
     * Format date to YYYY-mm-dd HH:MM:SS.
     *
     * @return formatted date.
     */
    @Nonnull
    public static String formatDate(@Nonnull @NonNull LocalDateTime dateObject) {
        // 2015-12-11 23:56:55
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return formatter.format(dateObject);
    }
}
