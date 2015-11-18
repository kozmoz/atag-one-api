package org.juurlink.atagone;

import lombok.experimental.UtilityClass;

/**
 * Utility.
 */
@UtilityClass
public class StringUtils {
	public static boolean isBlank(final String string) {
		return string == null || string.trim().length() == 0;
	}
}
