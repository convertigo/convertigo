/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.servlets;

import static org.junit.Assert.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.codehaus.jettison.json.JSONObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.PluginsManager;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.requesters.JsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class NativeJsonServletTest {
	private final Map<String,Object> attributes = new HashMap<>();
	private final Map<String,String> headers = new HashMap<>();
	private final StringWriter body = new StringWriter();
	private int status = 200;
	private String encoding;
	private boolean closed, finalized;
	private boolean disconnected;
	@Before public void setup() throws Exception {
		Engine.logEngine = Engine.logContext = org.apache.log4j.Logger.getLogger("native-json-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.pluginsManager = new PluginsManager();
	}
	private HttpServletRequest request() {
		return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpServletRequest.class},
			(proxy, method, args) -> switch(method.getName()) {
			case "getMethod" -> "GET";
			case "getScheme" -> "http";
			case "getServerName" -> "localhost";
			case "getServerPort" -> 18080;
			case "getRequestURI" -> "/convertigo/projects/Test/.json";
			case "getCharacterEncoding" -> "UTF-8";
			case "getParameterMap" -> Map.of();
			case "getAttribute" -> attributes.get(args[0]);
			case "setAttribute" -> { attributes.put((String)args[0], args[1]); yield null; }
			case "removeAttribute" -> { attributes.remove(args[0]); yield null; }
			default -> null;
		});
	}
	private HttpServletResponse response() {
		var writer = new PrintWriter(body) {
			@Override public void write(char[] chars, int offset, int length) {
				if (disconnected) setError(); else super.write(chars, offset, length);
			}
			@Override public void close() { closed = true; super.close(); }
		};
		return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpServletResponse.class},
			(proxy, method, args) -> switch(method.getName()) {
			case "getWriter" -> writer;
			case "setHeader", "addHeader" -> { headers.put((String)args[0], (String)args[1]); yield null; }
			case "setContentType" -> { headers.put("Content-Type", (String)args[0]); yield null; }
			case "getContentType" -> headers.get("Content-Type");
			case "getHeaderNames" -> headers.keySet();
			case "setStatus" -> { status = (Integer)args[0]; yield null; }
			case "setCharacterEncoding" -> { encoding = (String)args[0]; yield null; }
			default -> null;
		});
	}
	@Test public void jsonServletKeepsStatusHeadersUnicodeAndClosesTheWriter() throws Exception {
		var value = new JSONObject("{text:'é <&>',rows:[1,true,null]}");
		var request = request();
		RequestAttribute.responseStatus.set(request, Map.of(201,""));
		RequestAttribute.responseHeader.set(request, Map.of("X-Result","kept"));
		new JsonServlet() {
			@Override public Object processRequest(HttpServletRequest req) {
				req.setAttribute("convertigo.isErrorDocument",false);
				req.setAttribute("convertigo.charset","UTF-8");
				return new JsonResponse(value,null);
			}
			@Override void onFinally(HttpServletRequest req) { finalized=true; }
			@Override public void processException(HttpServletRequest req,HttpServletResponse res,Exception e) { throw new AssertionError(e); }
		}.doRequest(request,response());
		assertEquals(value.toString(),new JSONObject(body.toString()).toString());
		assertEquals(201,status); assertEquals("UTF-8",encoding);
		assertEquals("kept",headers.get("X-Result")); assertTrue(closed); assertTrue(finalized);
		assertFalse(headers.containsKey("Content-Length"));
	}
	@Test public void restWriterStreamsJsonAndJsonpCanUseTheSameResponse() throws Exception {
		var value = new JSONObject("{ok:true}");
		var method = RestApiServlet.class.getDeclaredMethod("writeResponseContent",HttpServletRequest.class,HttpServletResponse.class,Object.class);
		method.setAccessible(true);
		method.invoke(new RestApiServlet(),request(),response(),new JsonResponse(value,null));
		assertEquals(value.toString(),body.toString()); assertTrue(closed);
		var jsonp = new StringWriter(); new JsonResponse(value,"receive").writeTo(jsonp);
		assertEquals("receive("+value+");",jsonp.toString());
	}
	@Test public void disconnectStillFinalizesTheServletAndClosesItsWriter() throws Exception {
		disconnected = true;
		var failure = new Exception[1];
		var value = new JSONObject("{rows:[1,2,3]}");
		new JsonServlet() {
			@Override public Object processRequest(HttpServletRequest req) {
				req.setAttribute("convertigo.isErrorDocument",false);
				req.setAttribute("convertigo.charset","UTF-8");
				return new JsonResponse(value,null);
			}
			@Override void onFinally(HttpServletRequest req) { finalized=true; }
			@Override public void processException(HttpServletRequest req,HttpServletResponse res,Exception e) { failure[0]=e; }
		}.doRequest(request(),response());
		assertNull("disconnect is handled without attempting another error response", failure[0]);
		assertTrue(closed); assertTrue(finalized);
		assertEquals("",body.toString());
	}
}
