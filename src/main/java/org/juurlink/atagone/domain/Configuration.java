package org.juurlink.atagone.domain;

import java.math.BigDecimal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.juurlink.atagone.utils.StringUtils;

import lombok.Value;

/**
 * Program configurations value object.
 */
@Value
public class Configuration {
	@Nullable
	BigDecimal temperature;
	@Nullable
	private String email;
	@Nullable
	private String password;
	private boolean debug;
	@Nonnull
	private FORMAT format;

	/**
	 * When no portal email address given, we presume local operation.
	 */
	public boolean isLocal() {
		return StringUtils.isBlank(email);
	}
}
