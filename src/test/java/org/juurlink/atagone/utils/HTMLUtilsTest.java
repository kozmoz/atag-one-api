package org.juurlink.atagone.utils;

import java.math.BigDecimal;

import org.junit.Test;

import static org.junit.Assert.*;

public class HTMLUtilsTest {

	@Test
	@SuppressWarnings("ConstantConditions")
    public void testGetDiagnosticValueByLabel() {
		final String html = "            <div class=\"form-group no-border-top\">\n" +
			"                <label class=\"col-xs-6 control-label\">Apparaat</label>\n" +
			"                <div class=\"col-xs-6\">\n" +
			"                    <p class=\"form-control-static\">6808-1401-3109_15-30-001-555</p>\n" +
			"                </div>\n" +
			"            </div>\n" +
			"            <div class=\"form-group\">\n" +
			"                <label class=\"col-xs-6 control-label\">Apparaat alias</label>\n" +
			"                <div class=\"col-xs-6\">\n" +
			"                    <p class=\"form-control-static\">CV-ketel</p>\n" +
			"                </div>\n" +
			"            </div>\n" +
			"            <div class=\"form-group\">\n" +
			"                <label class=\"col-xs-6 control-label\">Laatste rapportagetijd</label>\n" +
			"                <div class=\"col-xs-6\">\n" +
			"                    <p class=\"form-control-static\">\n" +
			"  2015-11-18 00:07:48</p>\n" +
			"                </div>\n" +
			"            </div>\n" +
			"            <div class=\"form-group\">\n" +
			"                <label class=\"col-xs-6 control-label\">Branduren</label>\n" +
			"                <div class=\"col-xs-6\">\n" +
			"                    <p class=\"form-control-static\">34,95</p>\n" +
			"                </div>\n" +
			"            </div>\n" +
			"            <div class=\"form-group\">\n" +
			"                <label class=\"col-xs-6 control-label\">Brander status</label>\n" +
			"                <div class=\"col-xs-6\">\n" +
			"                        <p class=\"form-control-static\">Aan</p>\n" +
			"                </div>\n" +
			"            </div>\n" +
			"<div class=\"form-group\"><label class=\"col-xs-6 control-label\">Outside temperature</label>\n" +
			"            <div class=\"col-xs-6\"><p class=\"form-control-static\">12.5&#176;                    </p></div>\n" +
			"        </div>\n" +
			"        <div class=\"form-group\"><label class=\"col-xs-6 control-label\">DHW setpoint</label>\n" +
			"            <div class=\"col-xs-6\"><p class=\"form-control-static\">60.0&#176;</p></div>\n" +
			"        </div>";

		Object actual = HTMLUtils.getValueByLabel(html, String.class, "Apparaat alias", "");
		assertEquals("CV-ketel", actual);

		actual = HTMLUtils.getValueByLabel(html, String.class, "apparaat Alias");
		assertEquals("CV-ketel", actual);

		actual = HTMLUtils.getValueByLabel(html, String.class, "Device Alias", "apparaat alias");
		assertEquals("CV-ketel", actual);

		actual = HTMLUtils.getValueByLabel(html, String.class, "Laatste rapportagetijd");
		assertEquals("2015-11-18 00:07:48", actual);

		actual = HTMLUtils.getValueByLabel(html, BigDecimal.class, "Branduren");
		assertEquals(BigDecimal.class, actual.getClass());
		assertEquals("34.95", actual.toString());

		actual = HTMLUtils.getValueByLabel(html, Boolean.class, "Brander status");
		assertEquals(Boolean.class, actual.getClass());
		assertTrue((Boolean) actual);

		actual = HTMLUtils.getValueByLabel(html, BigDecimal.class, "Buitentemperatuur", "Outside temperature");
		assertEquals(BigDecimal.class, actual.getClass());
		assertEquals("12.5", actual.toString());
	}

