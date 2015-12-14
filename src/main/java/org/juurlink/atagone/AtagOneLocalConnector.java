package org.juurlink.atagone;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.juurlink.atagone.domain.Configuration;
import org.juurlink.atagone.domain.DeviceInfo;
import org.juurlink.atagone.domain.OneInfo;
import org.juurlink.atagone.exceptions.AccessDeniedException;
import org.juurlink.atagone.exceptions.AtagPageErrorException;
import org.juurlink.atagone.exceptions.AtagSearchErrorException;
import org.juurlink.atagone.utils.CalendarUtils;
import org.juurlink.atagone.utils.HTTPUtils;
import org.juurlink.atagone.utils.IOUtils;
import org.juurlink.atagone.utils.JSONUtils;
import org.juurlink.atagone.utils.StringUtils;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

/**
 * Connect to ATAG One thermostat in local network.
 */
@Log
public class AtagOneLocalConnector implements AtagOneConnectorInterface {

	/**
	 * Connection timeout in milliseconds.
	 */
	private static final int MAX_CONNECTION_TIMEOUT = 30000;
	/**
	 * Max number of connection retries. Sometimes a request result in "Connection Error: Unexpected end of file from server".
	 */
	private static final int MAX_CONNECTION_RETRIES = 3;
	/**
	 * Time between retries in milliseconds.
	 */
	private static final int MAX_TIME_BETWEEN_RETRIES = 2000;
	/**
	 * Max number of times to wait for thermostat authorization.
	 */
	private static final int MAX_AUTH_RETRIES = 15;

	private static final int MESSAGE_INFO_CONTROL = 1;
	//private static final int MESSAGE_INFO_SCHEDULES = 2;
	//private static final int MESSAGE_INFO_CONFIGURATION = 4;
	private static final int MESSAGE_INFO_REPORT = 8;
	//private static final int MESSAGE_INFO_STATUS = 16;
	//private static final int MESSAGE_INFO_WIFISCAN = 32;

	private final Configuration configuration;

	/**
	 * ATAG One device ID.
	 */
	@NonNull
	private String selectedDeviceId;

	/**
	 * ATAG One IP address.
	 */
	@Nullable
	private InetAddress selectedDeviceAddress;

	/**
	 * * Construct ATAG One connector to local device.
	 */
	public AtagOneLocalConnector(@Nonnull @NonNull final Configuration configuration) {
		log.fine("Instantiate " + AtagOneApp.THERMOSTAT_NAME + " local connector");
		this.configuration = configuration;
	}

	/**
	 * Find the thermostat in the local network.
	 */
	@Nonnull
	@Override
	@SneakyThrows
	public String login() throws IOException, AtagPageErrorException, AtagSearchErrorException {
		log.fine("No email address set, Try to find the " + AtagOneApp.THERMOSTAT_NAME + " in the local network.");
		OneInfo oneInfo = searchOnes(MAX_CONNECTION_RETRIES);
		if (oneInfo == null) {
			throw new AtagSearchErrorException("Cannot find " + AtagOneApp.THERMOSTAT_NAME + " thermostat in local network.");
		}

		// Device found in local network.
		selectedDeviceAddress = oneInfo.getDeviceAddress();
		selectedDeviceId = oneInfo.getDeviceId();

		return selectedDeviceId;
	}

	@Nonnull
	@Override
	public BigDecimal setTemperature() {
		log.fine("Set target temperature to " + configuration.getTemperature() + " degrees celsius");
		throw new RuntimeException("'setTemperature' method not yet implemented for thermostat in local network.");
	}

	@Nonnull
	@Override
	public Map<String, Object> getDiagnostics() throws InterruptedException, AccessDeniedException, AtagPageErrorException, IOException {
		return getDiagnostics(MAX_CONNECTION_RETRIES);
	}

