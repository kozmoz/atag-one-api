package org.juurlink.atagone.utils;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;

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
}
