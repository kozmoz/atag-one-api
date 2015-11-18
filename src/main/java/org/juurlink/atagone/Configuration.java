package org.juurlink.atagone;

import lombok.Value;

/**
 * Program configurations value object.
 */
@Value
public class Configuration {
	private String email;
	private String password;
	private boolean debug;
}
