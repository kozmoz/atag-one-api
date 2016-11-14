package org.juurlink.atagone.exceptions;

import java.io.IOException;

public class AtagSearchErrorException extends IOException {

    /**
     * Constructs an {@code IOException} with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public AtagSearchErrorException(String message) {
        super(message);
    }

}
