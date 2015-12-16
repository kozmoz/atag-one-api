package org.juurlink.atagone.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
	public static Date toDateObject(final long atagOneDate) {
		final Calendar calendar = Calendar.getInstance();
		// Convert seconds to milliseconds and add 30 years.
		calendar.setTime(new Date(atagOneDate * 1000L));
		calendar.add(Calendar.YEAR, 30);
		return calendar.getTime();
	}

	/**
	 * Format date to YYYY-mm-dd HH:MM:SS.
	 *
	 * @return formatted date.
	 */
	@Nonnull
	public static String formatDate(@Nonnull @NonNull Date dateObject) {
		// 2015-12-11 23:56:55
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return formatter.format(dateObject);
	}
}
