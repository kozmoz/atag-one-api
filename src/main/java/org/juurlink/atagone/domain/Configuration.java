package org.juurlink.atagone.domain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Value;

/**
 * Program configurations value object.
 */
@Value
public class Configuration {
	@Nullable
	Float temperature;
	@Nonnull
	private String email;
	@Nonnull
	private String password;
	private boolean debug;
	@Nonnull
	private FORMAT format;
}
