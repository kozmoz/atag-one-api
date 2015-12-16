package org.juurlink.atagone.utils;

import java.text.Collator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * String Utilities.
 */
@UtilityClass
public class StringUtils {

	/**
	 * Test if given string is null, empty or only contains white space characters.
	 */
	public static boolean isBlank(@Nullable final String string) {
		return string == null || string.trim().length() == 0;
	}

	/**
	 * Return empty string "" in case of null.
	 */
	@Nonnull
	public static String defaultString(@Nullable final String string) {
		return string != null ? string : "";
	}

	/**
	 * Return default string in case of null.
	 */
	@Nonnull
	public static String defaultString(@Nullable final String string, @Nonnull @NonNull final String defaultString) {
		return string != null ? string : defaultString;
	}

	/**
	 * Sort list of Strings, ignore upper and lower case.
	 *
	 * @param stringList List to sort
	 */
	public static void sort(@Nonnull @NonNull List<String> stringList) {
		// Default ASCII sort.
		Collator collator = Collator.getInstance(Locale.US);
		// Ignore upper and lowercase.
		collator.setStrength(Collator.PRIMARY);

		Collections.sort(stringList, collator);
	}
}
