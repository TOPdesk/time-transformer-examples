package com.topdesk.timetransformer.examples.webserver;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AuthenticationWebserverE2ECase {
	private static final int LOCKOUT_ATTEMPTS = 3;
	private static final int WEBDRIVER_TIMEOUT_IN_SECONDS = 10;
	
	private int port = getRandomUnusedPort();
	private Process webServerProcess;
	private WebDriver driver;
	private final String baseUrl = "http://localhost:" + port + "/";
	private final String loginUrl = baseUrl + "login";
	private final String transformtimeEndpoint = baseUrl + "test/transformtime";
	
	// Ensure test isolation: make sure every E2E test runs against its own instance of the webserver.
	@Before
	public void startWebserver() throws IOException {
		webServerProcess = startStandaloneWebServer();
		waitForWebserverStarted();
		driver = new HtmlUnitDriver();
		driver.manage().timeouts().implicitlyWait(WEBDRIVER_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
		driver.navigate().to(loginUrl);
	}
	
	@After
	public void stopWebserver() throws Exception {
		try {
			disableTimeTransformer();
		}
		finally {
			if (driver != null) {
				driver.quit();
			}
			if (webServerProcess != null) {
				webServerProcess.destroy();
			}
		}
	}
	
	@Test
	public void testSuccessfulLogin() {
		whenUserPerformsCorrectLogin();
		thenUserIsSuccessfullyLoggedIn();
	}
	
	@Test
	public void testUnsuccessfulLogin() {
		whenUserPerformsIncorrectLogin();
		thenUserIsNotLoggedIn();
	}
	
	@Test
	public void testUserLockout() {
		givenThatUserIsLockedOut();
		whenUserPerformsCorrectLogin();
		thenUserIsNotLoggedIn();
	}
	
	@Test
	public void testUserLockout_AfterLockoutExpiration() throws Exception {
		givenThatUserIsLockedOut();
		whenUserLockoutTimeHasExpired();
		whenUserPerformsCorrectLogin();
		thenUserIsSuccessfullyLoggedIn();
	}
	
	private void givenThatUserIsLockedOut() {
		for (int i = 0; i < LOCKOUT_ATTEMPTS; i++) {
			whenUserPerformsIncorrectLogin();
			// Good practice for stable Selenium tests: after performing an action, wait for the action to finish.
			// In this case of a failed login attempt: after clicking the loginbutton, wait for the reload of the login 
			// screen by looking at the username field which turns empty after the reload.
			waitForLoginScreenToReload();
		}
	}
	
	private void whenUserPerformsCorrectLogin() {
		performLogin("admin", "admin");
	}
	
	private void whenUserPerformsIncorrectLogin() {
		performLogin("admin", "wrong");
	}
	
	private void thenUserIsSuccessfullyLoggedIn() {
		assertEquals("Welcome admin", driver.findElement(By.id("welcome")).getText());
	}
	
	private void thenUserIsNotLoggedIn() {
		assertEquals("Incorrect login", driver.findElement(By.id("message")).getText());
	}
	
	private void whenUserLockoutTimeHasExpired() throws Exception {
		// Naive implementation would be to sleep 5 minutes
		long fiveMinutesLater = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
		transformWebServerTime(fiveMinutesLater);
		verifyWebServerTimeHasPast(fiveMinutesLater);
	}
	
	private void transformWebServerTime(long time) throws MalformedURLException, IOException {
		URI uri = URI.create(transformtimeEndpoint + "?time=" + time);
		HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
		connection.connect();
		if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
			throw new IllegalStateException("test/transformtime endpoint not reachable");
		}
	}
	
	private void verifyWebServerTimeHasPast(long time) throws MalformedURLException, IOException {
		URI uri = URI.create(transformtimeEndpoint + "?current");
		HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
		connection.connect();
		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			try (InputStream inputStream = connection.getInputStream()) {
				long currentTime = Long.parseLong(HtmlUtils.readInputStreamIntoString(inputStream));
				if (currentTime < time) {
					throw new IllegalStateException("test/transformtime did not advance the time ");
				}
			}
		}
	}
	
	private void disableTimeTransformer() throws Exception {
		URI uri = URI.create(transformtimeEndpoint + "?disable");
		HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
		connection.connect();
		if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
			throw new IllegalStateException("test/transformtime endpoint not reachable");
		}
	}
	
	private void performLogin(String username, String password) {
		driver.findElement(By.id("username")).sendKeys(username);
		driver.findElement(By.id("password")).sendKeys(password);
		driver.findElement(By.id("loginbutton")).click();
	}
	
	private void waitForLoginScreenToReload() {
		new WebDriverWait(driver, WEBDRIVER_TIMEOUT_IN_SECONDS).until(input -> driver.findElement(By.id("username")).getText().isEmpty());
	}
	
	private Process startStandaloneWebServer() throws IOException {
		String webserverBinary = System.getProperty("webserver.binary");
		if (webserverBinary == null) {
			throw new IllegalStateException("webserver.binary system property not set");
		}
		String javaagent = System.getProperty("javaagent");
		if (javaagent == null) {
			throw new IllegalStateException("javaagent system property not set");
		}
		ProcessBuilder processBuilder = new ProcessBuilder("java",
				"-javaagent:" + javaagent,
				"-DtestingMode=true",
				"-Dquiet=true",
				"-DwebserverPort=" + port,
				"-jar", webserverBinary).inheritIO();
		return processBuilder.start();
	}
	
	private void waitForWebserverStarted() {
		long startTime = System.currentTimeMillis();
		long timeout = TimeUnit.SECONDS.toMillis(10);
		while (System.currentTimeMillis() < startTime + timeout) {
			try {
				HttpURLConnection connection = (HttpURLConnection) URI.create(loginUrl).toURL().openConnection();
				connection.setConnectTimeout(600);
				connection.connect();
				if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					return;
				}
			}
			catch (MalformedURLException e) {
				throw new IllegalArgumentException(e);
			}
			catch (IOException e) {
				// Couldn't connect, ignore and retry.
			}
			try {
				Thread.sleep(250);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		throw new IllegalStateException("webserver did not come online within the specified timeout");
	}
	
	private static int getRandomUnusedPort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
