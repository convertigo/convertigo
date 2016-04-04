package com.twinsoft.convertigo.engine.requesters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.Part;

import com.twinsoft.convertigo.engine.util.GenericUtils;

@SuppressWarnings("deprecation")
public class InternalHttpServletRequest implements HttpServletRequest {
	static long sessionID = System.currentTimeMillis();
	
	InternalRequester internalRequester;
	
	private Map<String, Object> attributes;
	private HttpSession session = null;

	private String characterEncoding = "UTF-8";
	private String localAddr = "127.0.0.1";
	private String localName = "localhost";
	private int localPort = 18080;
	private String remoteAddr = localAddr;
	private String remoteHost = localName;
	private int remotePort = 18081;
	private String serverName = localAddr;
	private int serverPort = localPort;
	private ServletContext servletContext = null;
	private Map<String, List<String>> headers = null;
	
	public InternalHttpServletRequest() {
		
	}
	
	public InternalHttpServletRequest(HttpSession session) {
		this.session = session;
	}
	
	public InternalHttpServletRequest(HttpServletRequest request) {
		characterEncoding = request.getCharacterEncoding();
		localAddr = request.getLocalAddr();
		localName = request.getLocalName();
		localPort = request.getLocalPort();
		remoteAddr = request.getRemoteAddr();
		remoteHost = request.getRemoteHost();
		remotePort = request.getRemotePort();
		serverName = request.getServerName();
		serverPort = request.getServerPort();
		servletContext = request.getServletContext();
		session = request.getSession();
		
		headers = new HashMap<String, List<String>>();
		for (Enumeration<String> i = request.getHeaderNames(); i.hasMoreElements();) {
			String name = i.nextElement();
			List<String> values = new ArrayList<String>();
			for (Enumeration<String> j = request.getHeaders(name); j.hasMoreElements();) {
				values.add(j.nextElement());
			}
			headers.put(name, values);
		}
		
		for (Enumeration<String> i = request.getAttributeNames(); i.hasMoreElements();) {
			String name = i.nextElement();
			setAttribute(name, request.getAttribute(name));
		}
		
	}
	
	private Map<String, Object> getAttributes() {
		if (attributes == null) {
			attributes = new HashMap<String, Object>();
		}
		return attributes;
	}
	
	@Override
	public AsyncContext getAsyncContext() {
		return null;
	}

	@Override
	public Object getAttribute(String attribute) {
		Object o = null;
		if (attribute != null && attributes != null) {
			o = attributes.get(attribute);
		}
		return o;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(getAttributes().keySet());
	}

	@Override
	public String getCharacterEncoding() {
		return characterEncoding;
	}

	@Override
	public int getContentLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getContentType() {
		return "x-www-form-urlencoded";
	}

	@Override
	public DispatcherType getDispatcherType() {
		return null;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLocalAddr() {
		return localAddr;
	}

	@Override
	public String getLocalName() {
		return localName;
	}

	@Override
	public int getLocalPort() {
		return localPort;
	}

	@Override
	public Locale getLocale() {
		return Locale.getDefault();
	}

	@Override
	public Enumeration<Locale> getLocales() {
		return Collections.enumeration(Arrays.asList(getLocale()));
	}

	@Override
	public String getParameter(String parameter) {
		Map<String, Object> input = GenericUtils.cast(internalRequester.inputData);
		Object value = input.get(parameter);
		String result = null;
		if (value instanceof String) {
			result = (String) value;
		} else if (value instanceof String[]) {
			String[] values = (String[]) value;
			if (values.length > 0) {
				result = values[0];
			}
		}
		return result;
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		Map<String, Object> input = GenericUtils.cast(internalRequester.inputData);
		Map<String, String[]> result = new HashMap<String, String[]>(input.size());
		for (Entry<String, Object> entry: input.entrySet()) {
			String[] value = toStringArray(entry.getValue());
			if (value != null) {
				result.put(entry.getKey(), value);
			}
		}
		return result;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		Map<String, Object> input = GenericUtils.cast(internalRequester.inputData);
		Collection<String> keys = new ArrayList<String>(input.size());
		for (Entry<String, Object> entry: input.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof String || value instanceof String[]) {
				keys.add(entry.getKey());
			}
		}
		return Collections.enumeration(keys);
	}
	
	static private String[] toStringArray(Object value) {
		String[] result = null;
		if (value instanceof String) {
			result = new String[]{(String) value};
		} else if (value instanceof String[]) {
			result = (String[]) value;
		}
		return result;		
	}
	
	@Override
	public String[] getParameterValues(String parameter) {
		Map<String, Object> input = GenericUtils.cast(internalRequester.inputData);
		Object value = input.get(parameter);
		String[] result = toStringArray(value);
		return result;
	}

	@Override
	public String getProtocol() {
		return "http";
	}

