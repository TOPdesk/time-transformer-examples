package com.topdesk.timetransformer.examples.webserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

class HtmlUtils {
	static final String USERNAME_PARAMETER = "username";
	static final String PASSWORD_PARAMETER = "password";
	
	static void sendHtmlResponse(HttpExchange exchange, String responseString) throws IOException {
		sendResponse(exchange, responseString, "text/html; charset=utf-8");
	}
	
	static void sendTextResponse(HttpExchange exchange, String responseString) throws IOException {
		sendResponse(exchange, responseString, "text/plain; charset=utf-8");
	}
	
	static void sendResponse(HttpExchange exchange, String responseString, String contentType) throws IOException {
		byte[] response = responseString.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", contentType);
		exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(response);
		}
	}
	
	static String buildWelcomePage(String name) {
		return inBody(String.format("<span id=\"welcome\">Welcome %s</span>", escapeHtml(name)));
	}
	
	private static String escapeHtml(String text) {
		// Does not handle code points, performance is also sub-optimal 
		StringBuilder result = new StringBuilder();
		for (char c : text.toCharArray()) {
			result.append(String.format("&#x%02X;", c & 0xffff));
		}
		return result.toString();
	}
	
	/**
	 * Consumes the request body input stream and closes it.
	 * @param exchange
	 * @return a Map containing all URL decoded Post parameters
	 * @throws IOException
	 */
	static Map<String, String> consumePostRequestBody(HttpExchange exchange) throws IOException {
		try (InputStream inputStream = exchange.getRequestBody()) {
			return convertParametersToMap(readInputStreamIntoString(inputStream));
		}
	}
	
	static String readInputStreamIntoString(InputStream inputStream) throws IOException {
		// Does not look at headers etc, always uses UTF-8
		try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[1024];
			int length;
			while ((length = inputStream.read(buffer)) != -1) {
				result.write(buffer, 0, length);
			}
			return result.toString(StandardCharsets.UTF_8.name());
		}
	}
	
	static Map<String, String> convertParametersToMap(String parametersData) {
		// parsing parameters is more complicated than this!
		if (parametersData == null) {
			return Collections.emptyMap();
		}
		Map<String, String> parameters = new HashMap<>();
		for (String parameter : parametersData.split("&")) {
			String[] keyAndValue = parameter.split("=", 2);
			try {
				parameters.put(urlDecode(keyAndValue[0]), (keyAndValue.length == 2) ? urlDecode(keyAndValue[1]) : "");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		return parameters;
	}
	
	private static String urlDecode(String value) throws UnsupportedEncodingException {
		return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
	}
	
	static String buildLoginFormResponse(String message) {
		StringBuilder builder = new StringBuilder();
		if (message != null && !message.isEmpty()) {
			builder.append(String.format("<fieldset id=\"message\" style=\"display:inline-block\">\n%s</fieldset>\n", message));
		}
		builder.append(
				"<form method=\"post\">" + 
				"<fieldset style=\"display:inline-block\"><legend>Please login:</legend>" +
				"<label for=\"" +
				USERNAME_PARAMETER +
				"\">Username:</label><br/>\n<input name=\"" +
				USERNAME_PARAMETER +
				"\" id=\"" +
				USERNAME_PARAMETER +
				"\" type=\"text\"/><br/>\n<label for=\"" +
				PASSWORD_PARAMETER +
				"\">Password:</label><br/>\n<input name=\"" +
				PASSWORD_PARAMETER +
				"\" id=\"" +
				PASSWORD_PARAMETER +
				"\" type=\"password\"/><br/><br/>\n" +
				"<center><input type=\"submit\" id=\"loginbutton\" value=\"Login\"/></center>" +
				"</fieldset>\n</form>\n");
		return inBody(builder.toString());
	}
	
	static String inBody(String contents) {
		return String.format("<html>\n<head>\n<title>Authentication webserver</title>\n</head>\n<body>\n%s</body>\n</html>\n", contents);
	}
}
