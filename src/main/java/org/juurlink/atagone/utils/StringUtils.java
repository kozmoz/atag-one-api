package org.juurlink.atagone.utils;

import java.text.Collator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

	/**
	 * Sort list, ignore upper and lower case.
	 *
	 * @param stringList List to sort
	 */
	public static void sort(List<String> stringList) {
		// Default ASCII sort.
		Collator collator = Collator.getInstance(Locale.US);
		// Ignore upper and lowercase.
		collator.setStrength(Collator.PRIMARY);

		Collections.sort(stringList, collator);
	}

}
