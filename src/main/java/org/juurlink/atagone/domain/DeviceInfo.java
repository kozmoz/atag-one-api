package org.juurlink.atagone.domain;

import java.net.InetAddress;

import javax.annotation.Nonnull;

import lombok.Builder;
import lombok.Value;

/**
 * Device (computer) info like, name, ip - and mac address.
 */
@Value
@Builder
public class DeviceInfo {
	@Nonnull
	private String name;
	@Nonnull
	private InetAddress ip;
	@Nonnull
	private String mac;
}
