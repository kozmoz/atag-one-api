package org.juurlink.atagone.utils;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.junit.Test;

public class IOUtilsTest {

	@Test
	public void testToString() throws Exception {
		final byte[] bytes = "TEST".getBytes("UTF-8");
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		assertEquals("TEST", IOUtils.toString(stream, "UTF-8"));
	}

	@Test
	public void testToString_Null() throws Exception {
		assertEquals("", IOUtils.toString(null, "UTF-8"));
	}

	@Test(expected = UnsupportedEncodingException.class)
	public void testToString_Illegal_Charset() throws Exception {
		final byte[] bytes = "TEST".getBytes("UTF-8");
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		assertEquals("", IOUtils.toString(stream, "Whatever"));
	}
}
