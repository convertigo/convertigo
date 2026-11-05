package com.twinsoft.convertigo.engine.sessions;

import static org.junit.Assert.*;

import org.junit.Test;

import com.twinsoft.convertigo.engine.enums.SessionAttribute;

public class RedisHttpClientAttributesTest {
	@Test public void sessionHttpPoolsAreNeverStored() {
		assertTrue(SessionAttributeFilter.isNeverStored(SessionAttribute.httpClient3.value()));
		assertTrue(SessionAttributeFilter.isNeverStored(SessionAttribute.httpClient4.value()));
	}
}
