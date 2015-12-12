package org.juurlink.atagone.utils;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;

public class CalendarUtilsTest {

	@Test
	public void testToDateObject() {
		final long date = 503187998L;
		final Date dateObject = CalendarUtils.toDateObject(date);
		final String actual = CalendarUtils.formatDate(dateObject);
		assertEquals("2015-12-11 23:26:38", actual);
	}

	@Test
	public void testformatDate() {
		Calendar calendar = new GregorianCalendar(2015, Calendar.DECEMBER, 11, 0, 6);
		final Date date = calendar.getTime();

		final String actual = CalendarUtils.formatDate(date);
		assertEquals("2015-12-11 00:06:00", actual);
	}

}
