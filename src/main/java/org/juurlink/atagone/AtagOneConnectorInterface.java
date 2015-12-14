package org.juurlink.atagone;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import javax.annotation.Nonnull;

import org.juurlink.atagone.exceptions.AccessDeniedException;
import org.juurlink.atagone.exceptions.AtagPageErrorException;
import org.juurlink.atagone.exceptions.AtagSearchErrorException;

/**
 * ATAG One remote or local connector.
 */
public interface AtagOneConnectorInterface {

	String ENCODING_UTF_8 = "UTF-8";

	// Result map keys.
	String VALUE_DEVICE_ID = "deviceId";
	String VALUE_DEVICE_ALIAS = "deviceAlias";
	String VALUE_LATEST_REPORT_TIME = "latestReportTime";
	String VALUE_CONNECTED_TO = "connectedTo";
	String VALUE_BURNING_HOURS = "burningHours";
	String VALUE_BOILER_HEATING_FOR = "boilerHeatingFor";
	String VALUE_FLAME_STATUS = "flameStatus";
	String VALUE_ROOM_TEMPERATURE = "roomTemperature";
	String VALUE_OUTSIDE_TEMPERATURE = "outsideTemperature";
	String VALUE_DHW_SETPOINT = "dhwSetpoint";
	String VALUE_DHW_WATER_TEMPERATURE = "dhwWaterTemperature";
	String VALUE_CH_SETPOINT = "chSetpoint";
	String VALUE_CH_WATER_TEMPERATURE = "chWaterTemperature";
	String VALUE_CH_WATER_PRESSURE = "chWaterPressure";
	String VALUE_CH_RETURN_TEMPERATURE = "chReturnTemperature";
	String VALUE_TARGET_TEMPERATURE = "targetTemperature";
	String VALUE_CURRENT_MODE = "currentMode";
	String VALUE_VACATION_PLANNED = "vacationPlanned";

	// Variable names in JSON responses.
	String JSON_ROOM_TEMP = "room_temp";

	/**
	 * Login to portal or to local thermostat.
	 *
	 * @return Device ID
	 */
	@Nonnull
	String login() throws IOException, AtagPageErrorException, AtagSearchErrorException;

	/**
	 * Set temperature.
	 *
	 * @return current room temperature.
	 */
	@Nonnull
	BigDecimal setTemperature() throws IOException, AtagPageErrorException;

	/**
	 * Get MAP of diagnostic data.
	 */
	@Nonnull
	Map<String, Object> getDiagnostics() throws InterruptedException, AccessDeniedException, AtagPageErrorException, IOException;
}