	@Test
	@SuppressWarnings("ConstantConditions")
    public void testGetDiagnosticValueByLabelNegativeTemperature() {
		final String html = "" +
				"<fieldset>\n" +
				"        <legend>DIAGNOSTICS</legend>\n" +
				"            <div class=\"form-group no-border-top\">\n" +
				"                <label class=\"col-xs-6 control-label\">Latest report time</label>\n" +
				"                <div class=\"col-xs-6\">\n" +
				"                    <p class=\"form-control-static\">2022-12-13 16:34:16</p>\n" +
				"                </div>\n" +
				"            </div>\n" +
				"            <div class=\"form-group\">\n" +
				"                <label class=\"col-xs-6 control-label\">Burning hours</label>\n" +
				"                <div class=\"col-xs-6\">\n" +
				"                    <p class=\"form-control-static\">9205.95</p>\n" +
				"                </div>\n" +
				"            </div>\n" +
				"            <div class=\"form-group\">\n" +
				"                <label class=\"col-xs-6 control-label\">Room temperature</label>\n" +
				"                <div class=\"col-xs-6\">\n" +
				"                    <p class=\"form-control-static\">20.1</p>\n" +
				"                </div>\n" +
				"            </div>\n" +
				"            <div class=\"form-group no-border-top\">\n" +
				"                <div class=\"col-xs-12\">&nbsp;</div>\n" +
				"            </div>\n" +
				"            <div class=\"form-group no-border-top\">\n" +
				"                <label class=\"col-xs-6 control-label\">Boiler heating for</label>\n" +
				"                <div class=\"col-xs-6\">\n" +
				"                </div>\n" +
				"            </div>\n" +
				"            <div class=\"form-group\">\n" +
				"                <label class=\"col-xs-6 control-label\">Flame status</label>\n" +
				"                <div class=\"col-xs-6\">\n" +
				"                            <p class=\"form-control-static\">Off</p>\n" +
				"                </div>\n" +
				"            </div>\n" +
				"            <div class=\"form-group\">\n" +
				"                <label class=\"col-xs-6 control-label\">CH setpoint</label>\n" +
				"                <div class=\"col-xs-6\">\n" +
				"                    <p class=\"form-control-static\">40.3°</p>\n" +
				"                </div>\n" +
				"            </div>\n" +
				"            <div class=\"form-group\">\n" +
				"                <label class=\"col-xs-6 control-label\">CH water temperature</label>\n" +
				"                <div class=\"col-xs-6\">\n" +
				"                    <p class=\"form-control-static\">46.6°</p>\n" +
				"                </div>\n" +
				"            </div>\n" +
				"            <div class=\"form-group\">\n" +
				"                <label class=\"col-xs-6 control-label\">CH return temperature</label>\n" +
				"                <div class=\"col-xs-6\">\n" +
				"                    <p class=\"form-control-static\">46.6°</p>\n" +
				"                </div>\n" +
				"            </div>\n" +
				"            <div class=\"form-group\">\n" +
				"                <label class=\"col-xs-6 control-label\">dT</label>\n" +
				"                <div class=\"col-xs-6\">\n" +
				"                    <p class=\"form-control-static\">0.0°</p>\n" +
				"                </div>\n" +
				"            </div>\n" +
				"            <div class=\"form-group\">\n" +
				"                <label class=\"col-xs-6 control-label\">CH water pressure</label>\n" +
				"                <div class=\"col-xs-6\">\n" +
				"                    <p class=\"form-control-static\">1.6</p>\n" +
				"                </div>\n" +
				"            </div>\n" +
				"            <div class=\"form-group\">\n" +
				"                <label class=\"col-xs-6 control-label\">Outside temperature</label>\n" +
				"                <div class=\"col-xs-6\">\n" +
				"                    <p class=\"form-control-static\">\n" +
				"-2.1°                    </p>\n" +
				"                </div>\n" +
				"            </div>\n" +
				"                <div class=\"form-group\">\n" +
				"                    <label class=\"col-xs-6 control-label\">Average outside temperature</label>\n" +
				"                    <div class=\"col-xs-6\">\n" +
				"                        <p class=\"form-control-static\">\n" +
				"-2.0°                        </p>\n" +
				"                    </div>\n" +
				"                </div>\n" +
				"        <div class=\"form-group\">\n" +
				"            <label class=\"col-xs-6 control-label\">DHW setpoint</label>\n" +
				"            <div class=\"col-xs-6\">\n" +
				"                <p class=\"form-control-static\">43.0°</p>\n" +
				"            </div>\n" +
				"        </div>\n" +
				"        <div class=\"form-group\">\n" +
				"            <label class=\"col-xs-6 control-label\">DHW water temperature</label>\n" +
				"            <div class=\"col-xs-6\">\n" +
				"                <p class=\"form-control-static\">47.5°</p>\n" +
				"            </div>\n" +
				"        </div>\n" +
				"    </fieldset>";

		Object actual = HTMLUtils.getValueByLabel(html, String.class, "Laatste rapportagetijd", "Latest report time");
		assertEquals("2022-12-13 16:34:16", actual);

		actual = HTMLUtils.getValueByLabel(html, BigDecimal.class, "Branduren", "Burning hours");
		assertEquals(BigDecimal.class, actual.getClass());
		assertEquals("9205.95", actual.toString());

		actual = HTMLUtils.getValueByLabel(html, Boolean.class, "Brander status", "Flame status");
		assertEquals(Boolean.class, actual.getClass());
		assertFalse((Boolean) actual);

		actual = HTMLUtils.getValueByLabel(html, BigDecimal.class, "Buitentemperatuur", "Outside temperature");
		assertEquals(BigDecimal.class, actual.getClass());
		assertEquals("-2.1", actual.toString());
	}

	@Test
    public void testGetRequestVerificationToken() {
		final String html = "<div id=\"content\" class=\"col-xs-offset-0 col-xs-12 col-sm-offset-1 col-sm-6\">\n" +
			"<form action=\"/Account/Login\" autocomplete=\"off\" class=\"form-horizontal\" method=\"post\">" +
			"<input name=\"__RequestVerificationToken\" type=\"hidden\" value=\"lFVlMZlt2-YJKAwZWS_K_p3gsQWjZOvBNBZ3lM8io_nFGFL0oRsj4YwQUdqGfyrEqGwEUPmm0FgKH1lPWdk257tuTWQ1\" />    <div class=\"login-container center-block\">\n" +
			"        <fieldset>\n" +
			"            <div class=\"form-group\">\n" +
			"                <input class=\"form-control input-lg text-center\" id=\"Email\" name=\"Email\" placeholder=\"Email\" type=\"email\" value=\"\" />\n";

		final String actual = HTMLUtils.extractRequestVerificationToken(html);
		assertEquals("lFVlMZlt2-YJKAwZWS_K_p3gsQWjZOvBNBZ3lM8io_nFGFL0oRsj4YwQUdqGfyrEqGwEUPmm0FgKH1lPWdk257tuTWQ1", actual);
	}

	@Test
    public void testGetDeviceId() {
		final String html = "<tr onclick=\"javascript:changeDeviceAndRedirect('/Home/Index/{0}','6808-1401-3109_15-30-001-555');\">";

		final String actual = HTMLUtils.extractDeviceId(html);
		assertEquals("6808-1401-3109_15-30-001-555", actual);
	}
}
