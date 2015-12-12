package org.juurlink.atagone;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.juurlink.atagone.domain.Configuration;
import org.juurlink.atagone.domain.DeviceInfo;
import org.juurlink.atagone.domain.FORMAT;
import org.juurlink.atagone.domain.OneInfo;
import org.juurlink.atagone.domain.Version;
import org.juurlink.atagone.exceptions.AccessDeniedException;
import org.juurlink.atagone.exceptions.AtagPageErrorException;
import org.juurlink.atagone.exceptions.AtagSearchErrorException;
import org.juurlink.atagone.utils.CalendarUtils;
import org.juurlink.atagone.utils.HTMLUtils;
import org.juurlink.atagone.utils.HTTPUtils;
import org.juurlink.atagone.utils.IOUtils;
import org.juurlink.atagone.utils.JSONUtils;
import org.juurlink.atagone.utils.NumberUtils;
import org.juurlink.atagone.utils.StringUtils;

import lombok.NonNull;
import lombok.extern.java.Log;

/**
 * ATAG ONE Portal API.
 */
@Log
public class AtagOneApp {

	private static final String URL_LOGIN = "https://portal.atag-one.com/Account/Login";
	private static final String URL_DEVICE_HOME = "https://portal.atag-one.com/Home/Index/{0}";
	private static final String URL_DIAGNOSTICS = "https://portal.atag-one.com/Device/LatestReport";
	private static final String URL_UPDATE_DEVICE_CONTROL = "https://portal.atag-one.com/Home/UpdateDeviceControl/?deviceId={0}";
	private static final String URL_DEVICE_SET_SETPOINT = "https://portal.atag-one.com/Home/DeviceSetSetpoint";

	private static final String THERMOSTAT_NAME = "ATAG One";
	private static final String EXECUTABLE_NAME = "atag-one";
	private static final String ENCODING_UTF_8 = "UTF-8";

	// Command line options.
	private static final String OPTION_EMAIL = "email";
	private static final String OPTION_PASSWORD = "password";
	private static final String OPTION_HELP = "help";
	private static final String OPTION_DEBUG = "debug";
	private static final String OPTION_OUTPUT = "output";
	private static final String OPTION_SET = "set";
	private static final String OPTION_VERSION = "version";

	// Result map keys.
	private static final String VALUE_DEVICE_ID = "deviceId";
	private static final String VALUE_DEVICE_ALIAS = "deviceAlias";
	private static final String VALUE_LATEST_REPORT_TIME = "latestReportTime";
	private static final String VALUE_CONNECTED_TO = "connectedTo";
	private static final String VALUE_BURNING_HOURS = "burningHours";
	private static final String VALUE_BOILER_HEATING_FOR = "boilerHeatingFor";
	private static final String VALUE_FLAME_STATUS = "flameStatus";
	private static final String VALUE_ROOM_TEMPERATURE = "roomTemperature";
	private static final String VALUE_OUTSIDE_TEMPERATURE = "outsideTemperature";
	private static final String VALUE_DHW_SETPOINT = "dhwSetpoint";
	private static final String VALUE_DHW_WATER_TEMPERATURE = "dhwWaterTemperature";
	private static final String VALUE_CH_SETPOINT = "chSetpoint";
	private static final String VALUE_CH_WATER_TEMPERATURE = "chWaterTemperature";
	private static final String VALUE_CH_WATER_PRESSURE = "chWaterPressure";
	private static final String VALUE_CH_RETURN_TEMPERATURE = "chReturnTemperature";
	private static final String VALUE_TARGET_TEMPERATURE = "targetTemperature";
	private static final String VALUE_CURRENT_MODE = "currentMode";
	private static final String VALUE_VACATION_PLANNED = "vacationPlanned";

	// Variable names in JSON responses.
	private static final String JSON_ROOM_TEMP = "room_temp";

	private static final int TEMPERATURE_MAX = 30;
	private static final int TEMPERATURE_MIN = 4;

	private static final String PROPERTY_NAME_MAVEN_APPLICATION_VERSION = "applicationVersion";
	private static final String PROPERTY_NAME_MAVEN_BUILD_DATE = "buildDate";
	private static final String META_INF_MANIFEST_MF = "/META-INF/MANIFEST.MF";

	private static final int MAX_THERMOSTAT_AUTH_RETRIES = 10;

