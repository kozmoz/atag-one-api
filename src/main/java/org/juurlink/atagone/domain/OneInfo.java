package org.juurlink.atagone.domain;

import java.net.InetAddress;

import javax.annotation.Nonnull;

import lombok.Value;

/**
 * ATAG One info.
 */
@Value
public class OneInfo {
	@Nonnull
	private InetAddress deviceAddress;
	@Nonnull
	private String deviceId;
}
