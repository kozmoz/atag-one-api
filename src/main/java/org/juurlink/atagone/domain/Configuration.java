package org.juurlink.atagone.domain;

import lombok.Value;

/**
 * Program configurations value object.
 */
@Value
public class Configuration {
	private String email;
	private String password;
	private boolean debug;
	private FORMAT format;
}
