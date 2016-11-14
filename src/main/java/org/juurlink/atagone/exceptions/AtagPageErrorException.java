package org.juurlink.atagone.exceptions;

import java.io.IOException;

public class AtagPageErrorException extends IOException {

    /**
     * Constructs an {@code IOException} with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public AtagPageErrorException(String message) {
        super(message);
    }

}
