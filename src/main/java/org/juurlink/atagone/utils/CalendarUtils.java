package org.juurlink.atagone.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Date related utility methods.
 */
@UtilityClass
public class CalendarUtils {

    private static final long EPOCH_SECONDS_SINCE_20000101 = 946684800L;

    /**
     * Convert ATAG One date to Java date-time object (without timezone info).
     *
     * @return DateTime object
     */
    @Nonnull
    public static LocalDateTime toDateObject(final long atagOneDate) {
        val dateWithTimezone = new Date((atagOneDate + EPOCH_SECONDS_SINCE_20000101) * 1000);
        return LocalDateTime.ofInstant(dateWithTimezone.toInstant(), ZoneId.systemDefault());
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
