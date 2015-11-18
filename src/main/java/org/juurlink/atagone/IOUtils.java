package org.juurlink.atagone;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import lombok.experimental.UtilityClass;

/**
 * IO utils.
 */
@UtilityClass
public class IOUtils {

	/**
	 * Close stream, ignore all errors.
	 */
	public static void closeQuietly(final Closeable closeable) {
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
	 * @param stream   Input Stream
	 * @param encoding Character encoding
	 */
	public static String toString(final InputStream stream, final String encoding) throws IOException {

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
