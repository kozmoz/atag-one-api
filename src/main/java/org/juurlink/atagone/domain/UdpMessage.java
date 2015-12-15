package org.juurlink.atagone.domain;

import java.net.InetAddress;

import javax.annotation.Nonnull;

import lombok.Value;

/**
 * Received UDP message.
 */
@Value
public class UdpMessage {
	@Nonnull
	private InetAddress deviceAddress;
	@Nonnull
	private String message;
}
