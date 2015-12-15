package org.juurlink.atagone.utils;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * HTML related utility methods.
 */
@UtilityClass
public class HTMLUtils {

	private static final Pattern PATTERN_REQUEST_VERIFICATION_TOKEN = Pattern
		.compile("name=\"__RequestVerificationToken\"[^>]+ value=\"(.*?)\"", Pattern.DOTALL);
	private static final Pattern PATTERN_DEVICE_ID = Pattern
		.compile("[0-9]{4}-[0-9]{4}-[0-9]{4}_[0-9]{2}-[0-9]{2}-[0-9]{3}-[0-9]{3}", Pattern.DOTALL);

	/**
	 * Get value from Atag portal diagnostic screen.
	 *
	 * @param html   Full html of page
	 * @param clazz  Class type of value; [String, BigInteger, Boolean]
	 * @param labels Labels to search for, one or more
	 * @return Value or null when not found
	 * @throws IllegalStateException When requested value class not supported
	 */
	@Nullable
	public static <T> T getValueByLabel(@Nonnull @NonNull final String html, @Nonnull @NonNull final Class<T> clazz, final String... labels) {

		// HTML structure of values in page.
		//     <label class="col-xs-6 control-label">Apparaat alias</label>
		//     <div class="col-xs-6">
		//         <p class="form-control-static">CV-ketel</p>
		//     </div>
		for (final String label : labels) {
			final Pattern pattern = Pattern.compile(">" + label + "</label>.*?<p[^>]*>(.*?)<", Pattern.DOTALL + Pattern.CASE_INSENSITIVE);
			final Matcher matcher = pattern.matcher(html);
			if (matcher.find()) {
				final String value = matcher.group(1);
				if (!value.isEmpty()) {
					// Replace Dutch decimal separator.
					final String valueString = value.replace(",", ".").trim();
					if (clazz == String.class) {
						return clazz.cast(valueString);
					}
					if (clazz == Boolean.class) {
						return clazz.cast("aan".equalsIgnoreCase(valueString) || "on".equalsIgnoreCase(valueString));
					}
					if (clazz == BigDecimal.class) {
						return clazz.cast(new BigDecimal(valueString));
					}
					throw new IllegalStateException("Unknown return type requested: '" + clazz + "'");
				}
			}
		}
		return null;
	}

	/**
	 * Extract RequestVerificationToken from HTML.
	 *
	 * @param html HTML
	 * @return RequestVerificationToken or null when not found in HTML
	 */
	@Nullable
	public static String extractRequestVerificationToken(@Nonnull @NonNull final String html) {
		String result = null;
		@SuppressWarnings("SpellCheckingInspection")
		// <input name="__RequestVerificationToken" type="hidden" value="lFVlMZlt2-YJKAwZWS_K_p3gsQWjZOvBNBZ3lM8io_nFGFL0oRsj4YwQUdqGfyrEqGwEUPmm0FgKH1lPWdk257tuTWQ1" />
		final Matcher matcher = PATTERN_REQUEST_VERIFICATION_TOKEN.matcher(html);
		if (matcher.find()) {
			result = matcher.group(1);
		}
		return result;
	}

	/**
	 * Extract Device ID from HTML.
	 *
	 * @param html HTML
	 * @return Device ID or null when ID not found within HTML
	 */
	@Nullable
	public static String extractDeviceId(@Nonnull @NonNull final String html) {
		String result = null;
		// <tr onclick="javascript:changeDeviceAndRedirect('/Home/Index/{0}','6808-1401-3109_15-30-001-544');">
		final Matcher matcher = PATTERN_DEVICE_ID.matcher(html);
		if (matcher.find()) {
			result = matcher.group(0);
		}
		return result;
	}
}
