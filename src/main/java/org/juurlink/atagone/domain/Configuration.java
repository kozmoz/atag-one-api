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
	private String email;

	/**
	 * Password of Atag Portal account.
	 */
	@Nullable
	private String password;

	/**
	 * Debug logging.
	 */
	private boolean debug;

	/**
	 * Output format; CSV or JSON.
	 */
	@Nullable
	private FORMAT format;

	/**
	 * Host-name of local thermostat.
	 */
	@Nullable
	private String hostName;

	/**
	 * Skip request for authorization.
	 */
	private boolean skipAuthRequest;

	/**
	 * When true, dump the complete response from the thermostat.
	 */
	private boolean dump;

	/**
	 * Option to override mac address for authentication.
	 */
	@Nullable
	private String mac;

	/**
	 * When no portal email address given, we presume local operation.
	 */
	public boolean isLocal() {
		return StringUtils.isBlank(email);
	}
}
