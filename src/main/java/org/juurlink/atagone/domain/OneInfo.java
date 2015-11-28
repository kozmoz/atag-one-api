package org.juurlink.atagone.domain;

import java.net.InetAddress;

import lombok.Value;

@Value
public class OneInfo {
	private InetAddress ip;
	private String deviceId;
}
