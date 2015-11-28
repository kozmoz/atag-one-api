package org.juurlink.atagone.domain;

import java.net.InetAddress;

import lombok.Value;

/**
 * Device info like, name, ip - and mac address.
 */
@Value
public class DeviceInfo {
	private String name;
	private InetAddress ip;
	private String mac;
}
