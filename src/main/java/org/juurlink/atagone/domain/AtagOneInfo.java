package org.juurlink.atagone.domain;

import lombok.Builder;
import lombok.Value;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetAddress;

/**
 * ATAG One device id and ip address.
 */
@Value
@Builder
public class AtagOneInfo {
    @Nonnull
    InetAddress deviceAddress;
    /**
     * Not required for local communication.
     */
    @Nullable
    String deviceId;
}
