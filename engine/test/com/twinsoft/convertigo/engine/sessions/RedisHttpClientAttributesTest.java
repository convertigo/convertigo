package com.twinsoft.convertigo.engine.sessions;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.enums.HttpPool;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.util.HttpServletRequestTwsWrapper;

public class RedisHttpClientAttributesTest {
	private static RedisSessionConfiguration configuration;

	@BeforeClass public static void configure() throws Exception {
		Engine.logRedis = org.apache.log4j.Logger.getLogger("redis-http-client-attributes-test");
		Engine.logContext = Engine.logRedis;
		Engine.logEngine = Engine.logRedis;
		EnginePropertiesManager.initProperties();
		configuration = RedisSessionConfiguration.fromProperties();
		if (Engine.theApp == null) {
			Engine.theApp = new Engine();
		}
	}

	private static final class MemoryStore implements SessionStore {
		final Map<String, String> fields = new HashMap<>();
		@Override public SessionStoreMeta readMeta(String sessionId) { return null; }
		@Override public String readAttribute(String sessionId, String name) { return fields.get(name); }
		@Override public Set<String> readAttributeNames(String sessionId) { return new HashSet<>(fields.keySet()); }
		@Override public void writeDelta(String sessionId, Map<String, String> hset, Set<String> hdel, long ttlMillis) {
			fields.putAll(hset);
			hdel.forEach(fields::remove);
		}
		@Override public void delete(String sessionId) { fields.clear(); }
		@Override public void shutdown() {}
	}

	/** The session of one request, as a requester sees it: read from the store and wrapped by the servlet request. */
	private static HttpSession request(MemoryStore store, String sessionId) {
		var now = System.currentTimeMillis();
		var session = RedisHttpSession.fromMeta(store, null, configuration, sessionId, new SessionStoreMeta(now, now, 1800, Map.of()));
		var request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
				new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getSession" -> session;
					case "getMethod" -> "GET";
					case "getCharacterEncoding" -> "UTF-8";
					case "getParameterMap" -> Collections.emptyMap();
					default -> null;
				});
		return new HttpServletRequestTwsWrapper(request).getSession();
	}

	@Test public void sessionHttpPoolsAreNeverStored() {
		assertTrue(SessionAttributeFilter.isNeverStored(SessionAttribute.httpClient3.value()));
		assertTrue(SessionAttributeFilter.isNeverStored(SessionAttribute.httpClient4.value()));
	}

	@Test public void sessionHttpPoolIsKeptAcrossRequestsOfAWrappedSession() {
		var store = new MemoryStore();
		var context = new Context("pool-test");
		context.httpSession = request(store, "pool-session-1");
		var first3 = context.getHttpClient3(HttpPool.session);
		var first4 = context.getHttpClient(HttpPool.session);
		context.httpSession = request(store, "pool-session-1");
		assertSame(first3, context.getHttpClient3(HttpPool.session));
		assertSame(first4, context.getHttpClient(HttpPool.session));
		context.httpSession = request(store, "pool-session-2");
		assertNotSame(first3, context.getHttpClient3(HttpPool.session));
		assertTrue(store.fields.keySet().stream().noneMatch(name -> name.contains("httpClient")));
	}
}
