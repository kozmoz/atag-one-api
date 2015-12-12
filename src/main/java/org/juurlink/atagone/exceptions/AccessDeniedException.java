package org.juurlink.atagone.exceptions;

public class AccessDeniedException extends Exception {

	/**
	 * Constructs an {@code IOException} with the specified detail message.
	 *
	 * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
	 */
	public AccessDeniedException(String message) {
		super(message);
	}

}
