package org.juurlink.atagone.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.juurlink.atagone.exceptions.AtagPageErrorException;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

/**
 * HTTP related utility methods.
 */
@Log
@UtilityClass
public class HTTPUtils {

	private static final String ENCODING_UTF_8 = "UTF-8";
	private static final String USER_AGENT = "Mozilla/5.0 (compatible; AtagOneApp/0.1; http://atag.one/)";

	private static final String REQUEST_METHOD_POST = "POST";
	private static final String REQUEST_HEADER_CONTENT_TYPE = "Content-Type";
	private static final String REQUEST_HEADER_CONTENT_LENGTH = "Content-Length";
	private static final String REQUEST_HEADER_ACCEPT_CHARSET = "Accept-Charset";
	private static final String REQUEST_HEADER_ACCEPT = "Accept";
	private static final String REQUEST_HEADER_USER_AGENT = "User-Agent";

	private static final Pattern PATTERN_PAGE_ERROR = Pattern.compile("<li class=\"text-error\"><span>(.*?)</span>", Pattern.DOTALL);

	/**
	 * HTTP Connect timeout in milliseconds.
	 */
	private static final int HTTP_CONNECT_TIMEOUT = 5000;

	/**
	 * HTTP Read timeout in milliseconds.
	 */
	private static final int HTTP_READ_TIMEOUT = 5000;

	/**
	 * Get GET page content.
	 *
	 * @return Full page content html
	 * @throws IOException            in case of connection error
	 * @throws AtagPageErrorException in case the page contains an error message
	 */
	public static String getPageContent(@NonNull String url) throws IOException, AtagPageErrorException {

		// HTTP(S) Connect.
		HttpURLConnection httpConnection = createHttpConnection(url);

		return toPageResponse(httpConnection);
	}

	/**
	 * Get POST page content.
	 *
	 * @return Full page content html
	 * @throws IOException            in case of connection error
	 * @throws AtagPageErrorException in case the page contains an error message
	 */
	public static String getPostPageContent(@NonNull String url, Map<String, String> parameters) throws IOException, AtagPageErrorException {

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
	 * Get page contents from given httpUrlConnection.
	 *
	 * @return pageContent
	 */
	protected static String toPageResponse(@NonNull final HttpURLConnection httpConnection) throws AtagPageErrorException, IOException {
		InputStream inputStreamStd = null;
		InputStream inputStreamErr = null;
		try {
			inputStreamStd = httpConnection.getInputStream();
			final String html = IOUtils.toString(inputStreamStd, ENCODING_UTF_8);

			// Does the page contain an error message?
			String errorMessage = extractPageErrorFromHtml(html);
			if (!StringUtils.isBlank(errorMessage)) {
				throw new AtagPageErrorException(errorMessage);
			}
			return html;

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
	 * @throws IOException in case of connection error
	 */
	protected static HttpURLConnection createHttpConnection(final String urlString) throws IOException {
		HttpURLConnection httpConnection = (HttpURLConnection) new URL(urlString).openConnection();

		// Complete list of HTTP header fields:
		// https://en.wikipedia.org/wiki/List_of_HTTP_header_fields
		httpConnection.setRequestProperty(REQUEST_HEADER_ACCEPT_CHARSET, ENCODING_UTF_8);
		httpConnection.setRequestProperty(REQUEST_HEADER_ACCEPT, "*/*");
		httpConnection.setRequestProperty(REQUEST_HEADER_USER_AGENT, USER_AGENT);
		httpConnection.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
		httpConnection.setReadTimeout(HTTP_READ_TIMEOUT);
		return httpConnection;
	}

	/**
	 * Extract page error message from HTML.
	 *
	 * @param html HTML
	 * @return Error message or null when no message available on page
	 */
	@Nullable
	protected static String extractPageErrorFromHtml(@NonNull final String html) {
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
