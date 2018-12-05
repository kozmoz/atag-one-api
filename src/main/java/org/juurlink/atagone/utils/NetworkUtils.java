package org.juurlink.atagone.utils;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import lombok.val;
import org.juurlink.atagone.domain.DeviceInfo;
import org.juurlink.atagone.domain.UdpMessage;
import org.juurlink.atagone.exceptions.AtagPageErrorException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP related utility methods.
 */
@Log
@UtilityClass
public class NetworkUtils {

    /**
     * Max number of connection retries. Sometimes a request result in "Connection Error: Unexpected end of file from server".
     */
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; AtagOneAPI/$0; http://atag.one/)";
    private static final String REQUEST_METHOD_POST = "POST";
    private static final String REQUEST_HEADER_CONTENT_TYPE = "Content-Type";
    private static final String REQUEST_HEADER_CONTENT_LENGTH = "Content-Length";
    private static final String REQUEST_HEADER_ACCEPT_CHARSET = "Accept-Charset";
    private static final String REQUEST_HEADER_ACCEPT = "Accept";
    private static final String REQUEST_HEADER_USER_AGENT = "User-Agent";
    private static final String REQUEST_HEADER_X_ONEAPP_VERSION = "X-OneApp-Version";
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
    private static final int MAX_CONNECTION_TIMEOUT_MS = 60000;

    static {
        // Configure default in-memory cookie store.
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
    }

    /**
     * Get GET page content.
     *
     * @param url           URL to get raw page contents from
     * @param versionString Optional version string, will be used in request header
     * @return Full page content html
     * @throws IOException            in case of connection error
     * @throws AtagPageErrorException in case the page contains an error message
     */
    public static String getPageContent(final @NonNull String url, final @Nullable String versionString) throws IOException {

        // HTTP(S) Connect.
        HttpURLConnection httpConnection = post(url, versionString);

        return toPageResponse(httpConnection);
    }

    /**
     * Get POST page content; form url encoded.
     *
     * @param url           URL to connect to
     * @param parameters    POST parameters
     * @param versionString Optional version string, will be used in request header
     * @return Full page content html
     * @throws IOException            in case of connection error
     * @throws AtagPageErrorException in case the page contains an error message
     */
    public static String getPostPageContent(final @NonNull String url,
                                            final @Nonnull @NonNull Map<String, String> parameters,
                                            final @Nullable String versionString) throws IOException {

        return toPageResponse(post(url, createPostBody(parameters), versionString));
    }

