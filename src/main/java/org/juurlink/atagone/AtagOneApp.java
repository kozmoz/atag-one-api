package org.juurlink.atagone;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
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
import org.juurlink.atagone.domain.FORMAT;
import org.juurlink.atagone.domain.PortalCredentials;
import org.juurlink.atagone.domain.Version;
import org.juurlink.atagone.exceptions.AtagPageErrorException;
import org.juurlink.atagone.exceptions.AtagSearchErrorException;
import org.juurlink.atagone.utils.JSONUtils;
import org.juurlink.atagone.utils.StringUtils;

import lombok.NonNull;
import lombok.extern.java.Log;

/**
 * ATAG ONE Portal API.
 */
@Log
public class AtagOneApp {

	public static final String THERMOSTAT_NAME = "ATAG One";
	public static final String EXECUTABLE_NAME = "atag-one";

	public static final int TEMPERATURE_MIN = 4;
	public static final int TEMPERATURE_MAX = 27;

	// Command line options.
	private static final String OPTION_EMAIL = "email";
	private static final String OPTION_PASSWORD = "password";
	private static final String OPTION_HELP = "help";
	private static final String OPTION_DEBUG = "debug";
	private static final String OPTION_OUTPUT = "output";
	private static final String OPTION_SET = "set";
	private static final String OPTION_VERSION = "version";

	private static final String PROPERTY_NAME_MAVEN_APPLICATION_VERSION = "applicationVersion";
	private static final String PROPERTY_NAME_MAVEN_BUILD_DATE = "buildDate";
	private static final String META_INF_MANIFEST_MF = "/META-INF/MANIFEST.MF";

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

		try {
			// Initialize ATAG ONE connector; Either local or remote.
			AtagOneConnectorInterface atagOneConnector;
			if (configuration.isLocal()) {
				atagOneConnector = new AtagOneLocalConnector();
			} else {
				PortalCredentials portalCredentials = PortalCredentials.builder()
					.emailAddress(configuration.getEmail())
					.password(configuration.getPassword())
					.build();
				atagOneConnector = new AtagOneRemoteConnector(portalCredentials);
			}

			// Login; Either local or remote.
			atagOneConnector.login();

			// Set temperature?
			if (configuration.getTemperature() != null) {
				BigDecimal currentRoomTemperature = atagOneConnector.setTemperature(configuration.getTemperature());
				if (currentRoomTemperature != null) {
					System.out.println(String.format(Locale.US, "%.1f", currentRoomTemperature));
				}

			} else {
				// Get diagnostics.
				Map<String, Object> diagnostics = atagOneConnector.getDiagnostics();

				if (configuration.getFormat() == FORMAT.CSV) {

					// Convert flame status to 0 or 1 (boolean).
					final Boolean flameStatus = (Boolean) diagnostics.get(AtagOneConnectorInterface.VALUE_FLAME_STATUS);
					int newFlameStatus = flameStatus == null || !flameStatus ? 0 : 1;
					diagnostics.put("newFlameStatus", newFlameStatus);

					// Instead of null, print string '-' for boiler heating for.
					final String boilerHeating = (String) diagnostics.get(AtagOneConnectorInterface.VALUE_BOILER_HEATING_FOR);
					diagnostics.put("newBoilerHeating", StringUtils.defaultString(boilerHeating, "-"));

					// Print a list of CSV values.
					printValues(diagnostics,
						AtagOneConnectorInterface.VALUE_ROOM_TEMPERATURE,
						AtagOneConnectorInterface.VALUE_OUTSIDE_TEMPERATURE,
						AtagOneConnectorInterface.VALUE_CH_WATER_PRESSURE,
						AtagOneConnectorInterface.VALUE_CH_WATER_TEMPERATURE,
						AtagOneConnectorInterface.VALUE_CH_RETURN_TEMPERATURE,
						AtagOneConnectorInterface.VALUE_TARGET_TEMPERATURE,
						AtagOneConnectorInterface.VALUE_CH_SETPOINT,
						"newFlameStatus",
						"newBoilerHeating");

				} else {
					// Print diagnostics as JSON and keep the sequence.
					System.out.println(JSONUtils.toJSON(diagnostics));
				}
			}
			System.out.println();

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

		} catch (IOException e) {
			// Print human readable error message; include class name in error.
			System.err.println("Input Output Error: " + e.toString());
			e.printStackTrace(System.err);
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
	 * Print values from map in given sequence.
	 *
	 * @param diagnostics Key value map
	 * @param keys        List of keys of values to print; the sequence is preserved
	 */
	protected static void printValues(@Nonnull @NonNull final Map<String, Object> diagnostics, final String... keys) {
		for (String key : keys) {
			System.out.print(diagnostics.get(key));
			System.out.print(" ");
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
			BigDecimal temperature = null;

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
					temperature = new BigDecimal(temperatureString);
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
			return Configuration.builder()
				.temperature(temperature)
				.email(email)
				.password(password)
				.debug(debug)
				.format(outputFormat)
				.build();

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
	 * Display atag-one program version info and build timestamp.
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

		return Version.builder()
			.version(mavenApplicationVersion)
			.timestamp(mavenBuildDate)
			.build();
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
}
