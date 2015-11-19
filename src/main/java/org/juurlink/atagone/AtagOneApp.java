package org.juurlink.atagone;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.JSONObject;

import lombok.Setter;
import lombok.extern.java.Log;

/**
 * ATAG ONE Portal API.
 */
@Log
public class AtagOneApp {

	private static final String URL_LOGIN = "https://portal.atag-one.com/Account/Login";
	private static final String URL_DIAGNOSTICS = "https://portal.atag-one.com/Device/LatestReport";

	private static final String EXECUTABLE_NAME = "atag-one.sh";
	private static final String USER_AGENT = "Mozilla/5.0 (compatible; AtagOneApp/0.1; http://atag.one/)";

	private static final Pattern PATTERN_REQUEST_VERIFICATION_TOKEN = Pattern
		.compile("name=\"__RequestVerificationToken\"[^>]+ value=\"(.*?)\"", Pattern.DOTALL);
	private static final Pattern PATTERN_DEVICE_ID = Pattern
		.compile("[0-9]{4}-[0-9]{4}-[0-9]{4}_[0-9]{2}-[0-9]{2}-[0-9]{3}-[0-9]{3}", Pattern.DOTALL);
	private static final String ENCODING_UTF_8 = "UTF-8";

	/**
	 * HTTP Connect timeout in milliseconds.
	 */
	private static final int HTTP_CONNECT_TIMEOUT = 5000;

	/**
	 * HTTP Read timeout in milliseconds.
	 */
	private static final int HTTP_READ_TIMEOUT = 5000;

	@Setter
	private String username;
	@Setter
	private String password;

	/**
	 * HTTP(S) Form Request Verification Token.
	 */
	private String requestVerificationToken;

	/**
	 * ATAG ONE device ID.
	 */
	private String selectedDeviceId;

	/**
	 * Create new instance.
	 */
	public AtagOneApp() {
		// Configure default in-memory cookie store.
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
	}

	/**
	 * Application start point.
	 */
	public static void main(String[] args) throws Exception {
		Configuration configuration = parseCommandLine(args);

		// Show debugging info?
		if (configuration.isDebug()) {
			configureLogger();
		}

		// Initialize ATAG ONE Portal connector.
		AtagOneApp atagOneApp = new AtagOneApp();
		atagOneApp.setUsername(configuration.getEmail());
		atagOneApp.setPassword(configuration.getPassword());

		try {
			atagOneApp.login();

			// Get diagnostics.
			final Map<String, Object> diagnoses = atagOneApp.getDiagnostics();

			JSONObject jsonObject = new JSONObject(diagnoses);
			System.out.println(jsonObject.toString(4));
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

		} catch (Throwable e) {
			// Other errors.
			System.err.println(e.getMessage());
			System.err.println();

			System.exit(1);
		}
	}

	/**
	 * Parse command line options and exit in case of error.
	 *
	 * @param args Command line arguments
	 * @return Configuration object with username, password and other settings
	 */
	private static Configuration parseCommandLine(final String[] args) {

		Options options = new Options();
		options.addOption("e", "email", true, "User Portal email address");
		options.addOption("p", "password", true, "User Portal password");
		options.addOption("h", "help", false, "Print this help message");
		options.addOption("d", "debug", false, "Print debugging information");

		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args);

			final String email = cmd.getOptionValue("e");
			final String password = cmd.getOptionValue("p");
			final boolean debug = cmd.hasOption("d");

			if (StringUtils.isBlank(email) || StringUtils.isBlank(password)) {
				System.err.println("Username and password are both required");
				System.err.println();

				showCommandLineHelp(options);
				System.exit(1);
			}

			if (cmd.hasOption("h")) {
				showCommandLineHelp(options);
				System.exit(0);
			}

			return new Configuration(email, password, debug);

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
	 * Show command line help.
	 */
	private static void showCommandLineHelp(final Options options) {
		// Automatically generate the help statement.
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(EXECUTABLE_NAME, options);
	}

