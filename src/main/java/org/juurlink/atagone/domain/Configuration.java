package org.juurlink.atagone.domain;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Value;

/**
 * Program configurations value object.
 */
@Value
public class Configuration {
	@Nullable
	Float temperature;
	private String email;
	private String password;
	private boolean debug;
	private FORMAT format;
}
