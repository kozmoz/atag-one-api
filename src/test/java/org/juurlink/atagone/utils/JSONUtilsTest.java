package org.juurlink.atagone.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

	@Test
	public void testGetJSONValueByName() throws Exception {
		String json = "{\"isHeating\":false,\"targetTemp\":\"17.0\",\"currentTemp\":\"16.9\",\"vacationPlanned\":false,\"currentMode\":\"manual\"}";
		assertEquals(Boolean.FALSE, JSONUtils.getJSONValueByName(json, Boolean.class, "isHeating"));
		assertEquals(new BigDecimal("17.0"), JSONUtils.getJSONValueByName(json, BigDecimal.class, "targetTemp"));
		assertEquals(new BigDecimal("16.9"), JSONUtils.getJSONValueByName(json, BigDecimal.class, "currentTemp"));
		assertEquals("manual", JSONUtils.getJSONValueByName(json, String.class, "currentMode"));

		// Well, this is the exact format returned, sort of escaped JSON.
		String html = "\"{\\\"ch_control_mode\\\":0,\\\"temp_influenced\\\":false,\\\"room_temp\\\":18.0,\\\"ch_mode_temp\\\":18.0,\\\"is_heating\\\":false,\\\"vacationPlanned\\\":false,\\\"temp_increment\\\":null,\\\"round_half\\\":false,\\\"schedule_base_temp\\\":null,\\\"outside_temp\\\":null}\"";
		assertEquals(new BigDecimal("18.0"), JSONUtils.getJSONValueByName(html, BigDecimal.class, "room_temp"));
		html = "\"{\\\"ch_control_mode\\\":0,\\\"temp_influenced\\\":false,\\\"room_temp\\\":18.6,\\\"ch_mode_temp\\\":18.0,\\\"is_heating\\\":false,\\\"vacationPlanned\\\":false,\\\"temp_increment\\\":null,\\\"round_half\\\":false,\\\"schedule_base_temp\\\":null,\\\"outside_temp\\\":null}\"";
		assertEquals(new BigDecimal("18.6"), JSONUtils.getJSONValueByName(html, BigDecimal.class, "room_temp"));
		html = "\"{\\\"ch_control_mode\\\":0,\\\"temp_influenced\\\":false,\\\"room_temp\\\":8,\\\"ch_mode_temp\\\":18.0,\\\"is_heating\\\":false,\\\"vacationPlanned\\\":false,\\\"temp_increment\\\":null,\\\"round_half\\\":false,\\\"schedule_base_temp\\\":null,\\\"outside_temp\\\":null}\"";
		assertEquals(new BigDecimal("8"), JSONUtils.getJSONValueByName(html, BigDecimal.class, "room_temp"));
		html = "\"{\\\"ch_control_mode\\\":0,\\\"temp_influenced\\\":false,\\\"xxxx_temp\\\":18.5,\\\"ch_mode_temp\\\":18.0,\\\"is_heating\\\":false,\\\"vacationPlanned\\\":false,\\\"temp_increment\\\":null,\\\"round_half\\\":false,\\\"schedule_base_temp\\\":null,\\\"outside_temp\\\":null}\"";
		assertNull(JSONUtils.getJSONValueByName(html, BigDecimal.class, "room_temp"));
	}

}
