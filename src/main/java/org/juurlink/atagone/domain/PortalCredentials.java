package org.juurlink.atagone.domain;

import lombok.Builder;
import lombok.Value;

/**
 * ATAG One portal login details.
 */
@Value
@Builder
public class PortalCredentials {
	private String emailAddress;
	private String password;
}
