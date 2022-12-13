package org.juurlink.atagone.domain;

import lombok.Builder;
import lombok.Value;

/**
 * ATAG One portal login details.
 */
@Value
@Builder
public class PortalCredentials {
    String emailAddress;
    String password;
}
