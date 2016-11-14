package org.juurlink.atagone.domain;

import java.net.InetAddress;

import javax.annotation.Nonnull;

import lombok.Builder;
import lombok.Value;

/**
 * Received UDP message.
 */
@Value
@Builder
public class UdpMessage {
    @Nonnull
    private InetAddress senderAddress;
    @Nonnull
    private String message;
}
