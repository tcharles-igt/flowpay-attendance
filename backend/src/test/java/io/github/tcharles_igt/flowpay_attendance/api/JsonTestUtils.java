package io.github.tcharles_igt.flowpay_attendance.api;

import com.jayway.jsonpath.JsonPath;

final class JsonTestUtils {

	private JsonTestUtils() {
	}

	static Long readLong(String json, String path) {
		Number value = JsonPath.read(json, path);
		return value.longValue();
	}
}
