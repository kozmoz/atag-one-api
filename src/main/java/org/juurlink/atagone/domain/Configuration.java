package org.juurlink.atagone.domain;

import java.net.InetAddress;

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
	@Nullable
	private String email;
	@Nullable
	private String password;
	private boolean debug;
	@Nonnull
	private FORMAT format;
	@Nullable
	private InetAddress deviceAddress;
	// Todo: Device ID?
}
