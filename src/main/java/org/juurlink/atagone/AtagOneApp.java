package org.juurlink.atagone;

import static org.juurlink.atagone.AtagOneConnectorInterface.VALUE_BOILER_HEATING_FOR;
import static org.juurlink.atagone.AtagOneConnectorInterface.VALUE_CH_RETURN_TEMPERATURE;
import static org.juurlink.atagone.AtagOneConnectorInterface.VALUE_CH_SETPOINT;
import static org.juurlink.atagone.AtagOneConnectorInterface.VALUE_CH_WATER_PRESSURE;
import static org.juurlink.atagone.AtagOneConnectorInterface.VALUE_CH_WATER_TEMPERATURE;
import static org.juurlink.atagone.AtagOneConnectorInterface.VALUE_FLAME_STATUS;
import static org.juurlink.atagone.AtagOneConnectorInterface.VALUE_OUTSIDE_TEMPERATURE;
import static org.juurlink.atagone.AtagOneConnectorInterface.VALUE_ROOM_TEMPERATURE;
import static org.juurlink.atagone.AtagOneConnectorInterface.VALUE_TARGET_TEMPERATURE;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.net.UnknownHostException;
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
import org.juurlink.atagone.domain.Version;
import org.juurlink.atagone.exceptions.AccessDeniedException;
import org.juurlink.atagone.exceptions.AtagPageErrorException;
import org.juurlink.atagone.exceptions.AtagSearchErrorException;
import org.juurlink.atagone.utils.JSONUtils;
import org.juurlink.atagone.utils.StringUtils;

