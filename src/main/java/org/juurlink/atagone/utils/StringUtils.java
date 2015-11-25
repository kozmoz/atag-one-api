package org.juurlink.atagone.utils;

import lombok.experimental.UtilityClass;

/**
 * String Utilities.
 */
@UtilityClass
public class StringUtils {

	/**
	 * Test if given string is null, empty or only contains white space characters.
	 */
	public static boolean isBlank(final String string) {
		return string == null || string.trim().length() == 0;
	}

	/**
	 * Return "" in case of null.
	 */
	public static String defaultString(final String string) {
		return string != null ? string : "";
	}
}
