package org.juurlink.atagone;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.juurlink.atagone.domain.AtagOneInfo;
import org.juurlink.atagone.domain.Configuration;
import org.juurlink.atagone.domain.DeviceInfo;
import org.juurlink.atagone.domain.UdpMessage;
import org.juurlink.atagone.exceptions.AccessDeniedException;
import org.juurlink.atagone.exceptions.AtagPageErrorException;
import org.juurlink.atagone.exceptions.AtagSearchErrorException;
import org.juurlink.atagone.utils.CalendarUtils;
import org.juurlink.atagone.utils.JSONUtils;
import org.juurlink.atagone.utils.NetworkUtils;
import org.juurlink.atagone.utils.StringUtils;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

/**
 * Connect to ATAG One thermostat in local network.
 */
@Log
public class AtagOneLocalConnector implements AtagOneConnectorInterface {

	private static final int MAX_LISTEN_TIMEOUT_SECONDS = 60;

	/**
	 * Max number of times to wait for thermostat authorization.
	 */
	private static final int MAX_AUTH_RETRIES = 15;

	/**
	 * Time to wait between auth requests.
	 */
	private static final int SLEEP_BETWEEN_AUTH_REQUESTS_MS = 5000;

	private static final int MESSAGE_INFO_CONTROL = 1;
	//private static final int MESSAGE_INFO_SCHEDULES = 2;
	//private static final int MESSAGE_INFO_CONFIGURATION = 4;
	private static final int MESSAGE_INFO_REPORT = 8;
	//private static final int MESSAGE_INFO_STATUS = 16;
	//private static final int MESSAGE_INFO_WIFISCAN = 32;

	/**
	 * UDP port the thermostat sends its messages to.
	 */
	private static final int UDP_BROADCAST_PORT = 11000;

	private final Configuration configuration;

	private final DeviceInfo computerInfo;

	/**
	 * ATAG One Device ID and IP address.
	 */
	@Nullable
	private AtagOneInfo selectedDevice;

	/**
	 * Construct ATAG One connector.
	 *
	 * @throws IOException when error getting local device address
	 */
	public AtagOneLocalConnector(@Nonnull @NonNull final Configuration configuration) throws IOException {
		log.fine("Instantiate " + AtagOneApp.THERMOSTAT_NAME + " local connector");
		this.configuration = configuration;

		// Computer mac address (used for authorization with thermostat)
		computerInfo = NetworkUtils.getDeviceInfo();
	}

	/**
	 * Find the thermostat in the local network and authorize with it.
	 *
	 * @return Device ID
	 */
	@Nonnull
	@Override
	@SneakyThrows
	public String login() throws IOException, AtagPageErrorException, AtagSearchErrorException {

		log.fine("No email address set, Try to find the " + AtagOneApp.THERMOSTAT_NAME + " in the local network.");
		AtagOneInfo atagOneInfo = searchOnes();
		if (atagOneInfo == null) {
			throw new AtagSearchErrorException("Cannot find " + AtagOneApp.THERMOSTAT_NAME + " thermostat in local network.");
		}

		// Device found in local network.
		log.fine(AtagOneApp.THERMOSTAT_NAME + " found in local network: " + atagOneInfo);
		selectedDevice = atagOneInfo;

		// Start authorization proces with thermostat.
		authorizeWithThermostat();

		return selectedDevice.getDeviceId();
	}

	@Nonnull
	@Override
	public BigDecimal setTemperature() {
		log.fine("Set target temperature to " + configuration.getTemperature() + " degrees celsius");
		throw new RuntimeException("'setTemperature' method not yet implemented for thermostat in local network.");
	}

