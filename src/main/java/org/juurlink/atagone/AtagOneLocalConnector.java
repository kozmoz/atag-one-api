package org.juurlink.atagone;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.juurlink.atagone.domain.AtagOneInfo;
import org.juurlink.atagone.domain.Configuration;
import org.juurlink.atagone.domain.DeviceInfo;
import org.juurlink.atagone.domain.UdpMessage;
import org.juurlink.atagone.domain.Version;
import org.juurlink.atagone.exceptions.AccessDeniedException;
import org.juurlink.atagone.exceptions.AtagSearchErrorException;
import org.juurlink.atagone.exceptions.NotauthorizedException;
import org.juurlink.atagone.utils.CalendarUtils;
import org.juurlink.atagone.utils.JSONUtils;
import org.juurlink.atagone.utils.NetworkUtils;
import org.juurlink.atagone.utils.NetworkUtils.PageContent;
import org.juurlink.atagone.utils.NumberUtils;
import org.juurlink.atagone.utils.StringUtils;

import lombok.Getter;
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
    private static final int MESSAGE_INFO_SCHEDULES = 2;
    private static final int MESSAGE_INFO_CONFIGURATION = 4;
    private static final int MESSAGE_INFO_REPORT = 8;
    private static final int MESSAGE_INFO_STATUS = 16;
    private static final int MESSAGE_INFO_WIFISCAN = 32;

    /**
     * UDP port the thermostat sends its messages to.
     */
    private static final int UDP_BROADCAST_PORT = 11000;
    /**
     * Client port the thermostat listens on.
     */
    private static final int HTTP_CLIENT_PORT = 10000;
    private static final int SLEEP_BETWEEN_FAILURE_MS = 2000;

    private static final String RESPONSE_ACC_STATUS = "acc_status";

    /**
     * Hostname and MAC address of running machine.
     */
    private final DeviceInfo computerInfo;

    /**
     * API version info.
     */
    @Nullable
    private final Version versionInfo;

    /**
     * ATAG One Device ID and IP address. Will have a value when thermostat found.
     */
    @Getter
    @Nullable
    private AtagOneInfo selectedDevice;

    /**
     * When true, then skip the auth request during login.
     */
    private boolean skipAuthRequest;

    /**
     * Construct ATAG One connector.
     *
     * @throws IOException when error getting local device address
     */
    @SuppressWarnings("unused")
    public AtagOneLocalConnector() throws IOException {
        // With empty configuration.
        this(Configuration.builder().build());
    }

    /**
     * Construct ATAG One connector.
     *
     * @param configuration Configuration for device host-name
     * @throws java.net.UnknownHostException when configures host-name is invalid
     * @throws IOException                   when error getting local device address
     */
    @SuppressWarnings("unused")
    public AtagOneLocalConnector(final @Nonnull @NonNull Configuration configuration) throws IOException {
        log.fine("Instantiate " + AtagOneApp.THERMOSTAT_NAME + " local connector");

        versionInfo = configuration.getVersion();

        // Skip auth request?
        skipAuthRequest = configuration.isSkipAuthRequest();

        // Host-name for thermostat configured?
        final String hostName = configuration.getHostName();
        if (StringUtils.isNotBlank(hostName)) {

            // The host-name is set, so we can skip discovery during login process.
            final InetAddress deviceAddress = InetAddress.getByName(hostName);
            selectedDevice = AtagOneInfo.builder().deviceAddress(deviceAddress).build();
        }

        // Local computer MAC address (used for communication with thermostat).
        DeviceInfo deviceInfo = NetworkUtils.getDeviceInfo();
        if (StringUtils.isNotBlank(configuration.getMac())) {
            // Override MAC address with configured mac.
            deviceInfo = DeviceInfo.builder()
                .ip(deviceInfo.getIp())
                .name(deviceInfo.getName())
                .mac(configuration.getMac())
                .build();
        }
        computerInfo = deviceInfo;
    }

    /**
     * Find the thermostat in the local network and get authorization to connect to it.
     */
    @Override
    @SneakyThrows
    public void login() throws IOException {

        if (selectedDevice == null) {
            selectedDevice = searchOnes();
            if (selectedDevice == null) {
                throw new AtagSearchErrorException("Cannot find " + AtagOneApp.THERMOSTAT_NAME + " thermostat in local network.");
            }

        } else {
            log.fine("Connect to configured " + AtagOneApp.THERMOSTAT_NAME + " in the local network.");
            log.fine("Thermostat address is: " + selectedDevice.getDeviceAddress().getHostAddress());
        }

        if (!skipAuthRequest) {
            // Start authorization proces.
            requestAuthorizationFromThermostat();
        } else {
            log.fine("Skip authorization process as requested.");
        }
    }

    /**
     * Set temperature.
     *
     * @param targetTemperature Target temperature, between 4 and 27 (inclusive) and round by a half
     * @return Current room temperature
     */
    @Override
    @Nullable
    public BigDecimal setTemperature(final BigDecimal targetTemperature) throws IOException {

        if (selectedDevice == null) {
            throw new IllegalArgumentException("No device selected, cannot set temperature.");
        }

        if (computerInfo == null) {
            throw new IllegalArgumentException("Cannot determine MAC address of computer, cannot set temperature.");
        }

        // Discard the precision and round by half.
        float roundedTemperature = NumberUtils.roundHalf(targetTemperature.floatValue());
        if (roundedTemperature < AtagOneApp.TEMPERATURE_MIN || roundedTemperature > AtagOneApp.TEMPERATURE_MAX) {
            throw new IllegalArgumentException(
                "Device temperature out of bounds: " + roundedTemperature + ". Needs to be between " + AtagOneApp.TEMPERATURE_MIN +
                    " (inclusive) and " + AtagOneApp.TEMPERATURE_MAX + " (inclusive)");
        }

        final String messageUrl = "http://" + selectedDevice.getDeviceAddress().getHostAddress() + ":" + HTTP_CLIENT_PORT + "/update";
        log.fine("POST retrieve: URL=" + messageUrl);

        // Get computer MAC address.
        final String macAddress = computerInfo.getMac();

        final String jsonPayload = "{\"update_message\":{" +
            "\"seqnr\":0," +
            "\"account_auth\":{" +
            "\"user_account\":\"\"," +
            "\"mac_address\":\"" + macAddress + "\"}," +
            "\"control\":{" +
            "\"ch_mode_temp\":" + roundedTemperature +
            "}}}\n";
        String response = executeRequest(messageUrl, jsonPayload, versionInfo).getContent();

        // Response:
        // { "update_reply":{ "seqnr":0,"status":{"device_id":"6808-1401-3109_15-30-001-123","device_status":16385,"connection_status":23,"date_time":503527795},"acc_status":2} }
        final Integer accStatus = JSONUtils.getJSONValueByName(response, Integer.class, RESPONSE_ACC_STATUS);
        assertAuthorized(accStatus);

        // Update Device ID?
        updateSelectedDevice(response);

        // Get and return current room temperature.
        return (BigDecimal) getDiagnostics().get(VALUE_ROOM_TEMPERATURE);
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
        throws IOException, IllegalArgumentException {

        if (selectedDevice == null) {
            throw new IllegalArgumentException("No device selected, cannot get diagnostics.");
        }

        if (computerInfo == null) {
            throw new IllegalArgumentException("Cannot determine MAC address of computer, cannot get diagnostics.");
        }

        final String messageUrl = "http://" + selectedDevice.getDeviceAddress().getHostAddress() + ":" + HTTP_CLIENT_PORT + "/retrieve";
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

        // Sometimes the response is empty, try multiple times.
        final PageContent pageContent = executeRequest(messageUrl, jsonPayload, versionInfo);
        final String response = pageContent.getContent();

        // Try to get Atag ONE device version from response header.
        final List<String> versionHeaders = pageContent.getHeaders().get("X-One-Ver");
        final String atagOneVersion = versionHeaders != null && versionHeaders.size() > 0 ? versionHeaders.get(0) : "Unknown";

        // Test accStatus response.
        final Integer accStatus = JSONUtils.getJSONValueByName(response, Integer.class, RESPONSE_ACC_STATUS);
        assertAuthorized(accStatus);

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

        values.put(VALUE_DEVICE_IP, selectedDevice.getDeviceAddress().getHostAddress());
        values.put(VALUE_DEVICE_ID, JSONUtils.getJSONValueByName(response, String.class, "device_id"));
        // VALUE_DEVICE_ALIAS; Locally unknown
        final Integer reportTime = JSONUtils.getJSONValueByName(response, Integer.class, "report_time");

        if (reportTime != null) {
            final LocalDateTime dateObject = CalendarUtils.toDateObject(reportTime);
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
        final Integer boilerStatus = JSONUtils.getJSONValueByName(response, Integer.class, "boiler_status");
        values.put("deviceStatus", JSONUtils.getJSONValueByName(response, Integer.class, "device_status"));
        values.put("connectionStatus", JSONUtils.getJSONValueByName(response, Integer.class, "connection_status"));
        values.put("deviceErrors", JSONUtils.getJSONValueByName(response, String.class, "device_errors"));
        values.put("boilerErrors", JSONUtils.getJSONValueByName(response, String.class, "boiler_errors"));
        values.put("dbgOutsideTemp", JSONUtils.getJSONValueByName(response, BigDecimal.class, "dbg_outside_temp"));
        values.put("pcbTemp", JSONUtils.getJSONValueByName(response, BigDecimal.class, "pcb_temp"));
        values.put("dhwWaterTemp", JSONUtils.getJSONValueByName(response, BigDecimal.class, "dhw_water_temp"));
        values.put("dhwWaterPres", JSONUtils.getJSONValueByName(response, BigDecimal.class, "dhw_water_pres"));
        values.put("boilerStatus", boilerStatus);
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

        // Get "flameStatus" from boilerStatus bit 3.
        if (boilerStatus != null) {
            values.put("flameStatus", (boilerStatus & 8) == 8 ? "On" : "Off");
        }

        values.put("atagOneVersion", atagOneVersion);
        values.put("macAddress", macAddress);

        // Update Device ID?
        updateSelectedDevice(response);

        return values;
    }

    /**
     * Get all info from the thermostat and dump the response.
     *
     * @return String  Raw response from thermostat
     * @throws IOException              in case of connection error
     * @throws IllegalArgumentException when no device selected
     */
    @Nonnull
    @Override
    public String dump() throws IOException, IllegalArgumentException {

        if (selectedDevice == null) {
            throw new IllegalArgumentException("No device selected, cannot get diagnostics.");
        }

        if (computerInfo == null) {
            throw new IllegalArgumentException("Cannot determine MAC address of computer, cannot get dump.");
        }

        final String messageUrl = "http://" + selectedDevice.getDeviceAddress().getHostAddress() + ":" + HTTP_CLIENT_PORT + "/retrieve";
        log.fine("POST retrieve: URL=" + messageUrl);

        // Get computer MAC address.
        final String macAddress = computerInfo.getMac();

        final int info = MESSAGE_INFO_CONTROL + MESSAGE_INFO_SCHEDULES + MESSAGE_INFO_CONFIGURATION + MESSAGE_INFO_REPORT + MESSAGE_INFO_STATUS +
            MESSAGE_INFO_WIFISCAN;
        final String jsonPayload = "{\"retrieve_message\":{" +
            "\"seqnr\":0," +
            "\"account_auth\":{" +
            "\"user_account\":\"\"," +
            "\"mac_address\":\"" + macAddress + "\"}," +
            "\"info\":" + info + "}}\n";

        // Sometimes the response is empty, try multiple times.
        String response = executeRequest(messageUrl, jsonPayload, versionInfo).getContent();

        // Test accStatus response.
        final Integer accStatus = JSONUtils.getJSONValueByName(response, Integer.class, RESPONSE_ACC_STATUS);
        assertAuthorized(accStatus);

        // Update Device ID?
        updateSelectedDevice(response);

        return response;
    }

    /**
     * Search for thermostat in the local network.
     *
     * @return Info about the thermostat found, or null when noting found
     */
    @Nullable
    protected AtagOneInfo searchOnes() throws IOException {

        log.fine("Try to find the " + AtagOneApp.THERMOSTAT_NAME + " in the local network for " + MAX_LISTEN_TIMEOUT_SECONDS + " seconds.");

        final String messageTag = "ONE ";

        UdpMessage udpMessage = null;
        int maxRetriesAfterTechnicalError = 3;
        while (maxRetriesAfterTechnicalError > 0) {
            try {
                udpMessage = NetworkUtils.getUdpBroadcastMessage(UDP_BROADCAST_PORT, MAX_LISTEN_TIMEOUT_SECONDS, messageTag);

                log.finest("UDP message successful received: " + udpMessage);

                // No technical errors occurred, stop listening.
                break;

            } catch (IOException e) {
                maxRetriesAfterTechnicalError--;
                if (maxRetriesAfterTechnicalError <= 0) {
                    throw e;
                }

                log.fine("Error receiving UDP message: " + e.getMessage() + ". \nRetry " + maxRetriesAfterTechnicalError + " more times.");
            }
        }

        if (udpMessage != null) {
            // We received a message that matches our tag.
            String deviceId = udpMessage.getMessage().split(" ")[1];

            final AtagOneInfo deviceFound = AtagOneInfo.builder()
                .deviceAddress(udpMessage.getSenderAddress())
                .deviceId(deviceId)
                .build();

            // Device found in local network.
            log.fine(AtagOneApp.THERMOSTAT_NAME + " found in local network: " + deviceFound);

            return deviceFound;
        }

        // No thermostat found.
        return null;
    }

    /**
     * Start authorization proces; request permission.
     */
    @SneakyThrows(InterruptedException.class)
    protected void requestAuthorizationFromThermostat() throws IOException {

        if (selectedDevice == null) {
            throw new IllegalArgumentException("No device selected, cannot request authorization.");
        }

        if (computerInfo == null) {
            throw new IllegalArgumentException("Cannot determine MAC address of computer, authorization process cancelled.");
        }

        final String pairUrl = "http://" + selectedDevice.getDeviceAddress().getHostAddress() + ":" + HTTP_CLIENT_PORT + "/pair_message";
        log.fine("POST pair_message: URL=" + pairUrl);

        // Get the local (short) hostname.
        String shortName = computerInfo.getName();
        if (shortName.contains(".")) {
            shortName = shortName.split("\\.")[0];
        }

        String macAddress = computerInfo.getMac();
        String deviceName = shortName + " " + AtagOneApp.EXECUTABLE_NAME + " API";

        String jsonPayload = "{\"pair_message\":{\"seqnr\":0," +
            "\"account_auth\":{" +
            "\"user_account\":\"\"," +
            "\"mac_address\":\"" + macAddress + "\"}," +
            "\"accounts\":" +
            "{\"entries\":[{" +
            "\"user_account\":\"\"," +
            "\"mac_address\":\"" + macAddress + "\"," +
            "\"device_name\":\"" + deviceName + "\"," +
            "\"account_type\":0}]}}}";

        log.finest("POST payload:\n" + jsonPayload);

        // 1 = Pending
        // 2 = Accepted
        // 3 = Denied
        Integer accStatus = null;
        for (int i = 0; i < MAX_AUTH_RETRIES; i++) {

            // Sometimes the response is empty, try multiple times.
            String response = executeRequest(pairUrl, jsonPayload, versionInfo).getContent();

            accStatus = JSONUtils.getJSONValueByName(response, Integer.class, RESPONSE_ACC_STATUS);
            if (accStatus == null) {
                throw new IllegalStateException("Error during pair request. '" + RESPONSE_ACC_STATUS + "' is null.");
            }
            // Wait and try again within x seconds.
            if (accStatus != 2) {
                System.out.println("Access not yet granted. Please press the Yes button on the '" + deviceName + "' to grant access. \n" +
                    "By pressing the Yes button you prove that you have physical access to the thermostat. \n" +
                    "This is only an one time action per device.");
                Thread.sleep(SLEEP_BETWEEN_AUTH_REQUESTS_MS);
            } else {
                break;
            }
        }

        // Test accStatus response.
        assertAuthorized(accStatus);

        log.fine("Access granted; accStatus == " + accStatus);
    }

    /**
     * Execute request, in case of empty response or connection error, try multiple times.
     *
     * @param url         URL to connect
     * @param jsonPayload Payload JSON message
     * @param versionInfo Version info, will be included in request header
     * @return Response
     */
    @Nonnull
    @SneakyThrows(InterruptedException.class)
    protected PageContent executeRequest(final String url, final String jsonPayload, final Version versionInfo) throws IOException {

        // Create version string for header.
        final String versionString = versionInfo != null ? versionInfo.toString() : "";

        // Sometimes the response is empty, try multiple times.
        int maxRetries = 10;
        PageContent response = null;
        while (response == null && maxRetries > 0) {
            maxRetries--;
            try {
                PageContent pageContent = NetworkUtils.getPostPageContent(url, jsonPayload, versionString);
                log.fine("POST Response\n" + pageContent);

                if (StringUtils.isNotBlank(pageContent.getContent())) {
                    response = pageContent;
                }

            } catch (IOException e) {
                if (maxRetries > 0) {
                    log.fine(e.toString());
                } else {
                    // Tried n times.
                    throw e;
                }
            }

            if (response == null && maxRetries > 0) {
                log.fine("Empty response, try again.");
                Thread.sleep(SLEEP_BETWEEN_FAILURE_MS);
            }
        }

        // Cannot happen, just for sure.
        if (response == null) {
            throw new IllegalStateException("Empty response");
        }
        return response;
    }

    /**
     * Test response for authorization errors.
     *
     * @param accStatus accStatus from response
     * @throws NotauthorizedException When user did not approve authorization request
     * @throws AccessDeniedException  When user denied authorization request
     */
    protected void assertAuthorized(@Nullable
    final Integer accStatus) throws NotauthorizedException, AccessDeniedException {

        if (accStatus == null) {
            throw new IllegalStateException("Response '" + RESPONSE_ACC_STATUS + "' is null.");
        }

        // Access granted.
        if (accStatus == 2) {
            return;
        }

        // User did not approve authorization request.
        if (accStatus == 1) {
            throw new NotauthorizedException("Please grant access to connect to the " + AtagOneApp.THERMOSTAT_NAME + " thermostat. \n" +
                "This is only a one time action per device that wants to connect.");
        }

        // User denied authorization request.
        if (accStatus == 3) {
            throw new AccessDeniedException("Access to the " + AtagOneApp.THERMOSTAT_NAME + " thermostat is denied.");
        }

        throw new IllegalStateException("Unknown '" + RESPONSE_ACC_STATUS + "', expecting 1, 2 or 3, but is " + accStatus + ".");
    }

    /**
     * Update selected device when device id is empty.
     *
     * @param response RAW REST response
     */
    protected void updateSelectedDevice(final String response) {
        if (selectedDevice != null && StringUtils.isBlank(selectedDevice.getDeviceId())) {
            String deviceId = JSONUtils.getJSONValueByName(response, String.class, "device_id");
            selectedDevice = AtagOneInfo.builder()
                .deviceAddress(selectedDevice.getDeviceAddress())
                .deviceId(deviceId)
                .build();

            log.fine("Updated DeviceID because it was empty: " + deviceId);
        }
    }

}
