package org.juurlink.atagone.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.juurlink.atagone.domain.DeviceInfo;

public class HTTPUtilsTest {

	@Test
	public void testCreatePostBody() throws Exception {
		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put("__RequestVerificationToken", "123456-123%&");
		params.put("Email", "username@test.com");
		params.put("Password", "Password&^%");
		params.put("RememberMe", "false");

		final byte[] result = HTTPUtils.createPostBody(params);
		String resultString = new String(result, "UTF-8");

		assertEquals("__RequestVerificationToken=123456-123%25%26&Email=username%40test.com&Password=Password%26%5E%25&RememberMe=false",
			resultString);
	}

	@Test
	public void testExtractPageErrorFromHtml() throws Exception {
		String html = "                        <div class=\"error validation-message\">\n" +
			"                            <ul>\n" +
			"                                <li class=\"text-error\"><span>Your account has been locked out due to multiple failed login attempts. It will be unlocked in 12 minutes.</span>\n" +
			"                                </li>\n" +
			"                            </ul>\n" +
			"                        </div>\n";
		String message = HTTPUtils.extractPageErrorFromHtml(html);
		String expected = "Your account has been locked out due to multiple failed login attempts. It will be unlocked in 12 minutes.";
		assertEquals(expected, message);
	}

	@Test
	public void testGetMacAddress() throws Exception {
		final DeviceInfo deviceInfo = HTTPUtils.getDeviceInfo();
		assertNotNull(deviceInfo);
		assertNotNull(deviceInfo.getName());
		assertNotNull(deviceInfo.getIp());
		assertNotNull(deviceInfo.getMac());
	}
}
