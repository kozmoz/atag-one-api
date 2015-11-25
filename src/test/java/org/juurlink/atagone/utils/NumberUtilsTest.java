package org.juurlink.atagone.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NumberUtilsTest {

	@Test
	public void testRoundHalf() throws Exception {
		assertEquals(0, NumberUtils.roundHalf(0), 0);
		assertEquals(0.5f, NumberUtils.roundHalf(0.5f), 0);
		assertEquals(10f, NumberUtils.roundHalf(10f), 0);
		assertEquals(18f, NumberUtils.roundHalf(18.2f), 0);
		assertEquals(18.5f, NumberUtils.roundHalf(18.3f), 0);
		assertEquals(18.5f, NumberUtils.roundHalf(18.4f), 0);
		assertEquals(18.5f, NumberUtils.roundHalf(18.6f), 0);
		assertEquals(18.5f, NumberUtils.roundHalf(18.7f), 0);
		assertEquals(19f, NumberUtils.roundHalf(18.8f), 0);
		assertEquals(19f, NumberUtils.roundHalf(18.9f), 0);
	}
}
