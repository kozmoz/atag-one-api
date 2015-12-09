package org.juurlink.atagone.utils;

import com.sun.istack.internal.NotNull;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * JSON utils.
 */
@UtilityClass
public class JSONUtils {

	/**
	 * Convert Java Map to JSON String, pretty printed.
	 *
	 * @param map Java Map
	 * @return JSON as String
	 */
	@Nonnull
	public static String toJSON(@Nonnull @NonNull Map map) {
		StringBuilder result = new StringBuilder();
		result.append("{");

		boolean first = true;
		@SuppressWarnings("unchecked")
		final Set<Entry> entrySet = map.entrySet();
		for (Entry entry : entrySet) {
			if (!first) {
				result.append(",");
			}
			first = false;
			result.append("\n    ");
			result.append(entry.getKey()).append(": ");
			if (entry.getValue() instanceof String) {
				result.append("\"").append(entry.getValue()).append("\"");
			} else {
				result.append(entry.getValue());
			}
		}
		result.append("\n}");
		return result.toString();
	}

	/**
	 * Get value from JSON data blob. Only get primitive values like strings and numbers at any level. First value found is returned.
	 *
	 * @param json  Full JSON as String
	 * @param clazz Class type of value; [String, BigInteger, Boolean]
	 * @param name  Variable name to search for at any level
	 * @return Value or null when not found
	 * @throws IllegalStateException When requested value class not supported
	 */
	@Nullable
	public static <T> T getJSONValueByName(@Nonnull @NotNull final String json, @Nonnull @NotNull final Class<T> clazz,
		@Nonnull @NotNull final String name) {

		// Real world JSON example.
		// {"isHeating":false,"targetTemp":"17.0","currentTemp":"16.9","vacationPlanned":false,"currentMode":"manual"}
		String escapedName = name.replaceAll("[^a-zA-Z0-9_]+", "");

		// We need to double escape the backslash in RegEx.
		// http://stackoverflow.com/questions/11769555/java-regular-expression-to-match-a-backslash-followed-by-a-quote
		final Pattern pattern = Pattern
			.compile("(?:\"|\\\\\"|)" + escapedName + "(?:\"|\\\\\"|)\\s*:\\s*(?:\"|\\\\\"|)(.+?)(?:\"|\\\\\"|)[,}]", Pattern.DOTALL);
		final Matcher matcher = pattern.matcher(json);
		if (matcher.find()) {
			final String value = matcher.group(1);
			if (!value.isEmpty()) {
				// Replace Dutch decimal separator.
				if (clazz == String.class) {
					return clazz.cast(value);
				}
				if (clazz == Boolean.class) {
					return clazz.cast(Boolean.valueOf(value));
				}
				if (clazz == BigDecimal.class) {
					return clazz.cast(new BigDecimal(value));
				}
				throw new IllegalStateException("Unknown return type requested: '" + clazz + "'");
			}
		}
		return null;
	}
}