	/**
	 * Get all diagnostics for selected device.
	 *
	 * @return Map of diagnostic info
	 * @throws IOException              in case of connection error
	 * @throws IllegalArgumentException when no device selected
	 */
	@Nonnull
	@Override
	public Map<String, Object> getDiagnostics()
		throws IOException, IllegalArgumentException, AtagPageErrorException, InterruptedException, AccessDeniedException {

		if (selectedDevice == null) {
			throw new IllegalArgumentException("No device selected, cannot get diagnostics.");
		}

		if (computerInfo == null) {
			throw new IllegalArgumentException("Cannot determine MAC address of computer, cannot get diagnostics.");
		}

		final String messageUrl = "http://" + selectedDevice.getDeviceAddress().getHostAddress() + ":10000/retrieve";
		log.fine("POST retrieve: URL=" + messageUrl);

		// Get computer MAC address.
		final String macAddress = computerInfo.getMac();

		// {"retrieve_message":{"seqnr":0,"account_auth":{"user_account":"email@gmail.com","mac_address":"6C:42:98:B6:B2:90"},"info":15}}
		final int info = MESSAGE_INFO_CONTROL + MESSAGE_INFO_REPORT;
		final String jsonPayload = "{\"retrieve_message\":{" +
			"\"seqnr\":0," +
			"\"account_auth\":{" +
			"\"user_account\":\"\"," +
			"\"mac_address\":\"" + macAddress + "\"}," +
			"\"info\":" + info + "}}\n";

		// Sometimes the response is empty.
		int maxRetries = 3;
		String response = null;
		while (StringUtils.isBlank(response) || maxRetries > 0) {
			response = NetworkUtils.getPostPageContent(messageUrl, jsonPayload, NetworkUtils.MAX_CONNECTION_RETRIES);
			log.fine("POST retrieve_message Response\n" + response);
			maxRetries--;

			if (StringUtils.isBlank(response) && maxRetries > 0) {
				log.fine("Empty response, try another time.");
			}
		}

		/*
		{ "retrieve_reply":{ "seqnr":0,

		"status":{
		"device_id":"6808-1401-3109_15-30-001-123",
		"device_status":16385,
		"connection_status":23,
		"date_time":503187998},

		"report":{
		"report_time":503187998,
		"burning_hours":257.09,
		"device_errors":"",
		"boiler_errors":"",
		"room_temp":20.6,
		"outside_temp":5.1,
		"dbg_outside_temp":22.3,
		"pcb_temp":25.0,
		"ch_setpoint":28.1,
		"dhw_water_temp":33.6,
		"ch_water_temp":32.8,
		"dhw_water_pres":0.0,
		"ch_water_pres":1.5,
		"ch_return_temp":33.2,
		"boiler_status":770,
		"boiler_config":772,
		"ch_time_to_temp":0,
		"shown_set_temp":20.5,
		"power_cons":0,
		"rssi":26,
		"current":-155,
		"voltage":3846,
		"resets":11,
		"memory_allocation":2800},

		"control": {
		"ch_status":13,
		"ch_control_mode":0,
		"ch_mode":1,
		"ch_mode_duration":0,
		"ch_mode_temp":20.5,
		"dhw_temp_setp":60.0,
		"dhw_status":5,
		"dhw_mode":1,
		"dhw_mode_temp":60.0,
		"weather_temp":5.1,
		"weather_status":9,
		"vacation_duration":0,
		"extend_duration":0,
		"fireplace_duration":10800
		} ,
		"acc_status":2} }
		 */

		Map<String, Object> values = new LinkedHashMap<String, Object>();
		values.put(VALUE_DEVICE_ID, selectedDevice.getDeviceId());
		values.put(VALUE_DEVICE_IP, selectedDevice.getDeviceAddress().getHostAddress());
		// VALUE_DEVICE_ALIAS; Locally unknown
		final Integer reportTime = JSONUtils.getJSONValueByName(response, Integer.class, "report_time");

		if (reportTime != null) {
			final Date dateObject = CalendarUtils.toDateObject(reportTime);
			values.put(VALUE_LATEST_REPORT_TIME, CalendarUtils.formatDate(dateObject));
		}
		// VALUE_CONNECTED_TO
		values.put(VALUE_BURNING_HOURS, JSONUtils.getJSONValueByName(response, BigDecimal.class, "burning_hours"));
		values.put(VALUE_ROOM_TEMPERATURE, JSONUtils.getJSONValueByName(response, BigDecimal.class, "room_temp"));
		values.put(VALUE_OUTSIDE_TEMPERATURE, JSONUtils.getJSONValueByName(response, BigDecimal.class, "outside_temp"));
		values.put(VALUE_DHW_SETPOINT, JSONUtils.getJSONValueByName(response, BigDecimal.class, "dhw_temp_setp"));
		values.put(VALUE_DHW_WATER_TEMPERATURE, JSONUtils.getJSONValueByName(response, BigDecimal.class, "dhw_water_temp"));
		values.put(VALUE_CH_SETPOINT, JSONUtils.getJSONValueByName(response, BigDecimal.class, "ch_setpoint"));
		values.put(VALUE_CH_WATER_TEMPERATURE, JSONUtils.getJSONValueByName(response, BigDecimal.class, "ch_water_temp"));
		values.put(VALUE_CH_WATER_PRESSURE, JSONUtils.getJSONValueByName(response, BigDecimal.class, "ch_water_pres"));
		values.put(VALUE_CH_RETURN_TEMPERATURE, JSONUtils.getJSONValueByName(response, BigDecimal.class, "ch_return_temp"));
		values.put(VALUE_TARGET_TEMPERATURE, JSONUtils.getJSONValueByName(response, BigDecimal.class, "shown_set_temp"));

		// Values only local available.
		values.put("deviceStatus", JSONUtils.getJSONValueByName(response, Integer.class, "device_status"));
		values.put("connectionStatus", JSONUtils.getJSONValueByName(response, Integer.class, "connection_status"));
		values.put("deviceErrors", JSONUtils.getJSONValueByName(response, String.class, "device_errors"));
		values.put("boilerErrors", JSONUtils.getJSONValueByName(response, String.class, "boiler_errors"));
		values.put("dbgOutsideTemp", JSONUtils.getJSONValueByName(response, BigDecimal.class, "dbg_outside_temp"));
		values.put("pcbTemp", JSONUtils.getJSONValueByName(response, BigDecimal.class, "pcb_temp"));
		values.put("dhwWaterTemp", JSONUtils.getJSONValueByName(response, BigDecimal.class, "dhw_water_temp"));
		values.put("dhwWaterPres", JSONUtils.getJSONValueByName(response, BigDecimal.class, "dhw_water_pres"));
		values.put("boilerStatus", JSONUtils.getJSONValueByName(response, Integer.class, "boiler_status"));
		values.put("boilerConfig", JSONUtils.getJSONValueByName(response, Integer.class, "boiler_config"));
		values.put("chTimeToTemp", JSONUtils.getJSONValueByName(response, Integer.class, "ch_time_to_temp"));
		values.put("powerCons", JSONUtils.getJSONValueByName(response, Integer.class, "power_cons"));
		values.put("rssi", JSONUtils.getJSONValueByName(response, Integer.class, "rssi"));
		values.put("current", JSONUtils.getJSONValueByName(response, Integer.class, "current"));
		values.put("voltage", JSONUtils.getJSONValueByName(response, Integer.class, "voltage"));
		values.put("resets", JSONUtils.getJSONValueByName(response, Integer.class, "resets"));
		values.put("memoryAllocation", JSONUtils.getJSONValueByName(response, Integer.class, "memory_allocation"));
		values.put("chStatus", JSONUtils.getJSONValueByName(response, Integer.class, "ch_status"));
		values.put("chControl_mode", JSONUtils.getJSONValueByName(response, Integer.class, "ch_control_mode"));
		values.put("chMode", JSONUtils.getJSONValueByName(response, Integer.class, "ch_mode"));
		values.put("chModeDuration", JSONUtils.getJSONValueByName(response, BigDecimal.class, "ch_mode_duration"));
		values.put("chModeTemp", JSONUtils.getJSONValueByName(response, BigDecimal.class, "ch_mode_temp"));
		values.put("dhwStatus", JSONUtils.getJSONValueByName(response, Integer.class, "dhw_status"));
		values.put("dhwMode", JSONUtils.getJSONValueByName(response, Integer.class, "dhw_mode"));
		values.put("weatherTemp", JSONUtils.getJSONValueByName(response, BigDecimal.class, "weather_temp"));
		values.put("weatherStatus", JSONUtils.getJSONValueByName(response, Integer.class, "weather_status"));
		values.put("vacationDuration", JSONUtils.getJSONValueByName(response, Integer.class, "vacation_duration"));
		values.put("extendDuration", JSONUtils.getJSONValueByName(response, Integer.class, "extend_duration"));
		values.put("fireplaceDuration", JSONUtils.getJSONValueByName(response, Integer.class, "fireplace_duration"));

		// Todo: get "flameStatus" from chStatus / chMode?

		return values;

	}

