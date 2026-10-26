package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;
import org.apache.commons.httpclient.Header;
import org.junit.Test;

public class HttpHeaderSanitizerTest {
	@Test public void hidesBothCredentialsWithoutChangingWireHeaders() {
		var headers = new Header[] { new Header("aUtHoRiZaTiOn", "synthetic-origin"),
			new Header("PROXY-AUTHORIZATION", "synthetic-proxy"), new Header("Accept", "application/json") };
		var text = HttpHeaderSanitizer.describe(headers);
		assertFalse(text.contains("synthetic"));
		assertTrue(text.contains("Accept: application/json"));
		assertEquals("synthetic-origin", headers[0].getValue());
		assertEquals("synthetic-proxy", headers[1].getValue());
	}
}
