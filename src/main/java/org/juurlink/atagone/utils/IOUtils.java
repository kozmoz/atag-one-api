package org.juurlink.atagone.utils;

import com.sun.istack.internal.NotNull;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.experimental.UtilityClass;

/**
 * IO utils.
 */
@UtilityClass
public class IOUtils {

	/**
	 * Close stream, ignore all errors.
	 */
	public static void closeQuietly(@Nullable final Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				//Ignore
			}
		}
	}

	/**
	 * Read full stream to String.
	 *
	 * @param stream   Input Stream or null
	 * @param encoding Character encoding, ie UTF-8
	 * @throws UnsupportedEncodingException If the named charset is not supported
	 */
	@NonNull
	public static String toString(@Nullable final InputStream stream, @NotNull final String encoding) throws IOException {
		if (stream == null) {
			return "";
		}

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(stream, encoding));
			StringBuilder out = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				out.append(line);
			}
			return out.toString();
		} finally {
			closeQuietly(reader);
		}
	}
}