	private static final int MESSAGE_INFO_CONTROL = 1;
	private static final int MESSAGE_INFO_SCHEDULES = 2;
	private static final int MESSAGE_INFO_CONFIGURATION = 4;
	private static final int MESSAGE_INFO_REPORT = 8;
	private static final int MESSAGE_INFO_STATUS = 16;
	private static final int MESSAGE_INFO_WIFISCAN = 32;

	@NonNull
	private Configuration configuration;

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

	private boolean selectedDeviceIsLocal;

	/**
	 * Create new instance.
	 */
	public AtagOneApp(Configuration configuration) {
		this.configuration = configuration;
		// Configure default in-memory cookie store.
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
	}

	/**
	 * Application start point.
	 */
	public static void main(String[] args) throws Exception {

		// Determine what to do.
		Configuration configuration = validateAndParseCommandLine(args);

		// Show debugging info?
		if (configuration.isDebug()) {
			configureLogger();
		}

		// Initialize ATAG ONE Portal connector.
		AtagOneApp atagOneApp = new AtagOneApp(configuration);

		try {
			//			if (atagOneApp.selectedDeviceIsLocal) {
			atagOneApp.login();
			//			} else {
			//				atagOneApp.loginAtPortal();
			//			}

			@Nullable
			final Float temperature = configuration.getTemperature();

			if (temperature != null) {
				// Set temperature
				BigDecimal currentRoomTemperature = atagOneApp.setDeviceSetPoint(temperature);
				System.out.println(String.format(Locale.US, "%.1f", currentRoomTemperature));

			} else {
				// Get diagnostics.
				Map<String, Object> diagnoses;

				if (atagOneApp.selectedDeviceIsLocal) {
					diagnoses = atagOneApp.getLocalDiagnostics();
				} else {
					diagnoses = atagOneApp.getDiagnostics();
				}

				if (configuration.getFormat() == FORMAT.CSV) {
					// Todo: Defer to new method, with sequence of values given.
					// Print a list of CSV values.
					System.out.print(diagnoses.get(VALUE_ROOM_TEMPERATURE));
					System.out.print(" ");
					System.out.print(diagnoses.get(VALUE_OUTSIDE_TEMPERATURE));
					System.out.print(" ");
					System.out.print(diagnoses.get(VALUE_CH_WATER_PRESSURE));
					System.out.print(" ");
					System.out.print(diagnoses.get(VALUE_CH_WATER_TEMPERATURE));
					System.out.print(" ");
					System.out.print(diagnoses.get(VALUE_CH_RETURN_TEMPERATURE));
					System.out.print(" ");
					System.out.print(diagnoses.get(VALUE_TARGET_TEMPERATURE));
					System.out.print(" ");
					System.out.print(diagnoses.get(VALUE_CH_SETPOINT));
					System.out.print(" ");
					System.out.print((Boolean) diagnoses.get(VALUE_FLAME_STATUS) ? "1" : "0");
					System.out.print(" ");
					System.out.print(diagnoses.get(VALUE_BOILER_HEATING_FOR));
					System.out.print(" ");
				} else {
					// Print diagnostics as JSON and keep the sequence.
					System.out.println(JSONUtils.toJSON(diagnoses));
				}
			}
			System.out.println();

		} catch (IOException e) {
			// Print human readable error message.
			System.err.println("Connection Error: " + e.getMessage());
			System.err.println();

			System.exit(1);

		} catch (IllegalStateException e) {
			// Print human readable error message.
			System.err.println("State Error: " + e.getMessage());
			System.err.println();

			System.exit(1);

		} catch (IllegalArgumentException e) {
			// Print human readable error message.
			System.err.println("Illegal Argument: " + e.getMessage());
			System.err.println();

			System.exit(1);

		} catch (AtagPageErrorException e) {
			// Print human readable error message.
			System.err.println(e.getMessage());
			System.err.println();

			System.exit(1);

		} catch (AtagSearchErrorException e) {
			// Print human readable error message.
			System.err.println(e.getMessage());
			System.err.println();

			System.exit(1);

		} catch (Throwable e) {
			// Other technical errors..
			final String message = e.getMessage();
			if (message != null) {
				System.err.println(message);
				System.err.println();
			}
			e.printStackTrace();

			System.exit(1);
		}
	}

