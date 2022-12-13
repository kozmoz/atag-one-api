package org.juurlink.atagone.domain;

import java.math.BigDecimal;

import javax.annotation.Nullable;

import org.juurlink.atagone.utils.StringUtils;

import lombok.Builder;
import lombok.Value;

/**
 * Program configurations value object.
 */
@Value
@Builder
public class Configuration {

    /**
     * Set temperature, when set.
     */
    @Nullable
    BigDecimal temperature;

    /**
     * Email address of Atag Portal account.
     */
    @Nullable
    String email;

    /**
     * Password of Atag Portal account.
     */
    @Nullable
    String password;

    /**
     * Debug logging.
     */
    boolean debug;

    /**
     * Output format; CSV or JSON.
     */
    @Nullable
    FORMAT format;

    /**
     * Host-name of local thermostat.
     */
    @Nullable
    String hostName;

    /**
     * Skip request for authorization.
     */
    boolean skipAuthRequest;

    /**
     * When true, dump the complete response from the thermostat.
     */
    boolean dump;

    /**
     * Option to override mac address for authentication.
     */
    @Nullable
    String mac;

    /**
     * API library version.
     * (Used for HTTP request header).
     */
    @Nullable
    Version version;

    /**
     * When no portal email address given, we presume local operation.
     */
    public boolean isLocal() {
        return StringUtils.isBlank(email);
    }
}
