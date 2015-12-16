package org.juurlink.atagone.domain;

import javax.annotation.Nonnull;

import lombok.Builder;
import lombok.Value;

/**
 * Version info and build timestamp.
 */
@Value
@Builder
public class Version {
	@Nonnull
	private String version;
	@Nonnull
	private String timestamp;
}
