package org.juurlink.atagone.domain;

import java.net.InetAddress;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Builder;
import lombok.Value;

/**
 * ATAG One device id and ip address
 */
@Value
@Builder
public class AtagOneInfo {
	@Nonnull
	private InetAddress deviceAddress;
	/**
	 * Not required for local communication.
	 */
	@Nullable
	private String deviceId;
}
