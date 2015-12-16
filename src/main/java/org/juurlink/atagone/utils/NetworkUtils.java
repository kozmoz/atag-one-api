package org.juurlink.atagone.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.juurlink.atagone.domain.DeviceInfo;
import org.juurlink.atagone.domain.UdpMessage;
import org.juurlink.atagone.exceptions.AtagPageErrorException;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

/**
 * HTTP related utility methods.
 */
@Log
@UtilityClass
public class NetworkUtils {

	/**
	 * Max number of connection retries. Sometimes a request result in "Connection Error: Unexpected end of file from server".
	 */
	private static final String ENCODING_UTF_8 = "UTF-8";
	private static final String USER_AGENT = "Mozilla/5.0 (compatible; AtagOneApp/0.1; http://atag.one/)";
	private static final String REQUEST_METHOD_POST = "POST";
	private static final String REQUEST_HEADER_CONTENT_TYPE = "Content-Type";
	private static final String REQUEST_HEADER_CONTENT_LENGTH = "Content-Length";
	private static final String REQUEST_HEADER_ACCEPT_CHARSET = "Accept-Charset";
	private static final String REQUEST_HEADER_ACCEPT = "Accept";
	private static final String REQUEST_HEADER_USER_AGENT = "User-Agent";
	private static final Pattern PATTERN_PAGE_ERROR = Pattern.compile("<li class=\"text-error\"><span>(.*?)</span>", Pattern.DOTALL);

	private static final int TIMEOUT_REACHABLE_MS = 1000;
	/**
	 * HTTP Connect timeout in milliseconds.
	 */
	private static final int HTTP_CONNECT_TIMEOUT_MS = 10000;
	/**
	 * HTTP Read timeout in milliseconds.
	 */
	private static final int HTTP_READ_TIMEOUT_MS = 10000;
	/**
	 * Connection timeout in milliseconds.
	 */
	private static final int MAX_CONNECTION_TIMEOUT_MS = 30000;
	/**
	 * Time between retries in milliseconds.
	 */
	private static final int MAX_TIME_BETWEEN_RETRIES_MS = 2000;

	static {
		// Configure default in-memory cookie store.
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
	}

	/**
	 * Get GET page content.
	 *
	 * @return Full page content html
	 * @throws IOException            in case of connection error
	 * @throws AtagPageErrorException in case the page contains an error message
	 */
	public static String getPageContent(@NonNull String url) throws IOException {

		// HTTP(S) Connect.
		HttpURLConnection httpConnection = createHttpConnection(url);

		return toPageResponse(httpConnection);
	}

	/**
	 * Get POST page content; form url encoded.
	 *
	 * @param url        URL to connect to
	 * @param parameters POST parameters
	 * @return Full page content html
	 * @throws IOException            in case of connection error
	 * @throws AtagPageErrorException in case the page contains an error message
	 */
	public static String getPostPageContent(@NonNull String url, Map<String, String> parameters) throws IOException {

		byte[] postData = createPostBody(parameters);

		// HTTP(S) Connect.
		HttpURLConnection httpConnection = createHttpConnection(url);
		httpConnection.setDoOutput(true);
		httpConnection.setRequestMethod(REQUEST_METHOD_POST);
		httpConnection.setRequestProperty(REQUEST_HEADER_CONTENT_TYPE, "application/x-www-form-urlencoded; " + ENCODING_UTF_8);
		httpConnection.setRequestProperty(REQUEST_HEADER_CONTENT_LENGTH, "" + postData.length);

		OutputStream outputStream = null;
		try {
			outputStream = httpConnection.getOutputStream();
			outputStream.write(postData);
		} finally {
			IOUtils.closeQuietly(outputStream);
		}

		return toPageResponse(httpConnection);
	}