	/**
	 * Search for thermostat in the local network.
	 *
	 * @return Info about the thermostat found, or null when noting found
	 */
	@Nullable
	protected AtagOneInfo searchOnes() throws IOException, InterruptedException {
		final UdpMessage udpMessage = NetworkUtils
			.getUdpBroadcastMessage(UDP_BROADCAST_PORT, MAX_LISTEN_TIMEOUT_SECONDS, "ONE ", NetworkUtils.MAX_CONNECTION_RETRIES);

		if (udpMessage != null && udpMessage.getMessage().startsWith("ONE ")) {
			String deviceId = udpMessage.getMessage().split(" ")[1];
			return new AtagOneInfo(udpMessage.getDeviceAddress(), deviceId);
		}

		// No thermostat found.
		return null;
	}

	/**
	 * Start authorization proces with thermostat.
	 */
	protected void authorizeWithThermostat() throws IOException, AtagPageErrorException, InterruptedException, AccessDeniedException {

		if (selectedDevice == null) {
			throw new IllegalArgumentException("No device selected, cannot authorize with thermostat.");
		}

		if (computerInfo == null) {
			throw new IllegalArgumentException("Cannot determine MAC address of computer, authorization process cancelled.");
		}

		final String pairUrl = "http://" + selectedDevice.getDeviceAddress().getHostAddress() + ":10000/pair_message";
		log.fine("POST pair_message: URL=" + pairUrl);

		// Get the local (short) hostname.
		String shortName = computerInfo.getName();
		if (shortName.contains(".")) {
			shortName = shortName.split("\\.")[0];
		}

		String macAddress = computerInfo.getMac();
		String deviceName = shortName + " " + AtagOneApp.EXECUTABLE_NAME + " API";

		// HTTP(S) Connect.
		String jsonPayload = "{\"pair_message\":{\"seqnr\":0,\"accounts\":" +
			"{\"entries\":[{" +
			"\"user_account\":\"\"," +
			"\"mac_address\":\"" + macAddress + "\"," +
			"\"device_name\":\"" + deviceName + "\"," +
			"\"account_type\":0}]}}}";

		// 1 = Pending
		// 2 = Accepted
		// 3 = Denied
		Integer accStatus = null;
		for (int i = 0; i < MAX_AUTH_RETRIES; i++) {

			// { "pair_reply":{ "seqnr":0,"acc_status":1} }
			String response = NetworkUtils.getPostPageContent(pairUrl, jsonPayload, NetworkUtils.MAX_CONNECTION_RETRIES);
			log.fine("POST pair_message Response\n" + response);

			accStatus = JSONUtils.getJSONValueByName(response, Integer.class, "acc_status");
			if (accStatus == null) {
				throw new IllegalStateException("Error during pair request. 'acc_status' is null.");
			}
			// Wait and try again within x seconds.
			if (accStatus != 2) {
				System.out.println("Access not yet granted. Please press the Ok button on the '" + deviceName + "' to grant access. \n" +
					"By pressing the Ok button you prove that you have physical access to the thermostat. \n" +
					"This is only a one time action per device that wants to connect.");
				Thread.sleep(SLEEP_BETWEEN_AUTH_REQUESTS_MS);
			} else {
				break;
			}
		}

		if (accStatus == 1) {
			throw new IllegalStateException("Please grant access to connect to the " + AtagOneApp.THERMOSTAT_NAME + " thermostat. \n" +
				"This is only a one time action per device that wants to connect.");
		}
		if (accStatus == 3) {
			throw new AccessDeniedException("Access to the " + AtagOneApp.THERMOSTAT_NAME + " thermostat is denied.");
		}
	}
}