	/**
	 * Parse command line options and exit in case of error.
	 *
	 * @param args Command line arguments
	 * @return Configuration object with username, password and other settings
	 */
	protected static Configuration validateAndParseCommandLine(final String[] args) {

		Options options = new Options();
		options.addOption("e", OPTION_EMAIL, true,
			"User Portal email address. Setting the email address assumes getting the thermostat data from the " + THERMOSTAT_NAME + " portal.");
		options.addOption("p", OPTION_PASSWORD, true, "User Portal password.");
		options.addOption("h", OPTION_HELP, false, "Print this help message.");
		options.addOption("d", OPTION_DEBUG, false, "Print debugging information.");
		options.addOption("o", OPTION_OUTPUT, true, "Output format; json [default] or csv.");
		options.addOption("s", OPTION_SET, true,
			"Set temperature in degrees celsius between " + TEMPERATURE_MIN + " and " + TEMPERATURE_MAX + " inclusive.");
		options.addOption("v", OPTION_VERSION, false, "Version info and build timestamp.");

		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args);

			final String email = cmd.getOptionValue(OPTION_EMAIL);
			final String password = cmd.getOptionValue(OPTION_PASSWORD);
			final boolean debug = cmd.hasOption(OPTION_DEBUG);
			final String output = cmd.getOptionValue(OPTION_OUTPUT);
			final boolean hasTemperature = cmd.hasOption(OPTION_SET);
			final String temperatureString = cmd.getOptionValue(OPTION_SET);
			final boolean hasVersion = cmd.hasOption(OPTION_VERSION);
			@Nullable
			Float temperature = null;

			// Display version info.
			if (hasVersion) {
				final Version versionInfo = getVersionInfo();
				System.out.println("Version: " + versionInfo.getVersion());
				System.out.println("Build:  " + getVersionInfo().getTimestamp());
				System.out.println();
				System.exit(0);
			}

			if (!StringUtils.isBlank(email) && StringUtils.isBlank(password)) {
				System.err.println("When the email address is specified, the password is required");
				System.err.println();

				showCommandLineHelp(options);
				System.exit(1);
			}

			if (hasTemperature) {
				if (StringUtils.isBlank(temperatureString)) {
					System.err.println("No temperature specified. Please set setpoint temperature.");
					System.err.println();

					showCommandLineHelp(options);
					System.exit(1);
				}

				try {
					temperature = Float.parseFloat(temperatureString);
				} catch (NumberFormatException e) {
					System.err.println("Temperature has to be a numeric value.");
					System.err.println();

					showCommandLineHelp(options);
					System.exit(1);
				}
			}

			if (cmd.hasOption("h")) {
				showCommandLineHelp(options);
				System.exit(0);
			}

			// Determine output format (default = json).
			FORMAT outputFormat = FORMAT.JSON;
			if (!StringUtils.isBlank(output)) {
				try {
					outputFormat = FORMAT.valueOf(output.toUpperCase(Locale.US));
				} catch (IllegalArgumentException e) {
					System.err.println("Illegal output format specified '" + output + "'.");
					System.out.println("Valid formats: " + Arrays.toString(FORMAT.values()) + ".");
					System.out.println();

					showCommandLineHelp(options);
					System.exit(1);
				}
			}