	/**
	 * Get POST page content.
	 *
	 * @param url  URL to connect to
	 * @param json JSON payload
	 * @return Full page content html or json
	 * @throws IOException            in case of connection error
	 * @throws AtagPageErrorException in case the page contains an error message
	 */
	@Nonnull
	public static String getPostPageContent(@NonNull String url, @NonNull String json) throws IOException {

		byte[] postData = json.getBytes(Charset.forName("UTF-8"));

		// HTTP(S) Connect.
		HttpURLConnection httpConnection = createHttpConnection(url);
		httpConnection.setDoOutput(true);
		httpConnection.setRequestMethod(REQUEST_METHOD_POST);
		httpConnection.setRequestProperty(REQUEST_HEADER_CONTENT_TYPE, "application/json; " + ENCODING_UTF_8);
		httpConnection.setRequestProperty(REQUEST_HEADER_CONTENT_LENGTH, "" + postData.length);

		OutputStream outputStream = null;
		try {
			outputStream = httpConnection.getOutputStream();
			outputStream.write(postData);

		} finally {
			IOUtils.closeQuietly(outputStream);
		}

		return toPageResponse(httpConnection);
	}

	/**
	 * Get system Hostname, IP - and MAC address.
	 *
	 * @return DeviceInfo Hostname, IP - and MAC address
	 * @throws IllegalStateException When no network interfaces found
	 * @throws SocketException       When not able to get network interface
	 */
	public static DeviceInfo getDeviceInfo() throws IllegalStateException, SocketException {

		// Search for network interfaces.
		final List<InetAddress> localHosts = getLocalHosts();
		if (localHosts.isEmpty()) {
			throw new IllegalStateException("Cannot determine local IP address.");
		}

		// Get the first one (in case of eth0 and eth1, get eth0).
		InetAddress inetAddress = localHosts.get(0);
		NetworkInterface network = NetworkInterface.getByInetAddress(inetAddress);
		byte[] mac = network.getHardwareAddress();

		// Convert mac address to human readable string.
		StringBuilder macAddressString = new StringBuilder();
		for (int i = 0; i < mac.length; i++) {
			macAddressString.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
		}

		return new DeviceInfo(inetAddress.getHostName(), inetAddress, macAddressString.toString());
	}

