package org.juurlink.atagone.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.nio.charset.Charset;

/**
 * IO utils.
 */
@UtilityClass
public class IOUtils {

    /**
     * Close stream, ignore all errors.
     */
    public static void closeQuietly(@Nullable final Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            // Prevent Class java.net.DatagramSocket does not implement the requested interface java.io.Closeable when Java < 7.
            if (closeable instanceof DatagramSocket) {
                ((DatagramSocket) closeable).close();
            } else {
                closeable.close();
            }
        } catch (IOException e) {
            //Ignore
        }
    }

    /**
     * Read full stream to String.
     *
     * @param stream   Input Stream or null
     * @param encoding Character encoding, ie UTF-8
     * @throws UnsupportedEncodingException If the named charset is not supported
     */
    @Nonnull
    public static String toString(@Nullable final InputStream stream,
                                  @Nonnull @NonNull final Charset encoding) throws IOException {
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