			return new Configuration(temperature, email, password, debug, outputFormat, null);

		} catch (ParseException e) {

			// Print human readable error message.
			System.err.println(e.getMessage());
			System.err.println();

			showCommandLineHelp(options);
			System.exit(1);
		}

		throw new IllegalStateException("Program should have been exited");
	}

	/**
	 * Display version info and build timestamp.
	 */
	private static Version getVersionInfo() {

		String mavenApplicationVersion = "UNKNOWN";
		String mavenBuildDate = "UNKNOWN";

		try {
			Class<AtagOneApp> clazz = AtagOneApp.class;
			String className = clazz.getSimpleName() + ".class";
			String classPath = clazz.getResource(className).toString();
			if (classPath.startsWith("jar")) {

				// Class from JAR.
				String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + META_INF_MANIFEST_MF;
				Attributes attributes = new Manifest(new URL(manifestPath).openStream()).getMainAttributes();

				if (attributes.getValue(PROPERTY_NAME_MAVEN_APPLICATION_VERSION) != null &&
					attributes.getValue(PROPERTY_NAME_MAVEN_BUILD_DATE) != null) {
					mavenApplicationVersion = attributes.getValue(PROPERTY_NAME_MAVEN_APPLICATION_VERSION);
					mavenBuildDate = attributes.getValue(PROPERTY_NAME_MAVEN_BUILD_DATE);
				}
			}
		} catch (final IOException e) {
			mavenBuildDate = mavenApplicationVersion = "Read Error";
		}

		return new Version(mavenApplicationVersion, mavenBuildDate);
	}

	/**
	 * Show command line help.
	 */
	protected static void showCommandLineHelp(final Options options) {
		// Automatically generate the help statement.
		HelpFormatter formatter = new HelpFormatter();
		final String headerMessage = "Prints by default diagnostic info about the " + THERMOSTAT_NAME + " thermostat in the local network.\n\n" +
			"Optionally it can set the setpoint temperature. " +
			"It can connect to both the " + THERMOSTAT_NAME + " portal or directly to the thermostat in the local network.\n\n";
		formatter.printHelp(EXECUTABLE_NAME,
			headerMessage, options, "", true);
	}

	/**
	 * Configure logging.
	 */
	protected static void configureLogger() {

		final ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);

		// Debug HttpURLConnection class.
		Logger logger = Logger.getLogger("sun.net.www.protocol.http");
		logger.setLevel(Level.ALL);
		logger.addHandler(handler);

		// Debug this.
		logger = Logger.getLogger("org.juurlink");
		logger.setLevel(Level.ALL);
		logger.addHandler(handler);
	}

	/**
	 * Login ATAG ONE portal and select first Device found or find the thermostat in the local network.
	 */
	protected void login() throws IOException, AtagPageErrorException, AtagSearchErrorException {
		if (configuration.getEmail() != null && configuration.getDeviceAddress() != null) {
			log.fine("Email address set, login ar " + THERMOSTAT_NAME + " portal.");
			loginAtPortal();
		} else {
			if (configuration.getDeviceAddress() == null) {
				log.fine("No email address set, Try to find the " + THERMOSTAT_NAME + " in the local network.");
				OneInfo oneInfo = searchOnes();
				if (oneInfo == null) {
					throw new AtagSearchErrorException("Cannot find " + THERMOSTAT_NAME + " thermostat in local network.");
				}

				// Device found in local network.
				selectedDeviceIsLocal = true;
				selectedDeviceAddress = oneInfo.getDeviceAddress();
				selectedDeviceId = oneInfo.getDeviceId();

			} else {

				log.fine("Device address set. Try to connect to configured IP: " + configuration.getDeviceAddress());
				// Todo: is device ID required?
				// Todo: verder uitwerken...
				selectedDeviceAddress = configuration.getDeviceAddress();
				throw new RuntimeException("Not yet implemented.");
			}
		}
	}

	protected void loginAtPortal() throws IOException, AtagPageErrorException {

		log.fine("POST authentication data: " + URL_LOGIN);

		// We need a session (cookie) and a verification token, get them first.
		String requestVerificationToken = getRequestVerificationToken(URL_LOGIN);

		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put("__RequestVerificationToken", requestVerificationToken);
		params.put("Email", configuration.getEmail());
		params.put("Password", configuration.getPassword());
		params.put("RememberMe", "false");

		String html = HTTPUtils.getPostPageContent(URL_LOGIN, params);
		selectedDeviceId = HTMLUtils.extractDeviceIdFromHtml(html);

		if (StringUtils.isBlank(selectedDeviceId)) {
			throw new IllegalStateException("No Device ID found, cannot continue.");
		}
	}

	/**
	 * Get all diagnostics for selected device.
	 *
	 * @return Map of diagnostic info
	 * @throws IOException              in case of connection error
	 * @throws IllegalArgumentException when no device selected
	 */
	protected Map<String, Object> getLocalDiagnostics()
		throws IOException, IllegalArgumentException, AtagPageErrorException, InterruptedException, AccessDeniedException {

		if (selectedDeviceAddress == null) {
			throw new IllegalArgumentException("No device selected to connect to.");
		}

		if (StringUtils.isBlank(selectedDeviceId)) {
			throw new IllegalArgumentException("No Device selected, cannot get diagnostics.");
		}

		final String pairUrl = "http://" + selectedDeviceAddress.getHostAddress() + ":10000/pair_message";
		log.fine("POST pair_message: URL=" + pairUrl);

		final DeviceInfo deviceInfo = HTTPUtils.getDeviceInfo();
		String shortName = deviceInfo.getName();
		if (shortName.contains(".")) {
			shortName = shortName.split("\\.")[0];
		}

		String macAddress = deviceInfo.getMac();
		String deviceName = shortName + " " + EXECUTABLE_NAME + " API";

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
		for (int i = 0; i < MAX_THERMOSTAT_AUTH_RETRIES; i++) {

			// { "pair_reply":{ "seqnr":0,"acc_status":1} }
			String response = HTTPUtils.getPostPageContent(pairUrl, jsonPayload);
			log.fine("POST pair_message Response\n" + response);

			accStatus = JSONUtils.getJSONValueByName(response, Integer.class, "acc_status");
			if (accStatus == null) {
				throw new IllegalStateException("Error during pair request. 'acc_status' is null.");
			}
			// Wait and try again within x seconds.
			if (accStatus != 2) {
				System.out.println("Access not granted yet. Please grant access to '" + deviceName + "'.");
				Thread.sleep(5000);
			} else {
				break;
			}
		}

		if (accStatus == 1) {
			throw new IllegalStateException("Please grant access to connect to the " + THERMOSTAT_NAME + " thermostat.");
		}
		if (accStatus == 3) {
			throw new AccessDeniedException("Access to the " + THERMOSTAT_NAME + " thermostat is denied.");
		}

		// {"retrieve_message":{"seqnr":0,"account_auth":{"user_account":"atag@juurlink.org","mac_address":"6C:40:08:B6:E2:80"},"info":15}}
		final int info = MESSAGE_INFO_CONTROL + MESSAGE_INFO_REPORT;
		jsonPayload = "{\"retrieve_message\":{" +
			"\"seqnr\":0," +
			"\"account_auth\":{" +
			"\"user_account\":\"\"," +
			"\"mac_address\":\"" + macAddress + "\"}," +
			"\"info\":" + info + "}}\n";

		String response = HTTPUtils.getPostPageContent(pairUrl, jsonPayload);
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
		values.put("device_errors", JSONUtils.getJSONValueByName(response, String.class, "device_errors"));
		values.put("boiler_errors", JSONUtils.getJSONValueByName(response, String.class, "boiler_errors"));
		values.put("dbg_outside_temp", JSONUtils.getJSONValueByName(response, BigDecimal.class, "dbg_outside_temp"));
		values.put("pcb_temp", JSONUtils.getJSONValueByName(response, BigDecimal.class, "pcb_temp"));
		values.put("dhw_water_temp", JSONUtils.getJSONValueByName(response, BigDecimal.class, "dhw_water_temp"));
		values.put("dhw_water_pres", JSONUtils.getJSONValueByName(response, BigDecimal.class, "dhw_water_pres"));
		values.put("boiler_status", JSONUtils.getJSONValueByName(response, Integer.class, "boiler_status"));
		values.put("boiler_config", JSONUtils.getJSONValueByName(response, Integer.class, "boiler_config"));
		values.put("ch_time_to_temp", JSONUtils.getJSONValueByName(response, Integer.class, "ch_time_to_temp"));
		values.put("power_cons", JSONUtils.getJSONValueByName(response, Integer.class, "power_cons"));
		values.put("rssi", JSONUtils.getJSONValueByName(response, Integer.class, "rssi"));
		values.put("current", JSONUtils.getJSONValueByName(response, Integer.class, "current"));
		values.put("voltage", JSONUtils.getJSONValueByName(response, Integer.class, "voltage"));
		values.put("resets", JSONUtils.getJSONValueByName(response, Integer.class, "resets"));
		values.put("memory_allocation", JSONUtils.getJSONValueByName(response, Integer.class, "memory_allocation"));
		values.put("ch_status", JSONUtils.getJSONValueByName(response, Integer.class, "ch_status"));
		values.put("ch_control_mode", JSONUtils.getJSONValueByName(response, Integer.class, "ch_control_mode"));
		values.put("ch_mode", JSONUtils.getJSONValueByName(response, Integer.class, "ch_mode"));
		values.put("ch_mode_duration", JSONUtils.getJSONValueByName(response, BigDecimal.class, "ch_mode_duration"));
		values.put("ch_mode_temp", JSONUtils.getJSONValueByName(response, BigDecimal.class, "ch_mode_temp"));
		values.put("dhw_status", JSONUtils.getJSONValueByName(response, Integer.class, "dhw_status"));
		values.put("dhw_mode", JSONUtils.getJSONValueByName(response, Integer.class, "dhw_mode"));
		values.put("weather_temp", JSONUtils.getJSONValueByName(response, BigDecimal.class, "weather_temp"));
		values.put("weather_status", JSONUtils.getJSONValueByName(response, Integer.class, "weather_status"));
		values.put("vacation_duration", JSONUtils.getJSONValueByName(response, Integer.class, "vacation_duration"));
		values.put("extend_duration", JSONUtils.getJSONValueByName(response, Integer.class, "extend_duration"));
		values.put("fireplace_duration", JSONUtils.getJSONValueByName(response, Integer.class, "fireplace_duration"));

		return values;

	}

	/**
	 * Get all diagnostics for selected device.
	 *
	 * @return Map of diagnostic info
	 * @throws IOException              in case of connection error
	 * @throws IllegalArgumentException when no device selected
	 */
	protected Map<String, Object> getDiagnostics() throws IOException, IllegalArgumentException, AtagPageErrorException {

		if (StringUtils.isBlank(selectedDeviceId)) {
			throw new IllegalArgumentException("No Device selected, cannot get diagnostics.");
		}

		final String diagnosticsUrl = URL_DIAGNOSTICS + "?deviceId=" + URLEncoder.encode(selectedDeviceId, ENCODING_UTF_8);
		log.fine("GET diagnostics: URL=" + diagnosticsUrl);

		// HTTP(S) Connect.
		String html = HTTPUtils.getPageContent(diagnosticsUrl);
		log.fine("GET diagnostics: Response HTML\n" + html);

		// Scrape values from HTML page.
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		values.put(VALUE_DEVICE_ID, selectedDeviceId);
		values.put(VALUE_DEVICE_ALIAS, HTMLUtils.getValueByLabel(html, String.class, "Apparaat alias", "Device alias"));
		values.put(VALUE_LATEST_REPORT_TIME, HTMLUtils.getValueByLabel(html, String.class, "Laatste rapportagetijd", "Latest report time"));
		values.put(VALUE_CONNECTED_TO, HTMLUtils.getValueByLabel(html, String.class, "Verbonden met", "Connected to"));
		values.put(VALUE_BURNING_HOURS, HTMLUtils.getValueByLabel(html, BigDecimal.class, "Branduren", "Burning hours"));
		values.put(VALUE_BOILER_HEATING_FOR, HTMLUtils.getValueByLabel(html, String.class, "Ketel in bedrijf voor", "Boiler heating for"));
		values.put(VALUE_FLAME_STATUS, HTMLUtils.getValueByLabel(html, Boolean.class, "Brander status", "Flame status"));
		values.put(VALUE_ROOM_TEMPERATURE, HTMLUtils.getValueByLabel(html, BigDecimal.class, "Kamertemperatuur", "Room temperature"));
		values.put(VALUE_OUTSIDE_TEMPERATURE, HTMLUtils.getValueByLabel(html, BigDecimal.class, "Buitentemperatuur", "Outside temperature"));
		values.put(VALUE_DHW_SETPOINT, HTMLUtils.getValueByLabel(html, BigDecimal.class, "Setpoint warmwater", "DHW setpoint"));
		values.put(VALUE_DHW_WATER_TEMPERATURE, HTMLUtils.getValueByLabel(html, BigDecimal.class, "Warmwatertemperatuur", "DHW water temperature"));
		values.put(VALUE_CH_SETPOINT, HTMLUtils.getValueByLabel(html, BigDecimal.class, "Setpoint cv", "CH setpoint"));
		values.put(VALUE_CH_WATER_TEMPERATURE, HTMLUtils.getValueByLabel(html, BigDecimal.class, "CV-aanvoertemperatuur", "CH water temperature"));
		values.put(VALUE_CH_WATER_PRESSURE, HTMLUtils.getValueByLabel(html, BigDecimal.class, "CV-waterdruk", "CH water pressure"));
		values.put(VALUE_CH_RETURN_TEMPERATURE, HTMLUtils.getValueByLabel(html, BigDecimal.class, "CV retourtemperatuur", "CH return temperature"));

		// We have to do an extra call to get the target temperature.
		// {"isHeating":false,"targetTemp":"17.0","currentTemp":"16.9","vacationPlanned":false,"currentMode":"manual"}
		String deviceControlUrl = URL_UPDATE_DEVICE_CONTROL.replace("{0}", URLEncoder.encode(selectedDeviceId, ENCODING_UTF_8));
		log.fine("GET deviceControl: URL=" + deviceControlUrl);

		// HTTP(S) Connect.
		html = HTTPUtils.getPageContent(deviceControlUrl);
		log.fine("GET deviceControl: Response HTML\n" + html);

		values.put(VALUE_TARGET_TEMPERATURE, JSONUtils.getJSONValueByName(html, BigDecimal.class, "targetTemp"));
		values.put(VALUE_CURRENT_MODE, JSONUtils.getJSONValueByName(html, String.class, "currentMode"));
		values.put(VALUE_VACATION_PLANNED, JSONUtils.getJSONValueByName(html, String.class, "vacationPlanned"));

		return values;
	}

	/**
	 * Set device temperature.
	 *
	 * @param pTemperature Device SetPoint temperature
	 * @return current room temperature or null when temperature not found in response
	 */
	protected BigDecimal setDeviceSetPoint(float pTemperature) throws IOException, IllegalArgumentException, AtagPageErrorException {

		// Test parameters.
		float roundedTemperature = NumberUtils.roundHalf(pTemperature);
		if (roundedTemperature < TEMPERATURE_MIN || roundedTemperature > TEMPERATURE_MAX) {
			throw new IllegalArgumentException(
				"Device temperature out of bounds: " + roundedTemperature + ". Needs to be between " + TEMPERATURE_MIN + " (inclusive) and " +
					TEMPERATURE_MAX + " (inclusive)");
		}
		if (StringUtils.isBlank(selectedDeviceId)) {
			throw new IllegalArgumentException("No Device selected, cannot get diagnostics.");
		}

		// Get updated request verification token first.
		final String requestVerificationToken = getRequestVerificationToken(URL_DEVICE_HOME);

		// https://portal.atag-one.com/Home/DeviceSetSetpoint/6808-1401-3109_15-30-001-544?temperature=18.5
		final String newUrl = URL_DEVICE_SET_SETPOINT + "/" + selectedDeviceId + "?temperature=" + roundedTemperature;
		log.fine("POST setDeviceSetPoint: " + newUrl);

		Map<String, String> params = new HashMap<String, String>();
		params.put("__RequestVerificationToken", requestVerificationToken);

		// Response contains current temperature.
		// {\"ch_control_mode\":0,\"temp_influenced\":false,\"room_temp\":18.0,\"ch_mode_temp\":18.2,\"is_heating\":true,\"vacationPlanned\":false,\"temp_increment\":null,\"round_half\":false,\"schedule_base_temp\":null,\"outside_temp\":null}
		final String html = HTTPUtils.getPostPageContent(newUrl, params);
		BigDecimal roomTemperature = JSONUtils.getJSONValueByName(html, BigDecimal.class, JSON_ROOM_TEMP);
		if (roomTemperature != null) {
			return roomTemperature;
		}

		throw new IllegalStateException("Cannot read current room temperature.");
	}

	/**
	 * Open device home page and return requests verification token.          ;
	 *
	 * @param url URL to connect to
	 * @return request verification token
	 * @throws IOException           When error connecting to ATAG ONE portal
	 * @throws IllegalStateException When session cannot be started
	 */
	protected String getRequestVerificationToken(@Nonnull @NonNull String url) throws IOException, IllegalStateException, AtagPageErrorException {

		log.fine("getRequestVerificationToken(" + url + ")");

		// HTTP(S) Connect.

		// Try to replace device id, ignore when no replace string available.
		final String newUrl = url.replace("{0}", StringUtils.defaultString(selectedDeviceId));
		String html = HTTPUtils.getPageContent(newUrl);

		// Get request verification.
		String requestVerificationToken = HTMLUtils.extractRequestVerificationTokenFromHtml(html);
		if (!StringUtils.isBlank(requestVerificationToken)) {
			return requestVerificationToken;
		}

		throw new IllegalStateException("No Request Verification Token received.");
	}

	/**
	 * Search for thermostat in local network.
	 *
	 * @return Info about the thermostat found, or null when noting found
	 */
	@Nullable
	protected OneInfo searchOnes() throws IOException {

		final int PORT = 11000;

		// Listen to all UDP packets send to port 11,000.
		DatagramSocket datagramSocket = null;

		try {
			datagramSocket = new DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"));
			datagramSocket.setBroadcast(true);
			datagramSocket.setSoTimeout(30000);

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

		} finally {
			if (datagramSocket != null) {
				datagramSocket.disconnect();
				IOUtils.closeQuietly(datagramSocket);
			}
		}
	}

}
