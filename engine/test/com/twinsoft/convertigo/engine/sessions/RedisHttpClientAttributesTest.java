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
import com.twinsoft.convertigo.engine.util.HttpUtils;

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
		return wrap(RedisHttpSession.fromMeta(store, null, configuration, sessionId, new SessionStoreMeta(now, now, 1800, Map.of())));
	}

	private static HttpSession wrap(RedisHttpSession session) {
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
		var now = System.currentTimeMillis();
		var first = RedisHttpSession.fromMeta(store, null, configuration, "pool-session-1", new SessionStoreMeta(now, now, 1800, Map.of()));
		context.httpSession = wrap(first);
		var first3 = context.getHttpClient3(HttpPool.session);
		var first4 = context.getHttpClient(HttpPool.session);
		first.flush();
		context.httpSession = request(store, "pool-session-1");
		assertSame(first3, context.getHttpClient3(HttpPool.session));
		assertSame(first4, context.getHttpClient(HttpPool.session));
		context.httpSession = request(store, "pool-session-2");
		assertNotSame(first3, context.getHttpClient3(HttpPool.session));
		assertFalse(store.fields.isEmpty());
		assertTrue(store.fields.keySet().stream().noneMatch(name -> name.contains("httpClient")));
	}

	private static boolean isShutdown(org.apache.commons.httpclient.HttpClient client) throws Exception {
		var field = org.apache.commons.httpclient.MultiThreadedHttpConnectionManager.class.getDeclaredField("shutdown");
		field.setAccessible(true);
		return (Boolean) field.get(client.getHttpConnectionManager());
	}

	private static final class CountingClient implements HttpUtils.HttpClientInterface {
		int closed;
		@Override public org.apache.http.client.methods.CloseableHttpResponse execute(org.apache.http.client.methods.HttpUriRequest request) { return null; }
		@Override public void close() { closed++; }
	}

	@Test public void sessionHttpPoolsAreClosedWhenTheSessionIsInvalidated() throws Exception {
		var store = new MemoryStore();
		var now = System.currentTimeMillis();
		var session = RedisHttpSession.fromMeta(store, null, configuration, "pool-session-close", new SessionStoreMeta(now, now, 1800, Map.of()));
		var context = new Context("pool-close-test");
		context.httpSession = wrap(session);
		var pool3 = context.getHttpClient3(HttpPool.session);
		var pool4 = new CountingClient();
		StatefulSessionAttributes.setStatefulAttribute(context.httpSession, SessionAttribute.httpClient4.value(), pool4);
		assertFalse(isShutdown(pool3));
		session.invalidate();
		assertTrue(isShutdown(pool3));
		assertEquals(1, pool4.closed);
	}

	@Test public void sessionHttpPoolIsClosedWhenRemoved() throws Exception {
		var store = new MemoryStore();
		var session = request(store, "pool-session-remove");
		var context = new Context("pool-remove-test");
		context.httpSession = session;
		var pool3 = context.getHttpClient3(HttpPool.session);
		StatefulSessionAttributes.removeStatefulAttribute(session, SessionAttribute.httpClient3.value());
		assertTrue(isShutdown(pool3));
	}

	@Test public void httpClient4IsClosable() throws Exception {
		var client = HttpUtils.makeHttpClient(true);
		client.close();
		client.close();
	}
}
