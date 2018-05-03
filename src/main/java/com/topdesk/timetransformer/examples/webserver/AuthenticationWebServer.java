package com.topdesk.timetransformer.examples.webserver;

import static com.topdesk.timetransformer.examples.webserver.HtmlUtils.PASSWORD_PARAMETER;
import static com.topdesk.timetransformer.examples.webserver.HtmlUtils.USERNAME_PARAMETER;
import static com.topdesk.timetransformer.examples.webserver.HtmlUtils.buildLoginFormResponse;
import static com.topdesk.timetransformer.examples.webserver.HtmlUtils.buildWelcomePage;
import static com.topdesk.timetransformer.examples.webserver.HtmlUtils.consumePostRequestBody;
import static com.topdesk.timetransformer.examples.webserver.HtmlUtils.convertParametersToMap;
import static com.topdesk.timetransformer.examples.webserver.HtmlUtils.sendHtmlResponse;
import static com.topdesk.timetransformer.examples.webserver.HtmlUtils.sendTextResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.topdesk.timetransformer.TimeTransformer;
import com.topdesk.timetransformer.TransformingTime;

public class AuthenticationWebServer {
	private static final int PASSWORD_ATTEMPTS = 3;
	
	public static void main(String[] args) throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(getWebServerPort()), 0);
		server.createContext("/login", new AuthenticationHandler());
		if (Boolean.getBoolean("testingMode")) {
			log("Enabling testing mode");
			server.createContext("/test/transformtime", new TimeTransformerHandler());
		}
		server.start();
		log("HttpServer started and listening on: " + server.getAddress());
	}
	
	private static void log(String message) {
		if (!Boolean.getBoolean("quiet")) {
			System.out.println(message);
		}
	}
	
	private static int getWebServerPort() {
		return Integer.getInteger("webserverPort", 8080);
	}
	
	private static final class TimeTransformerHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			Map<String, String> parameters = convertParametersToMap(exchange.getRequestURI().getQuery());
			if (parameters.get("disable") != null) {
				TimeTransformer.setTime(null);
				sendTextResponse(exchange, "disabled system time");
				return;
			}
			if (parameters.get("current") != null) {
				sendTextResponse(exchange, Long.toString(System.currentTimeMillis()));
				return;
			}
			String timeParameter = parameters.get("time");
			if (timeParameter != null) {
				long time = Long.parseLong(timeParameter);
				TimeTransformer.setTime(TransformingTime.INSTANCE);
				TransformingTime.INSTANCE.apply(TransformingTime.change().at(time));
				sendTextResponse(exchange, "set system time to: " + time);
				return;
			}
			sendTextResponse(exchange, "usage: GET ?disable or ?current or ?time=[timeInMillis]");
		}
	}
	
	private static final class AuthenticationHandler implements HttpHandler {
		private FailedLoginAttemptsManager failedLoginAttemptsManager = new FailedLoginAttemptsManager(PASSWORD_ATTEMPTS);
		
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
				sendHtmlResponse(exchange, buildLoginFormResponse(""));
				return;
			}
			
			Map<String, String> postData = consumePostRequestBody(exchange);
			if (authenticate(postData)) {
				sendHtmlResponse(exchange, buildWelcomePage(postData.get(USERNAME_PARAMETER)));
			}
			else {
				sendHtmlResponse(exchange, buildLoginFormResponse("Incorrect login"));
			}
		}
		
		private boolean authenticate(Map<String, String> postData) {
			String username = postData.get(USERNAME_PARAMETER);
			String password = postData.get(PASSWORD_PARAMETER);
			if (!failedLoginAttemptsManager.isLockedOut(username) && "admin".equals(username) && "admin".equals(password)) {
				return true;
			}
			failedLoginAttemptsManager.registerFailedLoginAttempt(username);
			return false;
		}
	}
}
