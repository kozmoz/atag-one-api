package org.juurlink.atagone.domain;

import lombok.Value;

/**
 * ATAG One portal login details.
 */
@Value
public class PortalCredentials {
	private String emailAddress;
	private String password;
}
