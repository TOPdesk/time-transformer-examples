package com.topdesk.timetransformer.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.topdesk.timetransformer.DefaultTime;
import com.topdesk.timetransformer.TimeTransformer;
import com.topdesk.timetransformer.TransformingTime;

public class TimeUtilsTest {
	@Test
	@Disabled("Not a good unit test, depends on the operating system time. Flaky as well, sometimes there is more than 1 ms between the evaluation of expected and actual.")
	public void naiveFlakyTestFiveMinutesAgo() {
		assertEquals(System.currentTimeMillis() - 5 * 60 * 1000, TimeUtils.fiveMinutesAgo());
	}
	
	@Test
	public void testFiveMinutesAgo() {
		try {
			TimeTransformer.setTime(TransformingTime.INSTANCE);
			long time = 1_000_000_000l;
			TransformingTime.INSTANCE.apply(TransformingTime.change().at(time).stop());
			assertEquals(time - 5 * 60 * 1000, TimeUtils.fiveMinutesAgo());
		}
		finally {
			TimeTransformer.setTime(DefaultTime.INSTANCE);
		}
	}
}
