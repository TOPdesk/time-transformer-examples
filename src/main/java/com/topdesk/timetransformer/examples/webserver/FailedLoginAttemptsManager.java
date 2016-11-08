package com.topdesk.timetransformer.examples.webserver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.topdesk.timetransformer.examples.TimeUtils;

// Naive implementation of a User lockout system, does not persist its state
public class FailedLoginAttemptsManager {
	private static final Attempts UNKNOWN_USER = new Attempts(0);
	
	private final ConcurrentMap<String, Attempts> loginAttempts = new ConcurrentHashMap<>();
	private final int maxAttempts;
	
	public FailedLoginAttemptsManager(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}
	
	public boolean isLockedOut(String username) {
		return find(username).isLockedOut();
	}
	
	public void registerFailedLoginAttempt(String username) {
		find(username).registerFailedAttempt();
	}
	
	private Attempts find(String username) {
		if (isKnownUser(username)) {
			return loginAttempts.computeIfAbsent(username, (name) -> new Attempts(maxAttempts));
		}
		return UNKNOWN_USER;
	}
	
	private boolean isKnownUser(String username) {
		// there might be more users in your application
		return "admin".equals(username);
	}
	
	private static final class Attempts {
		private final int maxAttempts;
		private int remainingAttempts;
		private long previousAttemptTimestamp = Long.MIN_VALUE;
		
		Attempts(int maxAttempts) {
			this.maxAttempts = maxAttempts;
			remainingAttempts = maxAttempts;
		}
		
		void registerFailedAttempt() {
			if (remainingAttempts > 0) {
				remainingAttempts--;
			}
			previousAttemptTimestamp = System.currentTimeMillis();
		}
		
		boolean isLockedOut() {
			if (previousAttemptTimestamp < TimeUtils.fiveMinutesAgo()) {
				remainingAttempts = maxAttempts;
				previousAttemptTimestamp = Long.MIN_VALUE;
			}
			return remainingAttempts == 0;
		}
	}
}