	/**
	 * Get all diagnostics for selected device.
	 *
	 * @param maxRetries Max number of retries
	 * @return Map of diagnostic info
	 * @throws IOException              in case of connection error
	 * @throws IllegalArgumentException when no device selected
	 */
	protected Map<String, Object> getDiagnostics(int maxRetries)
		throws IOException, IllegalArgumentException, AtagPageErrorException, InterruptedException, AccessDeniedException {

		if (selectedDeviceAddress == null) {
			throw new IllegalArgumentException("No device selected to connect to.");
		}

		if (StringUtils.isBlank(selectedDeviceId)) {
			throw new IllegalArgumentException("No Device selected, cannot get diagnostics.");
		}

		final String pairUrl = "http://" + selectedDeviceAddress.getHostAddress() + ":10000/pair_message";
		log.fine("POST pair_message: URL=" + pairUrl);

		// Get the local (short) hostname.
		final DeviceInfo deviceInfo = HTTPUtils.getDeviceInfo();
		String shortName = deviceInfo.getName();
		if (shortName.contains(".")) {
			shortName = shortName.split("\\.")[0];
		}

		String macAddress = deviceInfo.getMac();
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
			String response = HTTPUtils.getPostPageContent(pairUrl, jsonPayload, maxRetries);
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
				Thread.sleep(5000);
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

		// {"retrieve_message":{"seqnr":0,"account_auth":{"user_account":"atag@juurlink.org","mac_address":"6C:40:08:B6:E2:80"},"info":15}}
		final int info = MESSAGE_INFO_CONTROL + MESSAGE_INFO_REPORT;
		jsonPayload = "{\"retrieve_message\":{" +
			"\"seqnr\":0," +
			"\"account_auth\":{" +
			"\"user_account\":\"\"," +
			"\"mac_address\":\"" + macAddress + "\"}," +
			"\"info\":" + info + "}}\n";

		String response = HTTPUtils.getPostPageContent(pairUrl, jsonPayload, maxRetries);
		log.fine("POST retrieve_message Response\n" + response);

		/*
		{ "retrieve_reply":{ "seqnr":0,

		"status":{
	x	"device_id":"6808-1401-3109_15-30-001-544",
	x	"device_status":16385,
	x	"connection_status":23,
		"date_time":503187998},

		"report":{
	x	"report_time":503187998,
	x	"burning_hours":257.09,
	x	"device_errors":"",
	x	"boiler_errors":"",
	x	"room_temp":20.6,
	x	"outside_temp":5.1,
	x	"dbg_outside_temp":22.3,
	x	"pcb_temp":25.0,
	x	"ch_setpoint":28.1,
	x	"dhw_water_temp":33.6,
	x	"ch_water_temp":32.8,
	x	"dhw_water_pres":0.0,
	x	"ch_water_pres":1.5,
	x	"ch_return_temp":33.2,
	x	"boiler_status":770,
	x	"boiler_config":772,
	x	"ch_time_to_temp":0,
	x	"shown_set_temp":20.5,
	x	"power_cons":0,
	x	"rssi":26,
	x	"current":-155,
	x	"voltage":3846,
	x	"resets":11,
	x	"memory_allocation":2800},

		"control": {
	x	"ch_status":13,
	x	"ch_control_mode":0,
	x	"ch_mode":1,
	x	"ch_mode_duration":0,
	x	"ch_mode_temp":20.5,
	x	"dhw_temp_setp":60.0,
	x	"dhw_status":5,
	x	"dhw_mode":1,
	x	"dhw_mode_temp":60.0,
	x	"weather_temp":5.1,
	x	"weather_status":9,
	x	"vacation_duration":0,
	x	"extend_duration":0,
	x	"fireplace_duration":10800
		} ,
		"acc_status":2} }
		 */

		// Scrape values from HTML page.
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		values.put(VALUE_DEVICE_ID, selectedDeviceId);
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
	 * @param maxRetries Max number of retries after connection failure
	 * @return Info about the thermostat found, or null when noting found
	 */
	@Nullable
	protected OneInfo searchOnes(int maxRetries) throws IOException, InterruptedException {

		if (maxRetries < 0) {
			throw new IllegalArgumentException("'maxRetries' value cannot be smaller than zero.");
		}

		// Todo: Move HTTP related methods to utility classes.

		final int PORT = 11000;

		// Listen to all UDP packets send to port 11,000.
		DatagramSocket datagramSocket = null;

		try {
			datagramSocket = new DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"));
			datagramSocket.setBroadcast(true);
			datagramSocket.setSoTimeout(MAX_CONNECTION_TIMEOUT);

			// Receive.
			byte[] receiveData = new byte[37];
			final DatagramPacket datagramPacket = new DatagramPacket(receiveData, receiveData.length);
			datagramSocket.receive(datagramPacket);

			final InetAddress oneInetAddress = datagramPacket.getAddress();
			final String receivedMessage = new String(datagramPacket.getData(), "UTF-8");

			if (receivedMessage.startsWith("ONE ")) {
				String deviceId = receivedMessage.split(" ")[1];
				return new OneInfo(oneInetAddress, deviceId);
			}
			return null;

		} catch (IOException e) {

			// Retry another time.
			if (maxRetries > 0) {
				log.fine(e.toString());
				log.fine("But " + maxRetries + " retr" + (maxRetries > 1 ? "ies" : "y") + " to go, try again.");

				maxRetries--;
				Thread.sleep(MAX_TIME_BETWEEN_RETRIES);
				return searchOnes(maxRetries);
			}
			// Connection failure.
			throw e;

		} finally {
			if (datagramSocket != null) {
				datagramSocket.disconnect();
				IOUtils.closeQuietly(datagramSocket);
			}
		}
	}

}
