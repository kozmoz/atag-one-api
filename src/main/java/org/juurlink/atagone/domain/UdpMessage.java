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
    InetAddress senderAddress;
    @Nonnull
    String message;
}
