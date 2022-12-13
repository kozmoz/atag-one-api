package org.juurlink.atagone.domain;

import javax.annotation.Nonnull;

import lombok.Builder;
import lombok.Value;

/**
 * Version info and build timestamp.
 */
@Value
@Builder
public class Version {
    @Nonnull
    String version;
    @Nonnull
    String timestamp;

    public String toString() {
        return String.format("%s(%s)", version, timestamp);
    }
}