	/**
	 * Configure logging.
	 */
	private static void configureLogger() {

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
	 * Start new HTTP(S) session.
	 *
	 * @throws IOException           When error connecting to ATAG ONE portal
	 * @throws IllegalStateException When session cannot be started
	 */
	private void startSession() throws IOException, IllegalStateException {

		log.fine("GET login page: " + URL_LOGIN);

		// HTTP(S) Connect.
		HttpURLConnection httpConnection = createHttpConnection(URL_LOGIN);

		// HTTPS Connect, and get login page HTML.
		InputStream inputStreamStd = null;
		InputStream inputStreamErr = null;
		try {
			inputStreamStd = httpConnection.getInputStream();
			final String html = IOUtils.toString(inputStreamStd, ENCODING_UTF_8);

			// Get request verification.
			requestVerificationToken = getRequestVerificationToken(html);
			if (StringUtils.isBlank(requestVerificationToken)) {
				throw new IllegalStateException("No Request Verification Token received, cannot continue.");
			}

		} catch (IOException e) {

			inputStreamErr = httpConnection.getErrorStream();
			final String html = IOUtils.toString(inputStreamErr, ENCODING_UTF_8);

			// Log debug details in case of error.
			log.fine(html);
			throw e;

		} finally {
			IOUtils.closeQuietly(inputStreamStd);
			IOUtils.closeQuietly(inputStreamErr);
		}
	}

	/**
	 * Login ATAG ONE portal and select first Device found.
	 */
	private void login() throws IOException {

		// We need a session and a verification token.
		startSession();

		log.fine("POST authentication data: " + URL_LOGIN);

		String urlParameters =
			"__RequestVerificationToken=" + URLEncoder.encode(requestVerificationToken, ENCODING_UTF_8) +
				"&Email=" + URLEncoder.encode(username, ENCODING_UTF_8) +
				"&Password=" + URLEncoder.encode(password, ENCODING_UTF_8) +
				"&RememberMe=false";

		byte[] postData = urlParameters.getBytes(ENCODING_UTF_8);

		// HTTP(S) Connect.
		HttpURLConnection httpConnection = createHttpConnection(URL_LOGIN);
		httpConnection.setDoOutput(true);
		httpConnection.setRequestMethod("POST");
		httpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; " + ENCODING_UTF_8);
		httpConnection.setRequestProperty("Content-Length", "" + postData.length);

		OutputStream outputStream = null;
		try {
			outputStream = httpConnection.getOutputStream();
			outputStream.write(postData);
		} finally {
			IOUtils.closeQuietly(outputStream);
		}

		InputStream inputStreamStd = null;
		InputStream inputStreamErr = null;
		try {
			inputStreamStd = httpConnection.getInputStream();
			final String html = IOUtils.toString(inputStreamStd, ENCODING_UTF_8);
			selectedDeviceId = extractDeviceIdFromHtml(html);

			if (StringUtils.isBlank(selectedDeviceId)) {
				throw new IllegalStateException("No Device ID found, cannot continue.");
			}

		} catch (IOException e) {

			inputStreamErr = httpConnection.getErrorStream();
			final String html = IOUtils.toString(inputStreamErr, ENCODING_UTF_8);

			// Log debug details in case of error.
			log.fine(html);
			throw e;

		} finally {
			IOUtils.closeQuietly(inputStreamStd);
			IOUtils.closeQuietly(inputStreamErr);
		}
	}

	/**
	 * Get all diagnostics for selected device.
	 *
	 * @return Map of diagnostic info
	 * @throws IOException              in case of connection error
	 * @throws IllegalArgumentException when no device selected
	 */
	private Map<String, Object> getDiagnostics() throws IOException, IllegalArgumentException {

		if (StringUtils.isBlank(selectedDeviceId)) {
			throw new IllegalArgumentException("No Device selected, cannot get diagnostics.");
		}

		final String urlString = URL_DIAGNOSTICS + "?deviceId=" + URLEncoder.encode(selectedDeviceId, ENCODING_UTF_8);
		log.fine("GET diagnostics: " + urlString);

		// HTTP(S) Connect.
		HttpURLConnection httpConnection = createHttpConnection(urlString);

		InputStream inputStreamStd = null;
		InputStream inputStreamErr = null;
		try {
			inputStreamStd = httpConnection.getInputStream();
			final String html = IOUtils.toString(inputStreamStd, ENCODING_UTF_8);

			// Scrape values from HTML page.
			Map<String, Object> values = new HashMap<String, Object>();
			values.put("deviceId", selectedDeviceId);
			values.put("deviceAlias", getDiagnosticValueByLabel(html, String.class, "Apparaat alias", "Device alias"));
			values.put("latestReportTime", getDiagnosticValueByLabel(html, String.class, "Laatste rapportagetijd", "Latest report time"));
			values.put("connectedTo", getDiagnosticValueByLabel(html, String.class, "Verbonden met", "Connected to"));
			values.put("burningHours", getDiagnosticValueByLabel(html, BigDecimal.class, "Branduren", "Burning hours"));
			values.put("boilerHeatingFor", getDiagnosticValueByLabel(html, String.class, "Ketel in bedrijf voor", "Boiler heating for"));
			values.put("flameStatus", getDiagnosticValueByLabel(html, Boolean.class, "Brander status", "Flame status"));
			values.put("roomTemperature", getDiagnosticValueByLabel(html, BigDecimal.class, "Kamertemperatuur", "Room temperature"));
			values.put("outsideTemperature", getDiagnosticValueByLabel(html, BigDecimal.class, "Buitentemperatuur", "Outside temperature"));
			values.put("dhwSetpoint", getDiagnosticValueByLabel(html, BigDecimal.class, "Setpoint warmwater", "DHW setpoint"));
			values.put("dhwWaterTemperature", getDiagnosticValueByLabel(html, BigDecimal.class, "Warmwatertemperatuur", "DHW water temperature"));
			values.put("chSetpoint", getDiagnosticValueByLabel(html, BigDecimal.class, "Setpoint cv", "CH setpoint"));
			values.put("chWaterTemperature", getDiagnosticValueByLabel(html, BigDecimal.class, "CV-aanvoertemperatuur", "CH water temperature"));
			values.put("chWaterPressure", getDiagnosticValueByLabel(html, BigDecimal.class, "CV-waterdruk", "CH water pressure"));
			values.put("chReturnTemperature", getDiagnosticValueByLabel(html, BigDecimal.class, "CV retourtemperatuur", "CH return temperature"));
			return values;

		} catch (IOException e) {

			inputStreamErr = httpConnection.getErrorStream();
			final String html = IOUtils.toString(inputStreamErr, ENCODING_UTF_8);

			// Log debug details in case of error.
			log.fine(html);
			throw e;

		} finally {
			IOUtils.closeQuietly(inputStreamStd);
			IOUtils.closeQuietly(inputStreamErr);
		}
	}

	/**
	 * Create HTTP(s) connection.
	 *
	 * @param urlString URL to connect to.
	 */
	private HttpURLConnection createHttpConnection(final String urlString) throws IOException {
		HttpURLConnection httpConnection = (HttpURLConnection) new URL(urlString).openConnection();

		// Complete list of HTTP header fields:
		// https://en.wikipedia.org/wiki/List_of_HTTP_header_fields
		httpConnection.setRequestProperty("Accept-Charset", ENCODING_UTF_8);
		httpConnection.setRequestProperty("Accept", "text/html");
		httpConnection.setRequestProperty("User-Agent", USER_AGENT);
		httpConnection.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
		httpConnection.setReadTimeout(HTTP_READ_TIMEOUT);
		return httpConnection;
	}

	/**
	 * Get RequestVerificationToken from HTML.
	 *
	 * @return RequestVerificationToken or null when not found in HTML
	 */
	protected String getRequestVerificationToken(final String html) {
		String result = null;
		// <input name="__RequestVerificationToken" type="hidden" value="lFVlMZlt2-YJKAwZWS_K_p3gsQWjZOvBNBZ3lM8io_nFGFL0oRsj4YwQUdqGfyrEqGwEUPmm0FgKH1lPWdk257tuTWQ1" />
		final Matcher matcher = PATTERN_REQUEST_VERIFICATION_TOKEN.matcher(html);
		if (matcher.find()) {
			result = matcher.group(1);
		}
		return result;
	}

	/**
	 * Get Device ID from HTML.
	 *
	 * @return Device ID or null when ID not found within HTML
	 */
	protected String extractDeviceIdFromHtml(final String html) {
		String result = null;
		// <tr onclick="javascript:changeDeviceAndRedirect('/Home/Index/{0}','6808-1401-3109_15-30-001-544');">
		final Matcher matcher = PATTERN_DEVICE_ID.matcher(html);
		if (matcher.find()) {
			result = matcher.group(0);
		}
		return result;
	}

	/**
	 * Get Device ID from HTML.
	 *
	 * @param html   Full html of page
	 * @param clazz  Class type of value; [String, BigInteger, Boolean]
	 * @param labels Labels to search for, one or more
	 * @return Value or null when not found
	 * @throws IllegalStateException When requested value class not supported
	 */
	protected Object getDiagnosticValueByLabel(final String html, final Class clazz, final String... labels) {

		// HTML structure of values in page.
		//     <label class="col-xs-6 control-label">Apparaat alias</label>
		//     <div class="col-xs-6">
		//         <p class="form-control-static">CV-ketel</p>
		//     </div>
		for (final String label : labels) {
			final Pattern pattern = Pattern.compile(">" + label + "</label>.*?<p[^>]*>(.*?)<", Pattern.DOTALL + Pattern.CASE_INSENSITIVE);
			final Matcher matcher = pattern.matcher(html);
			if (matcher.find()) {
				final String value = matcher.group(1);
				if (!value.isEmpty()) {
					// Replace Dutch decimal separator.
					final String valueString = value.replace(",", ".").trim();
					if (clazz == String.class) {
						return valueString;
					}
					if (clazz == Boolean.class) {
						return "aan".equalsIgnoreCase(valueString) || "on".equalsIgnoreCase(valueString);
					}
					if (clazz == BigDecimal.class) {
						return new BigDecimal(valueString);
					}
					throw new IllegalStateException("Unknown return type requested: '" + clazz + "'");
				}
			}
		}
		return null;
	}
}
