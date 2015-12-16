package org.juurlink.atagone.domain;

import java.net.InetAddress;

import javax.annotation.Nonnull;

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
	@Nonnull
	private String deviceId;
}
