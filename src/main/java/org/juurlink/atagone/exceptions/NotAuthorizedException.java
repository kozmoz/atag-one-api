package org.juurlink.atagone.exceptions;

import java.io.IOException;

/**
 * User did not approve authorization request.
 */
public class NotAuthorizedException extends IOException {

    /**
     * Constructs an {@code IOException} with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public NotAuthorizedException(String message) {
        super(message);
    }

}