	@Override
	public BufferedReader getReader() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRealPath(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRemoteAddr() {
		return remoteAddr;
	}

	@Override
	public String getRemoteHost() {
		return remoteHost;
	}

	@Override
	public int getRemotePort() {
		return remotePort;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getScheme() {
		return "http";
	}

	@Override
	public String getServerName() {
		return serverName;
	}

	@Override
	public int getServerPort() {
		return serverPort;
	}

	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override
	public boolean isAsyncStarted() {
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		return false;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public void removeAttribute(String attribute) {
		if (attributes != null) {
			attributes.remove(attribute);
		}
	}

	@Override
	public void setAttribute(String attribute, Object value) {
		getAttributes().put(attribute, value);
	}

	@Override
	public void setCharacterEncoding(String characterEncoding)
			throws UnsupportedEncodingException {
		Charset.forName(characterEncoding);
		this.characterEncoding = characterEncoding;
	}

	@Override
	public AsyncContext startAsync() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean authenticate(HttpServletResponse arg0) throws IOException,
			ServletException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getAuthType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContextPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Cookie[] getCookies() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getDateHeader(String arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getHeader(String name) {
		if (headers != null) {
			List<String> values = headers.get(name);
			if (values != null && !values.isEmpty()) {
				return values.get(0);
			}
		}
		return null;
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		if (headers != null) {
			Collections.enumeration(headers.keySet());
		}
		return Collections.enumeration(Collections.<String>emptyList());
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		if (headers != null) {
			List<String> values = headers.get(name);
			if (values != null) {
				return Collections.enumeration(values);
			}
		}
		return Collections.enumeration(Collections.<String>emptyList());
	}

	@Override
	public int getIntHeader(String name) {
		try {
			return Integer.parseInt(getHeader(name));
		} catch (Exception e) {
			// ignore
		}
		return 0;
	}

	@Override
	public String getMethod() {
		return "POST";
	}

	@Override
	public Part getPart(String arg0) throws IOException, IllegalStateException,
			ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Part> getParts() throws IOException,
			IllegalStateException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPathInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPathTranslated() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getQueryString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRemoteUser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequestURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuffer getRequestURL() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequestedSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServletPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpSession getSession() {
		return getSession(true);
	}

	@Override
	public HttpSession getSession(boolean create) {
		if (create && session == null) {
			session = new InternalSession();
		}
		return session;
	}

	@Override
	public Principal getUserPrincipal() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isUserInRole(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void login(String arg0, String arg1) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void logout() throws ServletException {
		// TODO Auto-generated method stub

	}

	class InternalSession implements HttpSession {
		private Map<String, Object> values;
		private Map<String, Object> attributes;
		long creationTime = System.currentTimeMillis();
		long lastAccessTime = -1;
		int maxInactiveTime = 60000;
		String id = Long.toString(sessionID++, Character.MAX_RADIX).toUpperCase();
		
		private Map<String, Object> getAttributes() {
			if (attributes == null) {
				attributes = new HashMap<String, Object>();
			}
			return attributes;
		}
		
		private Map<String, Object> getValues() {
			if (values == null) {
				values = new HashMap<String, Object>();
			}
			return values;
		}
		
		private void tick() {
			lastAccessTime = System.currentTimeMillis();
		}

		@Override
		public Object getAttribute(String attribute) {
			tick();
			Object o = null;
			if (attribute != null && attributes != null) {
				o = attributes.get(attribute);
			}
			return o;
		}

		@Override
		public Enumeration<String> getAttributeNames() {
			tick();
			return Collections.enumeration(getAttributes().keySet());
		}

		@Override
		public long getCreationTime() {
			return creationTime;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public long getLastAccessedTime() {
			return lastAccessTime;
		}

		@Override
		public int getMaxInactiveInterval() {
			return maxInactiveTime;
		}

		@Override
		public ServletContext getServletContext() {
			return InternalHttpServletRequest.this.getServletContext();
		}

		@Override
		public HttpSessionContext getSessionContext() {
			return null;
		}

		@Override
		public Object getValue(String key) {
			tick();
			Object o = null;
			if (values != null) {
				o = values.get(key);
			}
			return o;
		}

		@Override
		public String[] getValueNames() {
			if (values != null) {
				Collection<String> names = values.keySet();
				return names.toArray(new String[names.size()]);
			}
			return new String[0];
		}

		@Override
		public void invalidate() {
		}

		@Override
		public boolean isNew() {
			return lastAccessTime == -1;
		}

		@Override
		public void putValue(String key, Object value) {
			getValues().put(key, value);
		}

		@Override
		public void removeAttribute(String attribute) {
			if (attributes != null) {
				attributes.remove(attribute);
			}
		}

		@Override
		public void removeValue(String key) {
			if (values != null) {
				values.remove(key);
			}
		}

		@Override
		public void setAttribute(String attribute, Object value) {
			getAttributes().put(attribute, value);
		}

		@Override
		public void setMaxInactiveInterval(int maxInactiveTime) {
			this.maxInactiveTime = maxInactiveTime;
		}
		
	}
}