import lombok.NonNull;
import lombok.extern.java.Log;
import lombok.val;

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
    private static final String OPTION_SKIP_AUTH_REQUEST = "skip-auth-request";
    private static final String OPTION_DUMP = "dump";
    private static final String OPTION_MAC = "mac";

    private static final String PROPERTY_NAME_MAVEN_APPLICATION_VERSION = "applicationVersion";
    private static final String PROPERTY_NAME_MAVEN_BUILD_DATE = "buildDate";
    private static final String META_INF_MANIFEST_MF = "/META-INF/MANIFEST.MF";

    /**
     * Application start point.
     */
    public static void main(String[] args) {

        // Determine what to do.
        val versionInfo = getVersionInfo();
        final Configuration configuration = validateAndParseCommandLine(args, versionInfo);

        // Show debugging info?
        if (configuration.isDebug()) {
            configureLogger();
        }

        try {
            // Initialize ATAG ONE connector; Either Local or Remote.
            val atagOneConnector = new AtagOneConnectorFactory().getInstance(configuration);

            // Login; Either local or remote.
            atagOneConnector.login();

            // Set temperature?
            if (configuration.getTemperature() != null) {
                val currentRoomTemperature = atagOneConnector.setTemperature(configuration.getTemperature());
                if (currentRoomTemperature != null) {
                    System.out.println(String.format(Locale.US, "%.1f", currentRoomTemperature));
                }

            } else if (configuration.isDump()) {

                // Dump all.
                System.out.println(atagOneConnector.dump());

            } else {
                // Get diagnostics.
                Map<String, Object> diagnostics = atagOneConnector.getDiagnostics();

                if (configuration.getFormat() == FORMAT.CSV) {

                    // Convert flame status to 0 or 1 (boolean).
                    val flameStatus = diagnostics.get(VALUE_FLAME_STATUS);
                    val newFlameStatus = Boolean.TRUE.equals(flameStatus) ? 1 : 0;
                    diagnostics.put("newFlameStatus", newFlameStatus);

                    // Instead of null, print string '-' for boiler heating for.
                    val boilerHeating = diagnostics.get(VALUE_BOILER_HEATING_FOR);
                    diagnostics.put("newBoilerHeating", StringUtils.defaultString(boilerHeating, "-"));

                    // Print a list of CSV values.
                    printValues(diagnostics,
                        VALUE_ROOM_TEMPERATURE,
                        VALUE_OUTSIDE_TEMPERATURE,
                        VALUE_CH_WATER_PRESSURE,
                        VALUE_CH_WATER_TEMPERATURE,
                        VALUE_CH_RETURN_TEMPERATURE,
                        VALUE_TARGET_TEMPERATURE,
                        VALUE_CH_SETPOINT,
                        "newFlameStatus",
                        "newBoilerHeating");

                } else {
                    // Convert Boolean to "On" "Off" Strings.
                    if (diagnostics.containsKey(VALUE_FLAME_STATUS)) {
                        diagnostics.put(VALUE_FLAME_STATUS, (Boolean) diagnostics.get(VALUE_FLAME_STATUS) ? "On" : "Off");
                    }

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

        } catch (AtagPageErrorException | AtagSearchErrorException | AccessDeniedException e) {
            // Print human readable error message.
            System.err.println(e.getMessage());
            System.err.println();
            System.exit(1);

        } catch (UnknownHostException e) {
            // Unknown host given.
            System.err.println("Cannot resolve host-name '" + configuration.getHostName() + "'.");
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
    protected static Configuration validateAndParseCommandLine(final String[] args, final @Nonnull @NonNull Version versionInfo) {

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
        options.addOption(null, OPTION_SKIP_AUTH_REQUEST, false, "Skip the authorization request. \nWhen authorization is already performed, " +
            "a new auth request is not entirely necessarily. Skipping this request in that case could save some seconds.");
        options.addOption(null, OPTION_DUMP, false,
            "Request all info and dump the complete response from the thermostat. Only supported for local thermostats.");
        options.addOption(null, OPTION_MAC, true, "Option to override hardware address (mac address) for authentication.");

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args, true);

            val email = cmd.getOptionValue(OPTION_EMAIL);
            val password = cmd.getOptionValue(OPTION_PASSWORD);
            val debug = cmd.hasOption(OPTION_DEBUG);
            val output = cmd.getOptionValue(OPTION_OUTPUT);
            val hasTemperature = cmd.hasOption(OPTION_SET);
            val temperatureString = cmd.getOptionValue(OPTION_SET);
            val hasVersion = cmd.hasOption(OPTION_VERSION);
            val skipAuthRequest = cmd.hasOption(OPTION_SKIP_AUTH_REQUEST);
            val dump = cmd.hasOption(OPTION_DUMP);
            val mac = cmd.getOptionValue(OPTION_MAC);
            // Remaining arguments
            val hostName = cmd.getArgs() != null && cmd.getArgs().length > 0 ? cmd.getArgs()[0] : null;

            @Nullable
            BigDecimal temperature = null;

            // Display version info.
            if (hasVersion) {
                System.out.println("Version: " + versionInfo.getVersion());
                System.out.println("Build:  " + versionInfo.getTimestamp());
                System.out.println();
                System.exit(0);
            }

            // When username supplied, password is required.
            if (StringUtils.isNotBlank(email) && StringUtils.isBlank(password)) {
                System.err.println("When the email address is specified, the password is required");
                System.err.println();

                showCommandLineHelp(options);
                System.exit(1);
            }

            // Dump option is only available in local operation.
            if (StringUtils.isNotBlank(email) && dump) {
                System.err.println("The dump option is not available for remote operation.");
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
                .hostName(hostName)
                .skipAuthRequest(skipAuthRequest)
                .dump(dump)
                .mac(mac)
                .version(versionInfo)
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
            "It can connect to both the " + THERMOSTAT_NAME + " portal or directly to the thermostat on the local network.\n\n";

        final StringWriter stringWriter = new StringWriter();
        formatter.printHelp(new PrintWriter(stringWriter), 120, EXECUTABLE_NAME, headerMessage, options, 1, 3, null, true);

        // Hack to add "[hostname]" after last option in usage line.
        String result = stringWriter.toString().replace("[-v]", "[-v] [host-name]");
        System.out.println(result);
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