	/**
	 * Get device IP address, or "localhost" in case the AP address cannot be determined.
	 * <p/>
	 * InetAddress.getLocalHost() doesn't do what most people think that it does. It actually returns the hostname of the machine, and the IP address
	 * associated with that hostname. This may be the address used to connect to the outside world. It may not. It just depends on how you have your
	 * system configured.
	 * <p/>
	 * For instance, looking at a Linux box, if you have the following configuration:<br />
	 * <pre>
	 *
	 * hostname 'myhostname'
	 *
	 * /etc/nsswitch.conf:
	 *
	 * hosts:      files dns
	 *
	 * (i.e. look to the hosts file before checking DNS when resolving hostnames)
	 *
	 * /etc/hosts:
	 * 127.0.0.1 localhost localhost.localdomain myhostname
	 *
	 * </pre>
	 * then InetAddress.getLocalHost() will get the hostname ('myhostname'), resolve the hostname (which will return 127.0.0.1, since that's the IP
	 * address that will resolve from 'myhostname'), and then return an InetAddress object for hostname 'myhostname' with IP address '127.0.0.1'.
	 *
	 * @return List of ip addresses found
	 */
	public static synchronized List<InetAddress> getLocalHosts() {

		List<InetAddress> localHost = new ArrayList<InetAddress>();

		try {
			localHost.clear();

			Map<String, InetAddress> addresses = new HashMap<String, InetAddress>();

			Enumeration iFaces = NetworkInterface.getNetworkInterfaces();
			for (; iFaces.hasMoreElements(); ) {

				NetworkInterface iface = (NetworkInterface) iFaces.nextElement();

				// Ignore local and virtual interfaces.
				if (!iface.isUp() || iface.isVirtual() || iface.isLoopback() || iface.isPointToPoint()) {
					continue;
				}

				// getName() gives short name.
				String ifaceName = iface.getName().toLowerCase();
				if (ifaceName.startsWith("lo") || ifaceName.startsWith("vmnet") || ifaceName.startsWith("vboxnet")) {
					// Skip local, VMWare and VirtualBox.
					continue;
				}

				for (Enumeration ips = iface.getInetAddresses(); ips.hasMoreElements(); ) {
					InetAddress ip = (InetAddress) ips.nextElement();
					if (ip instanceof Inet4Address) {

						// Ignore localhost, self-assigned and BlueTooth (which starts probably with 172).
						String hostAddress = ip.getHostAddress();
						if (hostAddress.startsWith("169.254") || hostAddress.startsWith("127") || hostAddress.startsWith("172")) {
							log.info("Skip localhost address or 172 range: " + hostAddress);
							continue;
						}

						try {
							if (ip.isReachable(TIMEOUT_REACHABLE_MS)) {
								addresses.put(ifaceName, ip);
							}
						} catch (IOException e) {
							log.fine("Skip unreachable IP: " + ip);
						}
					}
				}
			}

			// Cache addresses found.
			if (addresses.size() == 1) {

				localHost.addAll(addresses.values());
				return localHost;

			} else if (addresses.size() > 1) {

				// More then one address found, order list bij name (eth0 will be on top, above eth1).
				List<String> interfaceNames = new ArrayList<String>(addresses.keySet());
				StringUtils.sort(interfaceNames);

				for (String interfaceName : interfaceNames) {
					localHost.add(addresses.get(interfaceName));
				}
				return localHost;
			}
		} catch (SocketException e) {
			log.log(Level.WARNING, "Error determining local host address.", e);
			// Continue, try alternative method.
		}

		try {
			localHost.add(InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			log.log(Level.INFO, "Error getting localhost address.", e);
			try {
				localHost.add(InetAddress.getByName("127.0.0.1"));
			} catch (UnknownHostException e1) {
				log.log(Level.WARNING, "Localhost 127.0.0.1 unknown", e1);
			}
		}

		return localHost;
	}

	/**
	 * Receive UDP broadcast message.
	 *
	 * @param port              UDP port to listen on
	 * @param maxTimeoutSeconds Max number of seconds to wait for message
	 * @param messageTag        Message identification tag
	 * @return Message found or null in case no message
	 * @throws IOException in case of technical error
	 */
	@Nullable
	public static UdpMessage getUdpBroadcastMessage(final int port, final int maxTimeoutSeconds, final String messageTag) throws IOException {

		if (maxTimeoutSeconds < 0) {
			throw new IllegalArgumentException("'maxTimeoutSeconds' value cannot be smaller than zero.");
		}

		DatagramSocket datagramSocket = null;
		long endTimeMs = System.currentTimeMillis() + (maxTimeoutSeconds * 1000);

		try {
			// Listen to all UDP packets to any interface to port 'port'.
			datagramSocket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
			datagramSocket.setBroadcast(true);
			datagramSocket.setSoTimeout(MAX_CONNECTION_TIMEOUT_MS);

			// ATAG One message size is 37 bytes and that is the only message we are interested in.
			byte[] receiveData = new byte[37];

			// Keep reading received messages until time runs out.
			while (System.currentTimeMillis() < endTimeMs) {

				final DatagramPacket datagramPacket = new DatagramPacket(receiveData, receiveData.length);
				datagramSocket.receive(datagramPacket);

				final InetAddress messageInetAddress = datagramPacket.getAddress();
				final String receivedMessage = new String(datagramPacket.getData(), ENCODING_UTF_8);

				// Found message
				if (receivedMessage.startsWith(messageTag)) {
					return new UdpMessage(messageInetAddress, receivedMessage);
				}
			}
			return null;

		} finally {
			if (datagramSocket != null) {
				datagramSocket.disconnect();
				IOUtils.closeQuietly(datagramSocket);
			}
		}
	}

	/**
	 * Get page contents from given httpUrlConnection en close the streams.
	 *
	 * @param httpConnection Opened connection
	 * @return pageContent as String
	 */
	@Nonnull
	protected static String toPageResponse(@NonNull final HttpURLConnection httpConnection)
		throws IOException {
		InputStream inputStreamStd = null;
		InputStream inputStreamErr = null;
		try {
			inputStreamStd = httpConnection.getInputStream();
			final String html = IOUtils.toString(inputStreamStd, ENCODING_UTF_8);

			// Does the page contain an error message?
			// (Only in case of HTML response.)
			if (html.contains("<")) {
				String errorMessage = extractPageErrorFromHtml(html);
				if (!StringUtils.isBlank(errorMessage)) {
					throw new AtagPageErrorException(errorMessage);
				}
			}
			return html;

		} catch (IOException e) {

			// Retries exhausted; Connection failure.
			String errorResponse = null;
			try {
				inputStreamErr = httpConnection.getErrorStream();
				errorResponse = IOUtils.toString(inputStreamErr, ENCODING_UTF_8);
			} catch (IOException e2) {
				// Ignore this error.
				log.fine("Error reading error stream: " + e2);
			}

			// Log debug details in case of error.
			if (!StringUtils.isBlank(errorResponse)) {
				log.fine(errorResponse);
				throw new IOException(errorResponse, e);
			} else {
				throw e;
			}

		} finally {
			IOUtils.closeQuietly(inputStreamStd);
			IOUtils.closeQuietly(inputStreamErr);
		}
	}

	/**
	 * Create HTTP(s) connection.
	 *
	 * @param urlString URL to connect to.
	 * @throws IOException in case of connection error
	 */
	protected static HttpURLConnection createHttpConnection(final String urlString) throws IOException {
		HttpURLConnection httpConnection = (HttpURLConnection) new URL(urlString).openConnection();

		// Complete list of HTTP header fields:
		// https://en.wikipedia.org/wiki/List_of_HTTP_header_fields
		httpConnection.setRequestProperty(REQUEST_HEADER_ACCEPT_CHARSET, ENCODING_UTF_8);
		httpConnection.setRequestProperty(REQUEST_HEADER_ACCEPT, "*/*");
		httpConnection.setRequestProperty(REQUEST_HEADER_USER_AGENT, USER_AGENT);
		httpConnection.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
		httpConnection.setReadTimeout(HTTP_READ_TIMEOUT_MS);
		return httpConnection;
	}

	/**
	 * Extract page error message from HTML.
	 *
	 * @param html HTML
	 * @return Error message or null when no message available on page
	 */
	@Nullable
	protected static String extractPageErrorFromHtml(@Nonnull @NonNull final String html) {
		String result = null;
		// <li class="text-error"><span>Your account has been locked out due to multiple failed login attempts. It will be unlocked in 12 minutes.</span>
		final Matcher matcher = PATTERN_PAGE_ERROR.matcher(html);
		if (matcher.find()) {
			result = matcher.group(1);
		}
		return result;
	}

	/**
	 * Create UTF-8 URL encoded POST body.
	 */
	protected static byte[] createPostBody(final Map<String, String> parameters) throws UnsupportedEncodingException {
		// Create POST request payload.
		StringBuilder urlParams = new StringBuilder();
		for (Entry<String, String> stringStringEntry : parameters.entrySet()) {
			if (urlParams.length() > 0) {
				urlParams.append("&");
			}
			urlParams.append(stringStringEntry.getKey());
			urlParams.append("=");
			urlParams.append(URLEncoder.encode(stringStringEntry.getValue(), ENCODING_UTF_8));
		}
		return urlParams.toString().getBytes(ENCODING_UTF_8);
	}

}