    /**
     * Get POST page content.
     *
     * @param url           URL to connect to
     * @param json          JSON payload
     * @param versionString Optional version string, will be used in request header
     * @return Full page content html or json, headers included
     * @throws IOException            in case of connection error
     * @throws AtagPageErrorException in case the page contains an error message
     */
    @Nonnull
    public static PageContent getPostPageContent(final @NonNull String url,
                                                 final @NonNull String json,
                                                 final @Nullable String versionString) throws IOException {

        byte[] postData = json.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection httpConnection = post(url, postData, versionString);

        // Get raw page contents.
        final String content = toPageResponse(httpConnection);

        // Return both contents and headers.
        return PageContent.builder()
                .content(content)
                .headers(httpConnection.getHeaderFields())
                .build();
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
        String macAddress = formatHardwareAddress(mac);

        return DeviceInfo.builder()
                .name(inetAddress.getHostName())
                .ip(inetAddress)
                .mac(macAddress)
                .build();
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

        val localHost = new ArrayList<InetAddress>();

        try {
            localHost.clear();

            val addresses = new HashMap<String, InetAddress>();
            val iFaces = NetworkInterface.getNetworkInterfaces();
            for (; iFaces.hasMoreElements(); ) {

                val iface = iFaces.nextElement();

                // Ignore local and virtual interfaces.
                if (!iface.isUp() || iface.isVirtual() || iface.isLoopback() || iface.isPointToPoint()) {
                    continue;
                }

                // getName() gives short name.
                val ifaceName = iface.getName().toLowerCase();
                if (ifaceName.startsWith("lo") || ifaceName.startsWith("vmnet") || ifaceName.startsWith("vboxnet")) {
                    // Skip local, VMWare and VirtualBox.
                    continue;
                }

                for (Enumeration ips = iface.getInetAddresses(); ips.hasMoreElements(); ) {
                    val ip = (InetAddress) ips.nextElement();
                    if (ip instanceof Inet4Address) {

                        // Ignore localhost, self-assigned and BlueTooth (which is probably from the 172.29.0.0/16 network).
                        val hostAddress = ip.getHostAddress();
                        if (hostAddress.startsWith("169.254") || hostAddress.startsWith("127") || hostAddress.startsWith("172.29")) {
                            log.info("Skip localhost, self assigned or 172.29/16 network: " + hostAddress);
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
                val interfaceNames = new ArrayList<String>(addresses.keySet());
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
            datagramSocket = new DatagramSocket(port);
            datagramSocket.setBroadcast(true);
            datagramSocket.setSoTimeout(MAX_CONNECTION_TIMEOUT_MS);

            // ATAG One message size is 37 bytes and that is the only message we are interested in.
            byte[] receiveData = new byte[37];

            // Keep reading received messages until time runs out.
            while (System.currentTimeMillis() < endTimeMs) {

                val datagramPacket = new DatagramPacket(receiveData, receiveData.length);
                datagramSocket.receive(datagramPacket);

                val senderAddress = datagramPacket.getAddress();
                val receivedMessage = new String(datagramPacket.getData(), StandardCharsets.UTF_8);

                // Found message
                if (receivedMessage.startsWith(messageTag)) {
                    return UdpMessage.builder()
                            .senderAddress(senderAddress)
                            .message(receivedMessage)
                            .build();
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
     * Convert mac address to human readable string.
     */
    @Nonnull
    public static String formatHardwareAddress(final byte[] mac) {
        val macAddressString = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            macAddressString.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
        }
        return macAddressString.toString();
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
            val html = IOUtils.toString(inputStreamStd, StandardCharsets.UTF_8);

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

            // Connection failure.
            String errorResponse = null;
            try {
                inputStreamErr = httpConnection.getErrorStream();
                errorResponse = IOUtils.toString(inputStreamErr, StandardCharsets.UTF_8);
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
     * Create HTTP(s) connection, connect to it and post the payload data.
     *
     * @param urlString     URL to connect to
     * @param versionString Optional version string, will be used in request header
     * @param postData      Payload data to send
     * @return the connection the response can be read from
     * @throws IOException in case of connection error
     */
    @Nonnull
    protected static HttpURLConnection post(final @NonNull String urlString,
                                            final @NonNull byte[] postData,
                                            final @Nullable String versionString) throws IOException {
        // HTTP(S) Connect.
        HttpURLConnection httpConnection = post(urlString, versionString);
        httpConnection.setDoOutput(true);
        httpConnection.setRequestMethod(REQUEST_METHOD_POST);
        httpConnection.setRequestProperty(REQUEST_HEADER_CONTENT_TYPE, "application/x-www-form-urlencoded; " + StandardCharsets.UTF_8);
        httpConnection.setRequestProperty(REQUEST_HEADER_CONTENT_LENGTH, "" + postData.length);

        OutputStream outputStream = null;
        try {
            outputStream = httpConnection.getOutputStream();
            outputStream.write(postData);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
        return httpConnection;
    }

    /**
     * Create HTTP(s) connection.
     *
     * @param urlString     URL to connect to
     * @param versionString Optional version string, will be used in request header
     * @throws IOException in case of connection error
     */
    protected static HttpURLConnection post(final @NonNull String urlString,
                                            final @Nullable String versionString) throws IOException {
        val httpConnection = (HttpURLConnection) new URL(urlString).openConnection();

        // Complete list of HTTP header fields:
        // https://en.wikipedia.org/wiki/List_of_HTTP_header_fields
        httpConnection.setRequestProperty(REQUEST_HEADER_ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
        httpConnection.setRequestProperty(REQUEST_HEADER_ACCEPT, "*/*");
        if (versionString != null) {
            httpConnection.setRequestProperty(REQUEST_HEADER_USER_AGENT, USER_AGENT.replace("$0", versionString));
            httpConnection.setRequestProperty(REQUEST_HEADER_X_ONEAPP_VERSION, versionString);
        } else {
            // Version not set.
            httpConnection.setRequestProperty(REQUEST_HEADER_USER_AGENT, USER_AGENT.replace("$0", "x"));
        }
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
    protected static byte[] createPostBody(final @Nonnull @NonNull Map<String, String> parameters) throws UnsupportedEncodingException {
        // Create POST request payload.
        val urlParams = new StringBuilder();
        for (Entry<String, String> stringStringEntry : parameters.entrySet()) {
            if (urlParams.length() > 0) {
                urlParams.append("&");
            }
            urlParams.append(stringStringEntry.getKey());
            urlParams.append("=");
            urlParams.append(URLEncoder.encode(stringStringEntry.getValue(), StandardCharsets.UTF_8.name()));
        }
        return urlParams.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Value
    @Builder
    public class PageContent {
        private Map<String, List<String>> headers;
        private String content;
    }
}
