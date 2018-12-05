package org.juurlink.atagone.utils;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class IOUtilsTest {

	@Test
	public void testToString() throws Exception {
		final byte[] bytes = "TEST".getBytes(StandardCharsets.UTF_8);
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		assertEquals("TEST", IOUtils.toString(stream, StandardCharsets.UTF_8));
	}

	@Test
	public void testToString_Null() throws Exception {
		assertEquals("", IOUtils.toString(null, StandardCharsets.UTF_8));
	}

	@Test(expected = NullPointerException.class)
	public void testToString_Null_Charset() throws Exception {
		final byte[] bytes = "TEST".getBytes(StandardCharsets.UTF_8);
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		assertEquals("", IOUtils.toString(stream, null));
	}
}
