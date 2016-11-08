package com.topdesk.timetransformer.examples;

import java.util.concurrent.TimeUnit;

public class TimeUtils {
	public static long fiveMinutesAgo() {
		return System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5);
	}
}
