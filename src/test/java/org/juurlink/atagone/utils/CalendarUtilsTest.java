package org.juurlink.atagone.utils;

import java.time.LocalDateTime;
import java.time.Month;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CalendarUtilsTest {

	@Test
	public void testToDateObject() {
		final long date = 503187998L;
		final LocalDateTime dateObject = CalendarUtils.toDateObject(date);
		final String actual = CalendarUtils.formatDate(dateObject);
		assertEquals("2015-12-10 23:26:38", actual);
	}

        @Test
        public void testToDateObjectSummer() {
		final long date = 547503974L;
		final LocalDateTime dateObject = CalendarUtils.toDateObject(date);
		final String actual = CalendarUtils.formatDate(dateObject);
		assertEquals("2017-05-07 22:26:14", actual);
	}

        
	@Test
	public void testformatDate() {
            LocalDateTime dateTime = LocalDateTime.of(2015, Month.DECEMBER, 11, 0, 6);
	final String actual = CalendarUtils.formatDate(dateTime);
		assertEquals("2015-12-11 00:06:00", actual);
	}

}
