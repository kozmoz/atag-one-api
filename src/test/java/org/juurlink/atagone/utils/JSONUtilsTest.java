package org.juurlink.atagone.utils;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class JSONUtilsTest {

	@Test
	public void testToJSON() {
		Map<String, Object> testData = new LinkedHashMap<String, Object>();
		testData.put("deviceId", "6808-1401-3109_15-30-001-544");
		testData.put("latestReportTime", "2015-11-20 01:16:45");
		testData.put("roomTemperature", new BigDecimal("20.4"));
		testData.put("dhwSetpoint", new BigDecimal("60"));
		testData.put("chWaterTemperature", new BigDecimal("55.5"));
		testData.put("flameStatus", false);
		testData.put("boilerHeatingFor", null);

		String actual = JSONUtils.toJSON(testData);
		String expected = "{\n" +
			"    deviceId: \"6808-1401-3109_15-30-001-544\",\n" +
			"    latestReportTime: \"2015-11-20 01:16:45\",\n" +
			"    roomTemperature: 20.4,\n" +
			"    dhwSetpoint: 60,\n" +
			"    chWaterTemperature: 55.5,\n" +
			"    flameStatus: false,\n" +
			"    boilerHeatingFor: null\n" +
			"}";
		assertEquals(expected, actual);
	}
}
